package restaurante.mensajes;

import java.util.concurrent.CountDownLatch;

import restaurante.modelo.Menu;

public final class SolicitudCobro {

    public enum Tipo {
        COBRAR,
        FINALIZAR
    }

    private final Tipo tipo;
    private final int idCliente;
    private final Menu menu;
    private final CountDownLatch cobroCompletado;

    private SolicitudCobro(Tipo tipo, int idCliente, Menu menu) {
        this.tipo = tipo;
        this.idCliente = idCliente;
        this.menu = menu;

        this.cobroCompletado = new CountDownLatch(
                tipo == Tipo.COBRAR ? 1 : 0);
    }

    public static SolicitudCobro cobrar(int idCliente, Menu menu) {
        if (idCliente <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del cliente debe ser positivo.");
        }

        if (menu == null) {
            throw new IllegalArgumentException(
                    "El menu no puede ser null.");
        }

        return new SolicitudCobro(Tipo.COBRAR, idCliente, menu);
    }

    public static SolicitudCobro finalizar() {
        return new SolicitudCobro(Tipo.FINALIZAR, 0, null);
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** El valor 0 corresponde al mensaje FINALIZAR. */
    public int getIdCliente() {
        return idCliente;
    }

    /** Es null únicamente para FINALIZAR. */
    public Menu getMenu() {
        return menu;
    }

    /** El cliente espera hasta que el cajero complete el cobro. */
    public void esperarCobro() throws InterruptedException {
        exigirSolicitudDeCobro();
        cobroCompletado.await();
    }

    /** El cajero avisa que terminó de cobrar. */
    public synchronized void marcarCobrado() {
        exigirSolicitudDeCobro();

        if (cobroCompletado.getCount() == 0) {
            throw new IllegalStateException(
                    "Esta solicitud ya fue cobrada.");
        }

        cobroCompletado.countDown();
    }

    private void exigirSolicitudDeCobro() {
        if (tipo != Tipo.COBRAR) {
            throw new IllegalStateException(
                    "El mensaje FINALIZAR no representa un cobro.");
        }
    }

    @Override
    public String toString() {
        return "SolicitudCobro{tipo=" + tipo
                + ", cliente=" + idCliente
                + ", menu="
                + (menu == null ? "sin menu" : menu.getNombre())
                + "}";
    }
}