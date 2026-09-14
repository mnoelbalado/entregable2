package restaurante.mensajes;

import java.time.Instant;
import java.util.Objects;

import restaurante.display.EstadoRestaurante;

public final class EventoSimulacion {

    public enum Tipo {
        CAMBIO,
        FINALIZAR
    }

    private final Tipo tipo;
    private final long secuencia;
    private final Instant instante;
    private final String descripcion;
    private final EstadoRestaurante estado;

    private EventoSimulacion(
            Tipo tipo,
            long secuencia,
            Instant instante,
            String descripcion,
            EstadoRestaurante estado) {

        this.tipo = tipo;
        this.secuencia = secuencia;
        this.instante = instante;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public static EventoSimulacion cambio(
            long secuencia,
            Instant instante,
            String descripcion,
            EstadoRestaurante estado) {

        validarDatos(secuencia, instante);

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "La descripcion no puede estar vacia.");
        }

        Objects.requireNonNull(
                estado,
                "La copia del estado no puede ser null.");

        return new EventoSimulacion(
                Tipo.CAMBIO,
                secuencia,
                instante,
                descripcion.strip(),
                estado);
    }

    public static EventoSimulacion finalizar(
            long secuencia,
            Instant instante) {

        validarDatos(secuencia, instante);

        return new EventoSimulacion(
                Tipo.FINALIZAR,
                secuencia,
                instante,
                "Fin de las notificaciones.",
                null);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public long getSecuencia() {
        return secuencia;
    }

    public Instant getInstante() {
        return instante;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Es null únicamente para FINALIZAR. */
    public EstadoRestaurante getEstado() {
        return estado;
    }

    private static void validarDatos(long secuencia, Instant instante) {
        if (secuencia <= 0) {
            throw new IllegalArgumentException(
                    "La secuencia debe ser positiva.");
        }

        Objects.requireNonNull(
                instante,
                "El instante no puede ser null.");
    }

    @Override
    public String toString() {
        String encabezado = "[" + secuencia + "] "
                + instante + " - " + descripcion;

        if (tipo == Tipo.FINALIZAR) {
            return encabezado;
        }

        return encabezado + "\n" + estado;
    }
}