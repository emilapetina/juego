import java.util.concurrent.ThreadLocalRandom;

/**
 * Menu.java
 * Lista de menús disponibles en la carta. Cada menú tiene su propio rango
 * de tiempo de cocción (TCmin, TCmax) y un precio.
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public enum Menu {
    MILANESA("Milanesa con fritas", 1_200, 2_200, 520),
    PIZZA("Pizza", 1_500, 2_600, 480),
    CHIVITO("Chivito", 1_400, 2_400, 650),
    NOQUIS("Ñoquis con tuco", 1_000, 1_800, 420),
    ENSALADA("Ensalada César", 400, 900, 350),
    HAMBURGUESA("Hamburguesa", 900, 1_600, 450);

    private final String nombre;
    private final long tcMin;
    private final long tcMax;
    private final int precio;

    Menu(String nombre, long tcMin, long tcMax, int precio) {
        this.nombre = nombre;
        this.tcMin = tcMin;
        this.tcMax = tcMax;
        this.precio = precio;
    }

    /** Elige un menú al azar de la carta. */
    public static Menu aleatorio() {
        Menu[] carta = values();
        return carta[ThreadLocalRandom.current().nextInt(carta.length)];
    }

    public long tcMin() {
        return tcMin;
    }

    public long tcMax() {
        return tcMax;
    }

    public int precio() {
        return precio;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
