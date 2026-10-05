/**
 * Cajero.java
 * Un cajero: le cobra al primero de la fila única. Si no hay nadie para
 * pagar se queda esperando (take() de la BlockingQueue).
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Cajero implements Runnable {

    private final int id;
    private final Cajas cajas;
    private final EstadoRestaurante estado;
    private final Estadisticas estadisticas;

    public Cajero(int id, Cajas cajas, EstadoRestaurante estado, Estadisticas estadisticas) {
        this.id = id;
        this.cajas = cajas;
        this.estado = estado;
        this.estadisticas = estadisticas;
    }

    @Override
    public void run() {
        estado.actualizar(EstadoRestaurante.Rol.CAJA, id, "Esperando clientes", this + " abre su caja");
        try {
            Cajas.Cobro cobro;
            while ((cobro = cajas.siguiente()) != null) {
                Cliente cliente = cobro.cliente();
                Menu menu = cobro.menu();
                estado.actualizar(EstadoRestaurante.Rol.CAJA, id, "Cobrando a " + cliente.corto(),
                        this + " le cobra " + menu + " ($" + menu.precio() + ") a " + cliente);
                Azar.demorar(Config.TY_MIN, Config.TY_MAX);
                estadisticas.cobrar(menu.precio());
                estado.fijar(EstadoRestaurante.Rol.CAJA, id, "Esperando clientes");
                cobro.pagado().countDown();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        estado.actualizar(EstadoRestaurante.Rol.CAJA, id, "Caja cerrada", this + " cierra su caja");
    }

    @Override
    public String toString() {
        return "Cajero " + id;
    }
}
