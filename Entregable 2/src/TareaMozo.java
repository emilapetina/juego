/**
 * TareaMozo.java
 * Una tarea que el restaurante le asigna a un mozo libre. Es inmutable.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public record TareaMozo(Tipo tipo, Mesa mesa) {

    public enum Tipo {
        TOMAR_PEDIDO,
        SERVIR,
        LIMPIAR
    }
}
