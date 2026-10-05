/**
 * Config.java
 * Constantes de configuración de la simulación. Todos los tiempos están
 * expresados en milisegundos. Los nombres siguen la letra del entregable
 * (T, M, P, Z, C, Y, TPmin/TPmax, ...).
 *
 * Entregable 2 - Concurrencia - Programación Avanzada 2026
 */
public final class Config {

    private Config() {
    }

    // ------------------------------------------------------------------
    // Duración y cantidades
    // ------------------------------------------------------------------

    /** T: duración de la simulación (se puede cambiar pasando los segundos como argumento). */
    public static final long T = 30_000;

    /** M: cantidad de mesas. */
    public static final int M = 4;
    /** P: personas por mesa. */
    public static final int P = 3;
    /** Z: cantidad de mozos. */
    public static final int Z = 3;
    /** C: cantidad de cocineros. */
    public static final int C = 3;
    /** Y: cantidad de cajas (un cajero por caja). */
    public static final int Y = 2;

    // ------------------------------------------------------------------
    // Tiempos (ms)
    // ------------------------------------------------------------------

    /** Cada cuánto llega un cliente nuevo al restaurante. */
    public static final long TP_MIN = 400, TP_MAX = 1_200;
    /** Cuánto demora un cliente en elegir el menú. */
    public static final long TM_MIN = 500, TM_MAX = 1_500;
    /** Cuánto demora un cliente en comer. */
    public static final long TQ_MIN = 2_000, TQ_MAX = 4_000;
    /** Cuánto demora un mozo en anotar el pedido y llevarlo a la cocina. */
    public static final long TZ_MIN = 400, TZ_MAX = 800;
    /** Cuánto demora un mozo en retirar los platos de la cocina y servirlos. */
    public static final long TR_MIN = 400, TR_MAX = 800;
    /** Cuánto demora un mozo en levantar los platos y limpiar la mesa. */
    public static final long TL_MIN = 400, TL_MAX = 800;
    /** Cuánto demora un cajero en cobrarle a un cliente. */
    public static final long TY_MIN = 300, TY_MAX = 700;
    // TCmin / TCmax dependen de cada menú: ver Menu.java

    // ------------------------------------------------------------------
    // Otros
    // ------------------------------------------------------------------

    /** Archivo de texto donde se guarda el log de toda la simulación. */
    public static final String ARCHIVO_LOG = "restaurante.log";

    /** Tiempo máximo que se espera a que terminen los hilos luego del cierre. */
    public static final long ESPERA_MAXIMA_CIERRE = 60_000;
}
