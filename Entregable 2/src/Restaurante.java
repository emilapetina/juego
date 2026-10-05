import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Restaurante.java
 * Monitor central del salón: puerta de entrada (capacidad P*M), sala de
 * espera donde se arman los grupos de P clientes, mesas y lista de tareas
 * de los mozos (tomar pedidos, servir platos, limpiar mesas).
 *
 * Patrón Monitor: todo el estado compartido del salón vive acá adentro y
 * solo se accede desde métodos synchronized. Las esperas se hacen con
 * wait() dentro de un while (Guarded Suspension) que vuelve a verificar
 * la condición al despertar, y cada cambio de estado hace notifyAll()
 * para que cada hilo dormido vuelva a mirar si su condición se cumple.
 * Se usa notifyAll() y no notify() porque en el mismo monitor esperan
 * hilos con condiciones distintas (clientes afuera, clientes en la sala,
 * mesas esperando mozo, mozos sin tareas): un notify() podría despertar
 * a un hilo al que no le sirve el cambio y "perder" el aviso.
 *
 * Jerarquía de locks (para evitar deadlock): desde acá solo se llama a
 * EstadoRestaurante, que nunca llama de vuelta a nadie. Ninguna otra
 * clase llama a este monitor teniendo otro lock tomado.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Restaurante {

    private final Mesa[] mesas;
    private final int personasPorMesa;
    private final int capacidad;
    private final EstadoRestaurante estado;
    private final Estadisticas estadisticas;

    // Todo protegido por el monitor (this)
    private final Deque<Cliente> salaEspera = new ArrayDeque<>();
    private final Deque<Mesa> llamados = new ArrayDeque<>();
    private final Deque<Mesa> paraServir = new ArrayDeque<>();
    private final Deque<Mesa> paraLimpiar = new ArrayDeque<>();
    private int adentro;
    private boolean finJornada;
    // volatile: además se consulta sin el monitor (estaAbierto)
    private volatile boolean cerrado;

    public Restaurante(int cantidadMesas, int personasPorMesa, EstadoRestaurante estado, Estadisticas estadisticas) {
        this.personasPorMesa = personasPorMesa;
        this.capacidad = cantidadMesas * personasPorMesa;
        this.estado = estado;
        this.estadisticas = estadisticas;
        this.mesas = new Mesa[cantidadMesas];
        for (int i = 0; i < cantidadMesas; i++) {
            mesas[i] = new Mesa(i + 1, personasPorMesa, this::llamarMozo);
            estado.fijar(EstadoRestaurante.Rol.MESA, i + 1, mesas[i].descripcion());
        }
    }

    // ==================================================================
    // Clientes
    // ==================================================================

    /**
     * El cliente intenta entrar. Si ya hay P*M clientes adentro espera
     * afuera. Devuelve false si el restaurante cerró antes de que pudiera
     * entrar.
     */
    public synchronized boolean entrar(Cliente cliente) throws InterruptedException {
        if (adentro >= capacidad && !cerrado) {
            estado.actualizar(EstadoRestaurante.Rol.CLIENTE, cliente.id(), "Esperando afuera",
                    cliente + " llega pero el restaurante está lleno: espera afuera");
        }
        while (adentro >= capacidad && !cerrado) {
            wait();
        }
        if (cerrado) {
            return false;
        }
        adentro++;
        salaEspera.addLast(cliente);
        estado.actualizar(EstadoRestaurante.Rol.CLIENTE, cliente.id(), "En la sala de espera",
                cliente + " entra al restaurante (" + adentro + "/" + capacidad + " adentro)");
        sentarGrupos();
        return true;
    }

    /**
     * El cliente espera en la sala hasta que se junten P clientes y haya
     * una mesa libre. Devuelve la mesa, o null si cerraron mientras
     * esperaba (en ese caso tiene que irse).
     */
    public synchronized Mesa esperarMesa(Cliente cliente) throws InterruptedException {
        while (cliente.mesa() == null && !cerrado) {
            wait();
        }
        if (cliente.mesa() == null) {
            salaEspera.remove(cliente);
        }
        return cliente.mesa();
    }

    /**
     * Acción de la CyclicBarrier de la mesa: la ejecuta el último cliente
     * que termina de elegir. Si todavía está abierto se llama a un mozo.
     */
    private synchronized void llamarMozo(Mesa mesa) {
        if (cerrado) {
            mesa.setEstado(Mesa.Estado.CANCELADA);
            estado.actualizar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion(),
                    mesa + " terminó de elegir pero el restaurante ya cerró: no se toma el pedido");
        } else {
            mesa.setEstado(Mesa.Estado.ESPERANDO_MOZO);
            llamados.addLast(mesa);
            estado.actualizar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion(),
                    "Todos en la " + mesa + " eligieron: llaman a un mozo");
        }
        notifyAll();
    }

    /**
     * Los clientes esperan que llegue un mozo a tomarles el pedido.
     * Devuelve false si cerraron antes (no llegaron a pedir: se van).
     */
    public synchronized boolean esperarMozo(Mesa mesa) throws InterruptedException {
        while (mesa.estado() == Mesa.Estado.ESPERANDO_MOZO) {
            wait();
        }
        return mesa.estado() != Mesa.Estado.CANCELADA;
    }

    /**
     * El cliente se va del restaurante. Si era el último de su mesa, la
     * mesa queda sucia para que la limpie un mozo (o libre directamente si
     * nunca llegaron a pedir).
     */
    public synchronized void salir(Cliente cliente, String motivo) {
        adentro--;
        Mesa mesa = cliente.mesa();
        if (mesa != null && mesa.levantarse() == 0) {
            if (mesa.huboPedido()) {
                mesa.setEstado(Mesa.Estado.SUCIA);
                paraLimpiar.addLast(mesa);
            } else {
                mesa.liberar();
            }
            estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
            estado.quitar(EstadoRestaurante.Rol.CLIENTE, cliente.id(),
                    cliente + " " + motivo + ". La " + mesa + " queda " + (mesa.huboPedido() ? "sucia" : "libre"));
            sentarGrupos();
        } else {
            if (mesa != null) {
                estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
            }
            estado.quitar(EstadoRestaurante.Rol.CLIENTE, cliente.id(), cliente + " " + motivo);
        }
        notifyAll();
    }

    // ==================================================================
    // Mozos
    // ==================================================================

    /**
     * Devuelve la próxima tarea para un mozo, esperando si no hay ninguna.
     * Prioridad: servir platos listos > limpiar mesas > tomar pedidos
     * (primero se termina lo que ya está empezado, así se liberan mesas
     * y no se enfría la comida). Devuelve null cuando terminó la jornada
     * y no queda nada por hacer.
     */
    public synchronized TareaMozo siguienteTarea() throws InterruptedException {
        while (true) {
            Mesa mesa = paraServir.pollFirst();
            if (mesa != null) {
                return new TareaMozo(TareaMozo.Tipo.SERVIR, mesa);
            }
            mesa = paraLimpiar.pollFirst();
            if (mesa != null) {
                mesa.setEstado(Mesa.Estado.LIMPIANDO);
                estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
                return new TareaMozo(TareaMozo.Tipo.LIMPIAR, mesa);
            }
            mesa = llamados.pollFirst();
            if (mesa != null) {
                // A partir de acá la mesa "ya realizó su pedido": si cierran
                // ahora siguen con los pasos normales.
                mesa.setEstado(Mesa.Estado.TOMANDO_PEDIDO);
                estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
                notifyAll(); // despierta a los clientes de esa mesa
                return new TareaMozo(TareaMozo.Tipo.TOMAR_PEDIDO, mesa);
            }
            if (finJornada) {
                return null;
            }
            wait();
        }
    }

    /** El mozo dejó el pedido en la cocina. */
    public synchronized void pedidoEnCocina(Mesa mesa) {
        mesa.setEstado(Mesa.Estado.ESPERANDO_COMIDA);
        estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
    }

    /** Callback de la cocina: todos los platos de una mesa están listos. */
    public synchronized void pedidoListo(Pedido pedido) {
        Mesa mesa = pedido.mesa();
        mesa.setEstado(Mesa.Estado.PLATOS_LISTOS);
        paraServir.addLast(mesa);
        estado.actualizar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion(),
                "La cocina avisa a los mozos: " + pedido + " de la " + mesa + " está completo");
        notifyAll();
    }

    /** El mozo terminó de servir la mesa: los clientes pueden empezar a comer. */
    public synchronized void mesaServida(Mesa mesa) {
        mesa.setEstado(Mesa.Estado.COMIENDO);
        estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
        estadisticas.mesaAtendida();
        mesa.comidaServida().countDown();
    }

    /** El mozo terminó de limpiar: la mesa queda libre y se intenta sentar a otro grupo. */
    public synchronized void mesaLimpia(Mesa mesa) {
        mesa.liberar();
        estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
        sentarGrupos();
        notifyAll();
    }

    // ==================================================================
    // Cierre
    // ==================================================================

    /**
     * Cierra las puertas: no entra nadie más, los que esperan afuera o en
     * la sala se van, y las mesas que llamaron al mozo pero todavía no
     * hicieron el pedido se cancelan.
     */
    public synchronized void cerrar() {
        cerrado = true;
        for (Mesa mesa : llamados) {
            mesa.setEstado(Mesa.Estado.CANCELADA);
            estado.fijar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion());
        }
        llamados.clear();
        estado.evento("*** CIERRE *** Se cierran las puertas. Los que no pidieron se retiran; "
                + "los que ya pidieron siguen normalmente");
        notifyAll();
    }

    /** Ya no quedan clientes: los mozos terminan cuando no tengan más tareas. */
    public synchronized void terminarJornada() {
        finJornada = true;
        notifyAll();
    }

    public boolean estaAbierto() {
        return !cerrado;
    }

    // ==================================================================
    // Auxiliares (se llaman con el monitor tomado)
    // ==================================================================

    /** Mientras haya P clientes en la sala y alguna mesa libre, los sienta. */
    private void sentarGrupos() {
        if (cerrado) {
            return;
        }
        for (Mesa mesa : mesas) {
            if (salaEspera.size() < personasPorMesa) {
                return;
            }
            if (mesa.estado() != Mesa.Estado.LIBRE) {
                continue;
            }
            List<Cliente> grupo = new ArrayList<>(personasPorMesa);
            for (int i = 0; i < personasPorMesa; i++) {
                grupo.add(salaEspera.pollFirst());
            }
            mesa.sentar(grupo);
            for (Cliente cliente : grupo) {
                cliente.asignarMesa(mesa);
            }
            String nombres = grupo.stream().map(Cliente::toString).collect(Collectors.joining(", "));
            estado.actualizar(EstadoRestaurante.Rol.MESA, mesa.id(), mesa.descripcion(),
                    "Se sientan " + nombres + " en la " + mesa);
            notifyAll();
        }
    }
}
