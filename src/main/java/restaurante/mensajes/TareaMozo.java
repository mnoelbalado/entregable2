package restaurante.mensajes;

import restaurante.modelo.Mesa;
import restaurante.modelo.Pedido;

/** Mensaje para la cola compartida de mozos. */
public final class TareaMozo {

    public enum Tipo {
        TOMAR_PEDIDO,
        SERVIR_PEDIDO,
        LIMPIAR_MESA,
        FINALIZAR
    }

    private final Tipo tipo;
    private final Mesa mesa;
    private final Pedido pedido;

    private TareaMozo(Tipo tipo, Mesa mesa, Pedido pedido) {
        this.tipo = tipo;
        this.mesa = mesa;
        this.pedido = pedido;
    }

    public static TareaMozo tomarPedido(Mesa mesa) {
        validarMesa(mesa);

        return new TareaMozo(Tipo.TOMAR_PEDIDO, mesa, null);
    }

    public static TareaMozo servirPedido(Mesa mesa, Pedido pedido) {
        validarMesa(mesa);

        if (pedido == null || pedido.getIdMesa() != mesa.getIdMesa()) {
            throw new IllegalArgumentException(
                    "El pedido debe pertenecer a la mesa indicada.");
        }

        return new TareaMozo(Tipo.SERVIR_PEDIDO, mesa, pedido);
    }

    public static TareaMozo limpiarMesa(Mesa mesa) {
        validarMesa(mesa);

        return new TareaMozo(Tipo.LIMPIAR_MESA, mesa, null);
    }

    public static TareaMozo finalizar() {
        return new TareaMozo(Tipo.FINALIZAR, null, null);
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Es null únicamente para FINALIZAR. */
    public Mesa getMesa() {
        return mesa;
    }

    /** Es null salvo para SERVIR_PEDIDO. */
    public Pedido getPedido() {
        return pedido;
    }

    private static void validarMesa(Mesa mesa) {
        if (mesa == null) {
            throw new IllegalArgumentException(
                    "La mesa no puede ser null.");
        }
    }

    @Override
    public String toString() {
        return "TareaMozo{tipo=" + tipo
                + ", mesa="
                + (mesa == null ? "sin mesa" : mesa.getIdMesa())
                + ", pedido="
                + (pedido == null ? "sin pedido" : pedido.getIdPedido())
                + "}";
    }
}