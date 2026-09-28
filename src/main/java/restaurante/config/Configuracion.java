package restaurante.config;

import java.util.List;
import restaurante.modelo.Menu;
import restaurante.util.Tiempos;

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

    /** Rango inclusivo e inmutable, compartido por los actores. */
    public record Rango(long min, long max) {
        public Rango { validarRango("Tiempo", min, max); }
        public long sortear() { return Tiempos.aleatorio(min, max); }
        public void esperar() throws InterruptedException { Tiempos.esperar(sortear()); }
    }

    /** Permite probar otras capacidades y tiempos sin modificar las constantes. */
    public record Parametros(long duracion, int mesas, int personas, int mozos,
            int cocineros, int cajeros, Rango llegada, Rango seleccion,
            Rango comida, Rango pedido, Rango entrega, Rango limpieza,
            Rango cobro, List<Menu> carta) {
        public Parametros {
            if (duracion <= 0 || mesas <= 0 || personas <= 0 || mozos <= 0
                    || cocineros <= 0 || cajeros <= 0) {
                throw new IllegalArgumentException("Duracion y cantidades deben ser positivas.");
            }
            Math.multiplyExact(mesas, personas);
            for (Rango rango : new Rango[]{llegada, seleccion, comida, pedido,
                    entrega, limpieza, cobro}) {
                java.util.Objects.requireNonNull(rango, "Falta un rango de tiempos.");
            }
            if (llegada.min() == 0) throw new IllegalArgumentException("Llegada debe ser positiva.");
            carta = List.copyOf(carta);
            if (carta.isEmpty()) throw new IllegalArgumentException("La carta no puede estar vacia.");
        }
    }

    public static Parametros predeterminados() {
        validar();
        return new Parametros(T, M, P, Z, C, Y,
                new Rango(TPmin, TPmax), new Rango(TMmin, TMmax),
                new Rango(TQmin, TQmax), new Rango(TZmin, TZmax),
                new Rango(TRmin, TRmax), new Rango(TLmin, TLmax),
                new Rango(TYmin, TYmax), List.of(
                        new Menu("Pasta", 1000, 2000),
                        new Menu("Milanesa", 2000, 3500),
                        new Menu("Pescado", 1500, 2800)));
    }

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
    }

    private static void validarRango(String nombre, long min, long max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException(
                    nombre + ": se requiere 0 <= minimo <= maximo.");
        }
    }
}
