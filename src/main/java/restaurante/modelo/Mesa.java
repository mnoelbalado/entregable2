package restaurante.modelo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import restaurante.modelo.Estados.EstadoMesa;
import restaurante.modelo.Estados.EstadoPedido;

public final class Mesa {

    private final int idMesa;
    private final int capacidad;

    private EstadoMesa estado = EstadoMesa.LIBRE;
    private List<Integer> clientes = List.of();
    private final Set<Integer> clientesPresentes = new HashSet<>();
    private Pedido pedido;

    public Mesa(int idMesa, int capacidad) {
        if (idMesa <= 0 || capacidad <= 0) {
            throw new IllegalArgumentException(
                    "El identificador y la capacidad deben ser positivos.");
        }

        this.idMesa = idMesa;
        this.capacidad = capacidad;
    }

    public int getIdMesa() {
        return idMesa;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public synchronized EstadoMesa getEstado() {
        return estado;
    }

    public synchronized List<Integer> getClientes() {
        return clientes;
    }

    public synchronized int getCantidadClientesPresentes() {
        return clientesPresentes.size();
    }

    public synchronized Pedido getPedido() {
        return pedido;
    }

    public synchronized void ocupar(List<Integer> idsClientes) {
        exigirEstado(EstadoMesa.LIBRE);

        if (idsClientes == null || idsClientes.size() != capacidad) {
            throw new IllegalArgumentException(
                    "La mesa requiere exactamente " + capacidad + " clientes.");
        }

        Set<Integer> idsUnicos = new HashSet<>();

        for (Integer id : idsClientes) {
            if (id == null || id <= 0 || !idsUnicos.add(id)) {
                throw new IllegalArgumentException(
                        "Los clientes deben tener identificadores positivos y distintos.");
            }
        }

        clientes = List.copyOf(idsClientes);
        clientesPresentes.addAll(idsUnicos);
        estado = EstadoMesa.OCUPADA;
    }

    public synchronized void registrarPedido(Pedido nuevoPedido) {
        exigirEstado(EstadoMesa.OCUPADA);

        if (pedido != null) {
            throw new IllegalStateException(
                    "La mesa ya tiene un pedido.");
        }

        if (nuevoPedido == null || nuevoPedido.getIdMesa() != idMesa) {
            throw new IllegalArgumentException(
                    "El pedido debe pertenecer a esta mesa.");
        }

        if (clientesPresentes.size() != capacidad) {
            throw new IllegalStateException(
                    "No se puede registrar un pedido con clientes ausentes.");
        }

        if (nuevoPedido.getEstado() != EstadoPedido.REGISTRADO) {
            throw new IllegalArgumentException(
                    "El pedido debe estar recien registrado.");
        }

        if (nuevoPedido.getPlatos().size() != capacidad) {
            throw new IllegalArgumentException(
                    "Debe haber un plato por cliente de la mesa.");
        }

        Set<Integer> destinatarios = new HashSet<>();

        for (Plato plato : nuevoPedido.getPlatos()) {
            destinatarios.add(plato.getIdCliente());
        }

        if (!destinatarios.equals(clientesPresentes)) {
            throw new IllegalArgumentException(
                    "Los platos deben corresponder a los clientes de la mesa.");
        }

        pedido = nuevoPedido;
    }

    /**
     * Devuelve true cuando sale el último cliente.
     * Se utiliza después del pago y la salida, o al retirarse
     * sin pedido por el cierre del restaurante.
     */
    public synchronized boolean registrarSalida(int idCliente) {
        exigirEstado(EstadoMesa.OCUPADA);

        if (!clientesPresentes.remove(idCliente)) {
            throw new IllegalArgumentException(
                    "El cliente no esta presente en esta mesa.");
        }

        if (clientesPresentes.isEmpty()) {
            estado = EstadoMesa.PENDIENTE_LIMPIEZA;
            return true;
        }

        return false;
    }

    public synchronized void iniciarLimpieza() {
        exigirEstado(EstadoMesa.PENDIENTE_LIMPIEZA);
        estado = EstadoMesa.EN_LIMPIEZA;
    }

    public synchronized void finalizarLimpieza() {
        exigirEstado(EstadoMesa.EN_LIMPIEZA);

        clientes = List.of();
        pedido = null;
        estado = EstadoMesa.LIBRE;
    }

    // Se invoca únicamente desde métodos sincronizados.
    private void exigirEstado(EstadoMesa esperado) {
        if (estado != esperado) {
            throw new IllegalStateException(
                    "La mesa " + idMesa + " esta " + estado
                            + "; se esperaba " + esperado + ".");
        }
    }

    @Override
    public synchronized String toString() {
        return "Mesa{id=" + idMesa
                + ", estado=" + estado
                + ", clientes=" + clientes
                + ", presentes=" + clientesPresentes.size()
                + ", pedido="
                + (pedido == null ? "sin pedido" : pedido.getIdPedido())
                + "}";
    }
}