import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main.java
 * Arma el restaurante, larga todos los hilos, deja correr la simulación
 * durante T y después hace el cierre ordenado:
 *
 *  1. Se cierran las puertas (no entra nadie más; los que no pidieron se
 *     van) y se dejan de generar clientes.
 *  2. Se espera a que terminen TODOS los clientes que ya estaban adentro
 *     (los que pidieron siguen normalmente: comen, pagan y se van).
 *  3. Recién ahí se avisa al personal que termina la jornada: los mozos
 *     limpian las mesas que queden, la cocina y las cajas cierran.
 *  4. El display muestra todo lo que quedó en la cola y termina.
 *
 * Todos los hilos corren en pools de hilos (Executors), uno por rol
 * (patrón Working Threads).
 *
 * Uso: java Main [segundos]   (por defecto T = Config.T)
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Main {

    public static void main(String[] args) throws InterruptedException {
        long duracion = args.length > 0 ? Long.parseLong(args[0]) * 1_000 : Config.T;

        // ---------------- Display (consumidor de notificaciones) ----------------
        Display display = new Display(Path.of(Config.ARCHIVO_LOG));
        ExecutorService poolDisplay = Executors.newSingleThreadExecutor();
        poolDisplay.execute(display);

        // ---------------- Recursos compartidos ----------------
        EstadoRestaurante estado = new EstadoRestaurante(display);
        Estadisticas estadisticas = new Estadisticas();
        Restaurante restaurante = new Restaurante(Config.M, Config.P, estado, estadisticas);
        Cocina cocina = new Cocina(estado);
        Cajas cajas = new Cajas(estado);

        estado.evento(String.format("*** APERTURA *** T=%ds, M=%d mesas, P=%d por mesa, Z=%d mozos, "
                        + "C=%d cocineros, Y=%d cajas", duracion / 1_000, Config.M, Config.P, Config.Z,
                Config.C, Config.Y));

        // ---------------- Personal (pools de tamaño fijo) ----------------
        ExecutorService poolMozos = Executors.newFixedThreadPool(Config.Z);
        ExecutorService poolCocineros = Executors.newFixedThreadPool(Config.C);
        ExecutorService poolCajeros = Executors.newFixedThreadPool(Config.Y);
        for (int i = 1; i <= Config.Z; i++) {
            poolMozos.execute(new Mozo(i, restaurante, cocina, estado));
        }
        for (int i = 1; i <= Config.C; i++) {
            poolCocineros.execute(new Cocinero(i, cocina, restaurante, estado, estadisticas));
        }
        for (int i = 1; i <= Config.Y; i++) {
            poolCajeros.execute(new Cajero(i, cajas, estado, estadisticas));
        }

        // ---------------- Clientes ----------------
        // Pool "cached": crea un hilo por cliente que llega y reutiliza los
        // hilos de clientes que ya se fueron.
        ExecutorService poolClientes = Executors.newCachedThreadPool();
        ExecutorService poolPuerta = Executors.newSingleThreadExecutor();
        poolPuerta.execute(new Generador(poolClientes,
                () -> new Cliente(restaurante, cajas, estado, estadisticas),
                Config.TP_MIN, Config.TP_MAX, restaurante::estaAbierto));

        // ---------------- Simulación ----------------
        Thread.sleep(duracion);

        // ---------------- Cierre ----------------
        // 1. Puertas cerradas y no se generan más clientes
        restaurante.cerrar();
        poolPuerta.shutdownNow(); // interrumpe el sleep del generador
        esperar(poolPuerta, "la puerta", estado);

        // 2. Se espera a que se vayan todos los clientes
        poolClientes.shutdown();
        esperar(poolClientes, "clientes", estado);
        estado.evento("Ya no quedan clientes en el restaurante: el personal termina lo pendiente");

        // 3. Fin de la jornada para el personal
        restaurante.terminarJornada();
        cocina.cerrar(Config.C);
        cajas.cerrar(Config.Y);
        poolMozos.shutdown();
        poolCocineros.shutdown();
        poolCajeros.shutdown();
        esperar(poolMozos, "mozos", estado);
        esperar(poolCocineros, "cocineros", estado);
        esperar(poolCajeros, "cajeros", estado);

        estado.evento("*** RESTAURANTE CERRADO ***\n" + estadisticas.resumen());

        // 4. El display muestra lo que queda en la cola y termina
        display.finalizar();
        poolDisplay.shutdown();
        if (!poolDisplay.awaitTermination(Config.ESPERA_MAXIMA_CIERRE, TimeUnit.MILLISECONDS)) {
            poolDisplay.shutdownNow();
        }
        System.out.println("\nLog guardado en " + Path.of(Config.ARCHIVO_LOG).toAbsolutePath());
    }

    /**
     * Espera a que termine un pool. Si se pasa del tiempo máximo (no
     * debería pasar) se fuerza con shutdownNow() para no colgar el programa.
     */
    private static void esperar(ExecutorService pool, String nombre, EstadoRestaurante estado)
            throws InterruptedException {
        if (!pool.awaitTermination(Config.ESPERA_MAXIMA_CIERRE, TimeUnit.MILLISECONDS)) {
            estado.evento("ATENCIÓN: los hilos de " + nombre + " no terminaron a tiempo, se interrumpen");
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
