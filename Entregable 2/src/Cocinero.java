/**
 * Cocinero.java
 * Un cocinero: toma el primer plato sin cocinar de la cola de la cocina
 * (si no hay, espera), lo cocina (TCmin..TCmax del menú) y lo deja
 * pronto. Si con ese plato se completa el pedido, avisa a los mozos.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Cocinero implements Runnable {

    private final int id;
    private final Cocina cocina;
    private final Restaurante restaurante;
    private final EstadoRestaurante estado;
    private final Estadisticas estadisticas;

    public Cocinero(int id, Cocina cocina, Restaurante restaurante, EstadoRestaurante estado,
                    Estadisticas estadisticas) {
        this.id = id;
        this.cocina = cocina;
        this.restaurante = restaurante;
        this.estado = estado;
        this.estadisticas = estadisticas;
    }

    @Override
    public void run() {
        estado.actualizar(EstadoRestaurante.Rol.COCINERO, id, "Esperando pedidos", this + " entra a la cocina");
        try {
            Plato plato;
            while ((plato = cocina.tomarPlato()) != null) {
                cocinar(plato);
                estado.fijar(EstadoRestaurante.Rol.COCINERO, id, "Esperando pedidos");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        estado.actualizar(EstadoRestaurante.Rol.COCINERO, id, "Terminó su turno", this + " termina su turno");
    }

    private void cocinar(Plato plato) throws InterruptedException {
        Pedido pedido = plato.pedido();
        String que = plato.menu() + " (" + pedido + ", " + pedido.mesa() + ")";
        estado.actualizar(EstadoRestaurante.Rol.COCINERO, id, "Cocinando " + plato.menu(),
                this + " empieza a cocinar " + que);
        Azar.demorar(plato.menu().tcMin(), plato.menu().tcMax());
        estadisticas.platoCocinado();
        estado.actualizar(EstadoRestaurante.Rol.COCINERO, id, "Dejó listo " + plato.menu(),
                this + " deja listo " + que);

        // Si era el último plato del pedido, avisa a los mozos que pueden retirarlo
        if (pedido.platoListo()) {
            restaurante.pedidoListo(pedido);
        }
    }

    @Override
    public String toString() {
        return "Cocinero " + id;
    }
}
