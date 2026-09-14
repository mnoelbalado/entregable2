package restaurante.modelo;

import restaurante.modelo.Estados.EstadoPlato;

/** Preparación individual de un menú para un cliente. */
public final class Plato {

    private final int idCliente;
    private final Menu menu;
    private EstadoPlato estado;

    public Plato(int idCliente, Menu menu) {
        if (idCliente <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del cliente debe ser positivo.");
        }

        if (menu == null) {
            throw new IllegalArgumentException(
                    "El menu no puede ser null.");
        }

        this.idCliente = idCliente;
        this.menu = menu;
        this.estado = EstadoPlato.PENDIENTE;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public Menu getMenu() {
        return menu;
    }

    public synchronized EstadoPlato getEstado() {
        return estado;
    }

    public synchronized void iniciarPreparacion() {
        cambiarEstado(
                EstadoPlato.PENDIENTE,
                EstadoPlato.EN_PREPARACION);
    }

    public synchronized void marcarListo() {
        cambiarEstado(
                EstadoPlato.EN_PREPARACION,
                EstadoPlato.LISTO);
    }

    public synchronized void marcarServido() {
        cambiarEstado(
                EstadoPlato.LISTO,
                EstadoPlato.SERVIDO);
    }

    // Se invoca únicamente desde métodos sincronizados.
    private void cambiarEstado(
            EstadoPlato esperado, EstadoPlato siguiente) {

        if (estado != esperado) {
            throw new IllegalStateException(
                    "No se puede pasar de " + estado + " a " + siguiente
                            + " para el plato del cliente " + idCliente + ".");
        }

        estado = siguiente;
    }

    @Override
    public synchronized String toString() {
        return "Plato{cliente=" + idCliente
                + ", menu=" + menu.getNombre()
                + ", estado=" + estado + "}";
    }
}