import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Display.java
 * Hilo encargado de mostrar en pantalla (y guardar en el archivo de log)
 * cada cambio de la simulación. Es el CONSUMIDOR de un esquema
 * productor-consumidor: todos los demás hilos producen notificaciones
 * (ya con la foto del estado del restaurante en ese momento) y las
 * dejan en una BlockingQueue; este hilo las va sacando en orden.
 *
 * Así ningún actor escribe directo en la consola ni en el archivo (que
 * no son thread-safe para escrituras intercaladas) y el orden de los
 * mensajes es exactamente el orden en que ocurrieron los cambios.
 *
 * Para terminar se usa una "píldora venenosa" (FIN): cuando llega, el
 * display ya mostró todo lo anterior que había en la cola y termina.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Display implements Runnable {

    /** Marca de fin (se compara por referencia). */
    private static final String FIN = new String("<<FIN>>");

    private final BlockingQueue<String> cola = new LinkedBlockingQueue<>();
    private final Path archivoLog;

    public Display(Path archivoLog) {
        this.archivoLog = archivoLog;
    }

    /** Encola una notificación para mostrar. Nunca bloquea (cola sin límite). */
    public void publicar(String texto) {
        cola.add(texto);
    }

    /** Pide que el display termine luego de mostrar todo lo que ya está en la cola. */
    public void finalizar() {
        cola.add(FIN);
    }

    @Override
    public void run() {
        PrintWriter log = abrirLog();
        try {
            while (true) {
                String texto = cola.take();
                if (texto == FIN) {
                    break;
                }
                System.out.println(texto);
                if (log != null) {
                    log.println(texto);
                }
            }
        } catch (InterruptedException e) {
            // Si nos interrumpen igual vaciamos lo que quedó para no perder el log
            Thread.currentThread().interrupt();
            for (String texto : cola) {
                if (texto != FIN && log != null) {
                    log.println(texto);
                }
            }
        } finally {
            if (log != null) {
                log.close();
            }
        }
    }

    private PrintWriter abrirLog() {
        try {
            return new PrintWriter(Files.newBufferedWriter(archivoLog, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("No se pudo abrir el archivo de log " + archivoLog + ": " + e.getMessage()
                    + ". Se muestra solo en pantalla.");
            return null;
        }
    }
}
