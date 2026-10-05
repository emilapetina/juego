import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Generador.java
 * "La puerta de la calle": cada un tiempo aleatorio entre min y max crea
 * un cliente nuevo y lo manda a ejecutar en el pool de clientes
 * (TPmin..TPmax). Deja de generar cuando el restaurante cierra o cuando
 * lo interrumpen (shutdownNow del pool de generadores).
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Generador implements Runnable {

    private final ExecutorService destino;
    private final Supplier<Runnable> fabrica;
    private final long min;
    private final long max;
    private final BooleanSupplier abierto;

    public Generador(ExecutorService destino, Supplier<Runnable> fabrica, long min, long max, BooleanSupplier abierto) {
        this.destino = destino;
        this.fabrica = fabrica;
        this.min = min;
        this.max = max;
        this.abierto = abierto;
    }

    @Override
    public void run() {
        try {
            while (abierto.getAsBoolean()) {
                Azar.demorar(min, max);
                if (abierto.getAsBoolean()) {
                    destino.execute(fabrica.get());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // cierre: dejamos de generar
        } catch (RejectedExecutionException e) {
            // El pool de clientes ya no acepta tareas: también es el cierre
        }
    }
}
