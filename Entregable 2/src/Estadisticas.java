import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estadisticas.java
 * Contadores globales de la simulación. Los actualizan muchos hilos a la
 * vez, por eso se usan clases atómicas: cada incremento es una operación
 * indivisible (compare-and-set) sin necesidad de bloquear, y los valores
 * son siempre visibles para todos los hilos (semántica volatile).
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Estadisticas {

    private final AtomicInteger clientesLlegaron = new AtomicInteger();
    private final AtomicInteger clientesComieron = new AtomicInteger();
    private final AtomicInteger clientesSinEntrar = new AtomicInteger();
    private final AtomicInteger clientesRetiradosPorCierre = new AtomicInteger();
    private final AtomicInteger mesasAtendidas = new AtomicInteger();
    private final AtomicInteger platosCocinados = new AtomicInteger();
    private final AtomicLong recaudacion = new AtomicLong();

    public void clienteLlego() {
        clientesLlegaron.incrementAndGet();
    }

    public void clienteComio() {
        clientesComieron.incrementAndGet();
    }

    public void clienteSinEntrar() {
        clientesSinEntrar.incrementAndGet();
    }

    public void clienteRetiradoPorCierre() {
        clientesRetiradosPorCierre.incrementAndGet();
    }

    public void mesaAtendida() {
        mesasAtendidas.incrementAndGet();
    }

    public void platoCocinado() {
        platosCocinados.incrementAndGet();
    }

    public void cobrar(int monto) {
        recaudacion.addAndGet(monto);
    }

    public String resumen() {
        return "RESUMEN DE LA JORNADA\n"
                + "  Clientes que llegaron al salón:          " + clientesLlegaron.get() + "\n"
                + "  Clientes que comieron y pagaron:         " + clientesComieron.get() + "\n"
                + "  Clientes que no llegaron a entrar:       " + clientesSinEntrar.get() + "\n"
                + "  Clientes retirados sin pedir (cierre):   " + clientesRetiradosPorCierre.get() + "\n"
                + "  Mesas atendidas:                         " + mesasAtendidas.get() + "\n"
                + "  Platos cocinados:                        " + platosCocinados.get() + "\n"
                + "  Recaudación total:                       $" + recaudacion.get();
    }
}
