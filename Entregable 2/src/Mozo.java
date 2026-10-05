/**
 * Mozo.java
 * Un mozo del salón. No tiene mesas asignadas: le pide al restaurante la
 * próxima tarea (servir, limpiar o tomar un pedido) y si no hay ninguna
 * se queda esperando (wait dentro del monitor del restaurante).
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Mozo implements Runnable {

    private final int id;
    private final Restaurante restaurante;
    private final Cocina cocina;
    private final EstadoRestaurante estado;

    public Mozo(int id, Restaurante restaurante, Cocina cocina, EstadoRestaurante estado) {
        this.id = id;
        this.restaurante = restaurante;
        this.cocina = cocina;
        this.estado = estado;
    }

    @Override
    public void run() {
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Esperando", this + " empieza su turno");
        try {
            TareaMozo tarea;
            while ((tarea = restaurante.siguienteTarea()) != null) {
                switch (tarea.tipo()) {
                    case TOMAR_PEDIDO -> tomarPedido(tarea.mesa());
                    case SERVIR -> servir(tarea.mesa());
                    case LIMPIAR -> limpiar(tarea.mesa());
                }
                estado.fijar(EstadoRestaurante.Rol.MOZO, id, "Esperando");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Terminó su turno", this + " termina su turno");
    }

    private void tomarPedido(Mesa mesa) throws InterruptedException {
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Tomando pedido de la " + mesa,
                this + " va a la " + mesa + " a tomar el pedido");
        Azar.demorar(Config.TZ_MIN, Config.TZ_MAX);
        Pedido pedido = new Pedido(mesa);
        restaurante.pedidoEnCocina(mesa);
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Dejó el " + pedido + " en cocina",
                this + " pasa a la cocina el " + pedido + " de la " + mesa + " -> " + pedido.detalle());
        cocina.recibir(pedido);
    }

    private void servir(Mesa mesa) throws InterruptedException {
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Llevando platos a la " + mesa,
                this + " retira de la cocina los platos de la " + mesa);
        Azar.demorar(Config.TR_MIN, Config.TR_MAX);
        restaurante.mesaServida(mesa);
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Sirvió la " + mesa,
                this + " sirve los platos en la " + mesa);
    }

    private void limpiar(Mesa mesa) throws InterruptedException {
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Limpiando la " + mesa,
                this + " levanta los platos y limpia la " + mesa);
        Azar.demorar(Config.TL_MIN, Config.TL_MAX);
        restaurante.mesaLimpia(mesa);
        estado.actualizar(EstadoRestaurante.Rol.MOZO, id, "Limpió la " + mesa,
                this + " terminó de limpiar: la " + mesa + " queda libre");
    }

    @Override
    public String toString() {
        return "Mozo " + id;
    }
}
