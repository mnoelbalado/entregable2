package restaurante.modelo;

/** Opción de la carta, inmutable y compartible entre hilos. */
public final class Menu {

    private final String nombre;
    private final long tiempoCoccionMin;
    private final long tiempoCoccionMax;

    public Menu(String nombre, long tiempoCoccionMin, long tiempoCoccionMax) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del menu no puede estar vacio.");
        }

        if (tiempoCoccionMin < 0 || tiempoCoccionMax < tiempoCoccionMin) {
            throw new IllegalArgumentException(
                    "Los tiempos deben cumplir 0 <= minimo <= maximo.");
        }

        this.nombre = nombre.strip();
        this.tiempoCoccionMin = tiempoCoccionMin;
        this.tiempoCoccionMax = tiempoCoccionMax;
    }

    public String getNombre() {
        return nombre;
    }

    public long getTiempoCoccionMin() {
        return tiempoCoccionMin;
    }

    public long getTiempoCoccionMax() {
        return tiempoCoccionMax;
    }

    @Override
    public String toString() {
        return nombre;
    }
}