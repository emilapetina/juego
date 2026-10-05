import java.util.concurrent.ThreadLocalRandom;

/**
 * Azar.java
 * Utilidades para generar tiempos aleatorios y simular demoras con sleep.
 * Se usa ThreadLocalRandom (un generador por hilo) para no tener
 * contención entre hilos, cosa que sí pasaría con un Random compartido.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Azar {

    private Azar() {
    }

    /** Número aleatorio entre min y max (ambos incluidos). */
    public static long entre(long min, long max) {
        if (min >= max) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /** Duerme el hilo actual un tiempo aleatorio entre min y max milisegundos. */
    public static void demorar(long min, long max) throws InterruptedException {
        Thread.sleep(entre(min, max));
    }
}
