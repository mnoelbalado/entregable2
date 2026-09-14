package restaurante.config;

/** Constantes de la simulacion. Todos los tiempos estan en milisegundos. */
public final class Configuracion {

    // Duracion del periodo de admision; la atencion puede continuar despues.
    public static final long T = 60_000;

    public static final int M = 3; // Mesas.
    public static final int P = 2; // Clientes por mesa.
    public static final int Z = 2; // Mozos.
    public static final int C = 2; // Cocineros.
    public static final int Y = 1; // Cajeros.

    public static final long TPmin = 500;
    public static final long TPmax = 1_500;

    public static final long TMmin = 1_000;
    public static final long TMmax = 3_000;

    public static final long TQmin = 2_000;
    public static final long TQmax = 5_000;

    public static final long TZmin = 500;
    public static final long TZmax = 1_500;

    public static final long TRmin = 500;
    public static final long TRmax = 1_500;

    public static final long TLmin = 500;
    public static final long TLmax = 2_000;

    public static final long TYmin = 500;
    public static final long TYmax = 1_500;

    // TCmin y TCmax pertenecen a cada Menu, no a la configuracion global.

    // Anexo obligatorio para grupos de tres integrantes.
    public static final boolean ANEXO_HABILITADO = false;
    public static final int Q = 2; // Total de sartenes.
    public static final long TWmin = 1_000;
    public static final long TWmax = 3_000;

    private Configuracion() {
        // Esta clase no necesita instancias.
    }

    public static int capacidad() {
        return Math.multiplyExact(M, P);
    }

    /** Llamar desde Main antes de crear cualquier hilo. */
    public static void validar() {
        if (T <= 0) {
            throw new IllegalArgumentException("T debe ser mayor que cero.");
        }
        if (M <= 0 || P <= 0 || Z <= 0 || C <= 0 || Y <= 0) {
            throw new IllegalArgumentException("M, P, Z, C e Y deben ser positivos.");
        }

        capacidad(); // Detecta desbordamiento en M * P.
        validarRango("TP", TPmin, TPmax);
        validarRango("TM", TMmin, TMmax);
        validarRango("TQ", TQmin, TQmax);
        validarRango("TZ", TZmin, TZmax);
        validarRango("TR", TRmin, TRmax);
        validarRango("TL", TLmin, TLmax);
        validarRango("TY", TYmin, TYmax);

        if (TPmin == 0) {
            throw new IllegalArgumentException("TPmin debe ser positivo.");
        }
        if (ANEXO_HABILITADO) {
            if (Q < 2) {
                throw new IllegalArgumentException("Q debe ser al menos 2.");
            }
            validarRango("TW", TWmin, TWmax);
            if (TWmin == 0) {
                throw new IllegalArgumentException("TWmin debe ser positivo.");
            }
        }
    }

    private static void validarRango(String nombre, long min, long max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException(
                    nombre + ": se requiere 0 <= minimo <= maximo.");
        }
    }
}
