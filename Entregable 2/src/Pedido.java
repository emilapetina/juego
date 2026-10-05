import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Pedido.java
 * El pedido de una mesa: un plato por cada uno de los P clientes. Los
 * cocineros cocinan los platos de a uno, así que varios cocineros pueden
 * estar cocinando platos del mismo pedido al mismo tiempo.
 *
 * "listos" es un AtomicInteger porque cada cocinero lo incrementa al
 * terminar su plato, fuera de cualquier lock. Como incrementAndGet() es
 * atómico, exactamente UN cocinero ve el valor final (todos los platos
 * prontos) y es el único que avisa a los mozos: ni dos avisos ni ninguno.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Pedido {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final int numero = SECUENCIA.incrementAndGet();
    private final Mesa mesa;
    private final List<Plato> platos;
    private final AtomicInteger listos = new AtomicInteger();

    /** Arma el pedido con lo que eligió cada cliente sentado en la mesa. */
    public Pedido(Mesa mesa) {
        this.mesa = mesa;
        List<Plato> lista = new ArrayList<>();
        for (Cliente cliente : mesa.comensales()) {
            lista.add(new Plato(this, cliente.menu(), cliente.corto()));
        }
        this.platos = Collections.unmodifiableList(lista);
    }

    /** Marca un plato como listo; devuelve true si era el último del pedido. */
    public boolean platoListo() {
        return listos.incrementAndGet() == platos.size();
    }

    public int numero() {
        return numero;
    }

    public Mesa mesa() {
        return mesa;
    }

    public List<Plato> platos() {
        return platos;
    }

    /** Detalle de los platos, por ejemplo "C1: Pizza, C2: Chivito". */
    public String detalle() {
        return platos.stream().map(Plato::toString).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return "Pedido #" + numero;
    }
}
