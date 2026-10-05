/**
 * Plato.java
 * Un plato dentro de un pedido: qué menú es y para quién. Es inmutable,
 * así que se puede pasar entre hilos sin sincronización extra.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public record Plato(Pedido pedido, Menu menu, String comensal) {

    @Override
    public String toString() {
        return comensal + ": " + menu;
    }
}
