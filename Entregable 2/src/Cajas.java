import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Cajas.java
 * Fila única para pagar, compartida por todos los cajeros. Es un
 * productor-consumidor con BlockingQueue: los clientes encolan un cobro
 * (productores) y los cajeros lo sacan con take() (consumidores), que
 * bloquea si la fila está vacía. La BlockingQueue ya resuelve la
 * exclusión mutua y la espera, sin wait/notify a mano.
 *
 * Cada cobro trae un CountDownLatch(1): el cliente espera en él y el
 * cajero hace countDown() al terminar de cobrarle.
 *
 * Para que los cajeros terminen se encola una "píldora venenosa" (FIN)
 * por cajero cuando ya no quedan clientes.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Cajas {

    /** Un cliente esperando para pagar. */
    public record Cobro(Cliente cliente, Menu menu, CountDownLatch pagado) {
    }

    private static final Cobro FIN = new Cobro(null, null, null);

    private final BlockingQueue<Cobro> fila = new LinkedBlockingQueue<>();
    private final EstadoRestaurante estado;

    public Cajas(EstadoRestaurante estado) {
        this.estado = estado;
        mostrarFila();
    }

    /** El cliente se pone en la fila; devuelve el latch que se libera cuando le cobran. */
    public CountDownLatch hacerFila(Cliente cliente, Menu menu) {
        Cobro cobro = new Cobro(cliente, menu, new CountDownLatch(1));
        fila.add(cobro);
        mostrarFila();
        return cobro.pagado();
    }

    /** El cajero toma al primero de la fila (espera si no hay). Devuelve null al cerrar. */
    public Cobro siguiente() throws InterruptedException {
        Cobro cobro = fila.take();
        if (cobro == FIN) {
            return null;
        }
        mostrarFila();
        return cobro;
    }

    /** Ya no quedan clientes: un FIN por cajero (al final de la fila). */
    public void cerrar(int cantidadCajeros) {
        for (int i = 0; i < cantidadCajeros; i++) {
            fila.add(FIN);
        }
    }

    private synchronized void mostrarFila() {
        String texto = fila.stream()
                .filter(cobro -> cobro != FIN)
                .map(cobro -> cobro.cliente().corto())
                .collect(Collectors.joining(", "));
        estado.fijar(EstadoRestaurante.Rol.CAJA, EstadoRestaurante.FILA_CAJA, texto.isEmpty() ? "vacía" : texto);
    }
}
