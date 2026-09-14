package restaurante.util;

import java.util.concurrent.ThreadLocalRandom;

/** Todas las duraciones están en milisegundos. */
public final class Tiempos {

    private Tiempos() {
    }

    /** Genera una duración aleatoria incluyendo ambos extremos. */
    public static long aleatorio(long min, long max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException(
                    "Los tiempos deben cumplir 0 <= minimo <= maximo.");
        }

        if (min == max) {
            return min;
        }

        // Evita el desbordamiento de max + 1.
        if (max == Long.MAX_VALUE) {
            return ThreadLocalRandom.current()
                    .nextLong(min - 1, max) + 1;
        }

        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /** Pausa únicamente el hilo que llama al método. */
    public static void esperar(long milisegundos)
            throws InterruptedException {

        if (milisegundos < 0) {
            throw new IllegalArgumentException(
                    "La duracion no puede ser negativa.");
        }

        Thread.sleep(milisegundos);
    }

    public static void esperarAleatorio(long min, long max)
            throws InterruptedException {

        esperar(aleatorio(min, max));
    }
}