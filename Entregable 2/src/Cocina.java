import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Cocina.java
 * Cola de platos por cocinar. Los mozos son los PRODUCTORES (dejan los
 * pedidos) y los cocineros los CONSUMIDORES (toman platos de a uno).
 *
 * Herramienta: BlockingQueue (patrón productor-consumidor).
 *  - Cuando un mozo deja un pedido, se encolan sus P platos en orden.
 *    recibir() es synchronized para que los platos de un mismo pedido
 *    queden juntos aunque dos mozos dejen pedidos al mismo tiempo; así
 *    la cola respeta el orden de llegada de los pedidos.
 *  - Cada cocinero libre hace take(): toma el primer plato sin cocinar
 *    del primer pedido de la cola. Si no hay platos, take() lo deja
 *    esperando sin gastar CPU. Varios cocineros pueden cocinar platos
 *    del mismo pedido al mismo tiempo.
 *  - tomarPlato() NO es synchronized: si lo fuera, un cocinero dormido
 *    en take() se quedaría con el lock y ningún mozo podría dejar
 *    pedidos (deadlock).
 *  - Para terminar se encola una "píldora venenosa" (FIN) por cocinero.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Cocina {

    /** Marca de fin (se compara por referencia). */
    private static final Plato FIN = new Plato(null, null, null);

    private final BlockingQueue<Plato> platosPorCocinar = new LinkedBlockingQueue<>();
    private final EstadoRestaurante estado;

    public Cocina(EstadoRestaurante estado) {
        this.estado = estado;
        mostrarCola();
    }

    /** Un mozo deja un pedido en la cocina: se encolan todos sus platos juntos. */
    public synchronized void recibir(Pedido pedido) {
        platosPorCocinar.addAll(pedido.platos());
        mostrarCola();
    }

    /** Un cocinero toma el próximo plato (espera si no hay). Devuelve null al cerrar la cocina. */
    public Plato tomarPlato() throws InterruptedException {
        Plato plato = platosPorCocinar.take();
        if (plato == FIN) {
            return null;
        }
        mostrarCola();
        return plato;
    }

    /** Ya no quedan clientes: un FIN por cocinero (al final de la cola). */
    public void cerrar(int cantidadCocineros) {
        for (int i = 0; i < cantidadCocineros; i++) {
            platosPorCocinar.add(FIN);
        }
    }

    private synchronized void mostrarCola() {
        String texto = platosPorCocinar.stream()
                .filter(plato -> plato != FIN)
                .map(plato -> "#" + plato.pedido().numero() + " " + plato.menu())
                .collect(Collectors.joining(", "));
        estado.fijar(EstadoRestaurante.Rol.COCINA, EstadoRestaurante.COLA_PEDIDOS,
                texto.isEmpty() ? "vacía" : "por cocinar: " + texto);
    }
}
