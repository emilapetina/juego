import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Mesa.java
 * Una mesa del restaurante con lugar para P clientes.
 *
 *  - CyclicBarrier (barreraMenu): los P clientes de la mesa esperan en la
 *    barrera hasta que TODOS eligieron su menú. Cuando llega el último se
 *    ejecuta la acción de la barrera, que llama a un mozo. La barrera es
 *    cíclica, así que se reutiliza con cada grupo que se sienta.
 *  - CountDownLatch (comidaServida): los clientes esperan a que el mozo
 *    traiga los platos de los P. El mozo hace countDown() una sola vez
 *    cuando sirvió toda la mesa y se liberan todos juntos. Se crea uno
 *    nuevo cada vez que se sienta un grupo (un latch no se puede reusar).
 *
 * El resto del estado (estado, comensales, presentes, huboPedido) se
 * modifica únicamente desde métodos synchronized de Restaurante, o sea
 * siempre con el monitor del restaurante tomado.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Mesa {

    public enum Estado {
        LIBRE("Libre"),
        ELIGIENDO("Eligiendo menú"),
        ESPERANDO_MOZO("Llamando al mozo"),
        TOMANDO_PEDIDO("Haciendo el pedido"),
        ESPERANDO_COMIDA("Esperando la comida"),
        PLATOS_LISTOS("Platos listos en cocina"),
        COMIENDO("Comiendo"),
        CANCELADA("Se retiran sin pedir (cierre)"),
        SUCIA("Sucia, esperando mozo"),
        LIMPIANDO("Limpiando");

        private final String texto;

        Estado(String texto) {
            this.texto = texto;
        }
    }

    private final int id;
    private final CyclicBarrier barreraMenu;

    // Protegidos por el monitor de Restaurante (volatile para que los lean
    // otros hilos fuera del monitor, por ejemplo el mozo al armar el pedido)
    private volatile Estado estado = Estado.LIBRE;
    private volatile List<Cliente> comensales = List.of();
    private volatile CountDownLatch comidaServida = new CountDownLatch(1);
    private int presentes;
    private boolean huboPedido;

    public Mesa(int id, int personas, Consumer<Mesa> alTerminarDeElegir) {
        this.id = id;
        // La acción de la barrera la ejecuta el último cliente en llegar
        this.barreraMenu = new CyclicBarrier(personas, () -> alTerminarDeElegir.accept(this));
    }

    // ------------------------------------------------------------------
    // Cambios de estado (solo desde Restaurante, con su monitor tomado)
    // ------------------------------------------------------------------

    void sentar(List<Cliente> grupo) {
        comensales = List.copyOf(grupo);
        presentes = grupo.size();
        huboPedido = false;
        comidaServida = new CountDownLatch(1);
        estado = Estado.ELIGIENDO;
    }

    void setEstado(Estado nuevo) {
        if (nuevo == Estado.TOMANDO_PEDIDO) {
            huboPedido = true;
        }
        estado = nuevo;
    }

    /** Un cliente se levanta; devuelve cuántos quedan sentados. */
    int levantarse() {
        return --presentes;
    }

    void liberar() {
        comensales = List.of();
        presentes = 0;
        estado = Estado.LIBRE;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public int id() {
        return id;
    }

    public Estado estado() {
        return estado;
    }

    boolean huboPedido() {
        return huboPedido;
    }

    public List<Cliente> comensales() {
        return comensales;
    }

    public CyclicBarrier barreraMenu() {
        return barreraMenu;
    }

    public CountDownLatch comidaServida() {
        return comidaServida;
    }

    /** Texto para la pantalla, por ejemplo "Comiendo [C4,C7,C9]". */
    String descripcion() {
        if (comensales.isEmpty()) {
            return estado.texto;
        }
        String nombres = comensales.stream().map(Cliente::corto).collect(Collectors.joining(",", "[", "]"));
        String quedan = presentes < comensales.size() ? " (quedan " + presentes + ")" : "";
        return estado.texto + " " + nombres + quedan;
    }

    @Override
    public String toString() {
        return "Mesa " + id;
    }
}
