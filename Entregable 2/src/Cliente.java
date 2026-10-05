import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cliente.java
 * Un cliente que viene a comer al salón. Cada cliente es una tarea
 * (Runnable) que se ejecuta en el pool de hilos de clientes.
 *
 * Recorrido:
 *   llega -> (espera afuera si está lleno) -> entra -> espera en la sala
 *   hasta que se arma un grupo de P y hay mesa -> elige el menú ->
 *   espera en la barrera a que elija toda la mesa (el último llama al
 *   mozo) -> espera al mozo -> hace el pedido -> espera la comida (latch)
 *   -> come -> hace la fila de la caja y espera que le cobren -> se va.
 *
 * Si cierran antes de que el mozo le tome el pedido, se retira.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Cliente implements Runnable {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final int id = SECUENCIA.incrementAndGet();
    private final Restaurante restaurante;
    private final Cajas cajas;
    private final EstadoRestaurante estado;
    private final Estadisticas estadisticas;

    // Los escribe un hilo y los leen otros (restaurante, mozo): volatile
    private volatile Mesa mesa;
    private volatile Menu menu;

    public Cliente(Restaurante restaurante, Cajas cajas, EstadoRestaurante estado, Estadisticas estadisticas) {
        this.restaurante = restaurante;
        this.cajas = cajas;
        this.estado = estado;
        this.estadisticas = estadisticas;
    }

    @Override
    public void run() {
        estadisticas.clienteLlego();
        boolean entro = false;
        String motivo = "se va";
        try {
            if (!restaurante.entrar(this)) {
                estadisticas.clienteSinEntrar();
                estado.quitar(EstadoRestaurante.Rol.CLIENTE, id, this + " se va: el restaurante cerró sin que pudiera entrar");
                return;
            }
            entro = true;

            Mesa miMesa = restaurante.esperarMesa(this);
            if (miMesa == null) {
                motivo = "se retira de la sala de espera sin haber pedido (cierre)";
                estadisticas.clienteRetiradoPorCierre();
                return;
            }

            // Elegir el menú
            estado.actualizar(EstadoRestaurante.Rol.CLIENTE, id, "Eligiendo menú (" + miMesa + ")",
                    this + " se sienta en la " + miMesa + " y mira la carta");
            Azar.demorar(Config.TM_MIN, Config.TM_MAX);
            menu = Menu.aleatorio();
            estado.actualizar(EstadoRestaurante.Rol.CLIENTE, id, "Eligió " + menu + ", espera a su mesa",
                    this + " elige " + menu);

            // Barrera: cuando eligen los P, el último ejecuta la acción que llama al mozo
            miMesa.barreraMenu().await();

            estado.fijar(EstadoRestaurante.Rol.CLIENTE, id, "Esperando al mozo (" + menu + ")");
            if (!restaurante.esperarMozo(miMesa)) {
                motivo = "se retira sin haber hecho el pedido (cierre)";
                estadisticas.clienteRetiradoPorCierre();
                return;
            }

            // El mozo llegó: hace el pedido y espera que traigan la comida para toda la mesa
            estado.fijar(EstadoRestaurante.Rol.CLIENTE, id, "Pidió " + menu + ", espera la comida");
            miMesa.comidaServida().await();

            estado.actualizar(EstadoRestaurante.Rol.CLIENTE, id, "Comiendo " + menu,
                    this + " empieza a comer " + menu);
            Azar.demorar(Config.TQ_MIN, Config.TQ_MAX);

            // Pagar: fila única para todas las cajas
            estado.actualizar(EstadoRestaurante.Rol.CLIENTE, id, "En la fila de la caja",
                    this + " terminó de comer y va a la caja");
            CountDownLatch pagado = cajas.hacerFila(this, menu);
            pagado.await();

            estadisticas.clienteComio();
            motivo = "pagó y se va del restaurante";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            motivo = "se va (interrumpido)";
        } catch (BrokenBarrierException e) {
            motivo = "se va (la mesa no pudo completar el pedido)";
        } finally {
            if (entro) {
                restaurante.salir(this, motivo);
            }
        }
    }

    /** Lo llama el restaurante (con su monitor tomado) al sentar al grupo. */
    void asignarMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public int id() {
        return id;
    }

    public Mesa mesa() {
        return mesa;
    }

    public Menu menu() {
        return menu;
    }

    /** Nombre corto para la pantalla: "C7". */
    public String corto() {
        return "C" + id;
    }

    @Override
    public String toString() {
        return "Cliente " + id;
    }
}
