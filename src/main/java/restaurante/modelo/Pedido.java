package restaurante.modelo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import restaurante.modelo.Estados.EstadoPedido;
import restaurante.modelo.Estados.EstadoPlato;

/** Pedido aceptado de una mesa. Coordina el progreso de sus platos. */
public final class Pedido {

    private final int idPedido;
    private final int idMesa;
    private final List<Plato> platos;
    private EstadoPedido estado;

    public Pedido(int idPedido, int idMesa, List<Plato> platos) {
        if (idPedido <= 0 || idMesa <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores deben ser positivos.");
        }

        if (platos == null || platos.isEmpty()) {
            throw new IllegalArgumentException(
                    "El pedido debe contener platos.");
        }

        Set<Integer> clientes = new HashSet<>();

        for (Plato plato : platos) {
            if (plato == null) {
                throw new IllegalArgumentException(
                        "El pedido no puede contener platos null.");
            }

            if (!clientes.add(plato.getIdCliente())) {
                throw new IllegalArgumentException(
                        "Debe haber un solo plato por cliente.");
            }

            if (plato.getEstado() != EstadoPlato.PENDIENTE) {
                throw new IllegalArgumentException(
                        "Todos los platos deben estar pendientes.");
            }
        }

        this.idPedido = idPedido;
        this.idMesa = idMesa;
        this.platos = List.copyOf(platos);
        this.estado = EstadoPedido.REGISTRADO;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public int getIdMesa() {
        return idMesa;
    }

    public List<Plato> getPlatos() {
        return platos;
    }

    public synchronized EstadoPedido getEstado() {
        return estado;
    }

    public synchronized boolean tienePlatosPendientes() {
        return platos.stream().anyMatch(
                plato -> plato.getEstado() == EstadoPlato.PENDIENTE);
    }

    /** Reserva un plato; devuelve null si todos fueron asignados. */
    public synchronized Plato tomarPlatoPendiente() {
        for (Plato plato : platos) {
            if (plato.getEstado() == EstadoPlato.PENDIENTE) {
                plato.iniciarPreparacion();
                estado = EstadoPedido.EN_PREPARACION;
                return plato;
            }
        }

        return null;
    }

    /** Devuelve true solo cuando termina el último plato. */
    public synchronized boolean registrarPlatoListo(Plato plato) {
        if (!platos.contains(plato)) {
            throw new IllegalArgumentException(
                    "El plato no pertenece a este pedido.");
        }

        exigirEstado(EstadoPedido.EN_PREPARACION);
        plato.marcarListo();

        boolean todosListos = platos.stream().allMatch(
                actual -> actual.getEstado() == EstadoPlato.LISTO);

        if (todosListos) {
            estado = EstadoPedido.LISTO;
        }

        return todosListos;
    }

    public synchronized void iniciarEntrega() {
        exigirEstado(EstadoPedido.LISTO);
        estado = EstadoPedido.EN_ENTREGA;
    }

    public synchronized void finalizarEntrega() {
        exigirEstado(EstadoPedido.EN_ENTREGA);

        for (Plato plato : platos) {
            plato.marcarServido();
        }

        estado = EstadoPedido.ENTREGADO;
    }

    // Se invoca únicamente desde métodos sincronizados.
    private void exigirEstado(EstadoPedido esperado) {
        if (estado != esperado) {
            throw new IllegalStateException(
                    "El pedido " + idPedido + " esta " + estado
                            + "; se esperaba " + esperado + ".");
        }
    }

    @Override
    public synchronized String toString() {
        return "Pedido{id=" + idPedido
                + ", mesa=" + idMesa
                + ", estado=" + estado
                + ", platos=" + platos + "}";
    }
}