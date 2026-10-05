import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntFunction;

/**
 * EstadoRestaurante.java
 * Registro con el estado actual de todos los actores (mesas, clientes,
 * mozos, cocina, cocineros y cajas). Cada vez que un actor
 * cambia de estado lo informa acá; se arma una "foto" del restaurante
 * completo en ese instante y se la manda al Display.
 *
 * Herramienta: ReentrantLock (justo).
 *  - Un solo hilo a la vez modifica el mapa, arma la foto y la encola.
 *    Como todo eso pasa con el lock tomado, el orden de la cola coincide
 *    con el orden real de los cambios y la foto nunca queda "a medio
 *    actualizar" (no se ve un actor con el estado nuevo y otro con el viejo).
 *  - Es justo (fair = true): los hilos que quieren informar un cambio lo
 *    hacen en orden de llegada, así ninguno se queda esperando de más y
 *    los mensajes salen en el orden en que se pidieron.
 *
 * Esta clase es una "hoja" en la jerarquía de locks: nunca llama a otro
 * objeto sincronizado mientras tiene su lock, así que no puede formar
 * parte de un ciclo de espera (deadlock).
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class EstadoRestaurante {

    /** Id fijo de la cola de platos dentro del rol COCINA. */
    public static final int COLA_PEDIDOS = 1;
    /** Id fijo de la fila de cobro dentro del rol CAJA. */
    public static final int FILA_CAJA = 0;

    /** Cada tipo de actor que aparece en pantalla, en el orden en que se muestran. */
    public enum Rol {
        MESA("MESAS", id -> "Mesa " + id),
        CLIENTE("CLIENTES", id -> "C" + id),
        MOZO("MOZOS", id -> "Mozo " + id),
        COCINA("COCINA", id -> "Platos"),
        COCINERO("COCINEROS", id -> "Cocinero " + id),
        CAJA("CAJAS", id -> id == FILA_CAJA ? "Fila" : "Cajero " + id);

        private final String titulo;
        private final IntFunction<String> etiqueta;

        Rol(String titulo, IntFunction<String> etiqueta) {
            this.titulo = titulo;
            this.etiqueta = etiqueta;
        }
    }

    private static final int ANCHO = 120;
    private static final String SANGRIA = " ".repeat(13);
    private static final String SEPARADOR = "=".repeat(ANCHO);

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Map<Rol, TreeMap<Integer, String>> estados = new EnumMap<>(Rol.class);
    private final Display display;
    private final long inicio = System.nanoTime();
    private long numeroEvento; // protegido por lock

    public EstadoRestaurante(Display display) {
        this.display = display;
        for (Rol rol : Rol.values()) {
            estados.put(rol, new TreeMap<>());
        }
    }

    /** Cambia el estado de un actor y publica una notificación con la foto completa. */
    public void actualizar(Rol rol, int id, String estado, String mensaje) {
        lock.lock();
        try {
            estados.get(rol).put(id, estado);
            publicar(mensaje);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cambia el estado de un actor SIN publicar notificación. Se usa para
     * cambios que acompañan a otro (por ejemplo "el mozo queda libre"),
     * que se van a ver en la próxima foto.
     */
    public void fijar(Rol rol, int id, String estado) {
        lock.lock();
        try {
            estados.get(rol).put(id, estado);
        } finally {
            lock.unlock();
        }
    }

    /** Saca a un actor de la pantalla (por ejemplo un cliente que se fue) y publica. */
    public void quitar(Rol rol, int id, String mensaje) {
        lock.lock();
        try {
            estados.get(rol).remove(id);
            publicar(mensaje);
        } finally {
            lock.unlock();
        }
    }

    /** Publica un evento general sin cambiar el estado de ningún actor. */
    public void evento(String mensaje) {
        lock.lock();
        try {
            publicar(mensaje);
        } finally {
            lock.unlock();
        }
    }

    // ------------------------------------------------------------------
    // Armado del texto (siempre se llama con el lock tomado)
    // ------------------------------------------------------------------

    private void publicar(String mensaje) {
        numeroEvento++;
        double segundos = (System.nanoTime() - inicio) / 1_000_000_000.0;
        String encabezado = String.format("#%05d [%7.2fs] %s", numeroEvento, segundos, mensaje);
        display.publicar(SEPARADOR + "\n" + encabezado + "\n" + armarFoto());
    }

    private String armarFoto() {
        StringBuilder sb = new StringBuilder();
        for (Rol rol : Rol.values()) {
            agregarLinea(sb, rol, estados.get(rol));
        }
        sb.setLength(sb.length() - 1); // saco el último salto de línea
        return sb.toString();
    }

    /** Arma una línea por rol; si no entra en el ancho, sigue en la línea de abajo. */
    private static void agregarLinea(StringBuilder sb, Rol rol, Map<Integer, String> actores) {
        sb.append(String.format("  %-11s", rol.titulo));
        if (actores.isEmpty()) {
            sb.append("-\n");
            return;
        }
        int largo = SANGRIA.length();
        boolean primero = true;
        for (Map.Entry<Integer, String> actor : actores.entrySet()) {
            String item = rol.etiqueta.apply(actor.getKey()) + ": " + actor.getValue();
            if (!primero) {
                if (largo + 3 + item.length() > ANCHO) {
                    sb.append('\n').append(SANGRIA);
                    largo = SANGRIA.length();
                } else {
                    sb.append(" | ");
                    largo += 3;
                }
            }
            sb.append(item);
            largo += item.length();
            primero = false;
        }
        sb.append('\n');
    }
}
