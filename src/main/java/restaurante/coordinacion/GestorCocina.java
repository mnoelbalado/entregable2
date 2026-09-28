package restaurante.coordinacion;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import restaurante.modelo.Estados.EstadoPedido;
import restaurante.modelo.Pedido;
import restaurante.modelo.Plato;

/** Distribuye platos entre cocineros respetando la llegada de los pedidos. */
public final class GestorCocina {

    private final ArrayDeque<Pedido> pedidos = new ArrayDeque<>();
    private final Set<Pedido> pedidosRecibidos =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Trabajo> trabajosActivos =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private boolean recepcionAbierta = true;

    /** Una asignacion concreta de un plato a un cocinero. */
    public static final class Trabajo {
        private final Pedido pedido;
        private final Plato plato;

        private Trabajo(Pedido pedido, Plato plato) {
            this.pedido = pedido;
            this.plato = plato;
        }

        public Pedido getPedido() {
            return pedido;
        }

        public Plato getPlato() {
            return plato;
        }
    }

    /** El mozo entrega un pedido aceptado a la cocina. */
    public synchronized void recibirPedido(Pedido pedido) {
        Objects.requireNonNull(pedido, "El pedido no puede ser null.");
        if (!recepcionAbierta) {
            throw new IllegalStateException("La cocina ya no recibe pedidos.");
        }
        if (pedido.getEstado() != EstadoPedido.REGISTRADO
                || !pedidosRecibidos.add(pedido)) {
            throw new IllegalArgumentException(
                    "El pedido debe ser nuevo y estar registrado.");
        }

        pedidos.addLast(pedido);
        notifyAll();
    }

    /**
     * Devuelve el primer plato pendiente del primer pedido con trabajo.
     * Devuelve null al cerrar la recepcion y agotarse los platos por asignar.
     */
    public synchronized Trabajo tomarTrabajo() throws InterruptedException {
        while (true) {
            Pedido primero = pedidos.peekFirst();
            if (primero != null) {
                Plato plato = primero.tomarPlatoPendiente();
                if (!primero.tienePlatosPendientes()) {
                    pedidos.removeFirst();
                }
                if (plato != null) {
                    Trabajo trabajo = new Trabajo(primero, plato);
                    trabajosActivos.add(trabajo);
                    return trabajo;
                }
                continue;
            }

            if (!recepcionAbierta) {
                return null;
            }
            wait();
        }
    }

    /** Devuelve true solo al terminar el ultimo plato de ese pedido. */
    public synchronized boolean finalizarTrabajo(Trabajo trabajo) {
        if (trabajo == null || !trabajosActivos.contains(trabajo)) {
            throw new IllegalArgumentException("El trabajo no esta activo.");
        }

        boolean pedidoListo = trabajo.pedido.registrarPlatoListo(trabajo.plato);
        trabajosActivos.remove(trabajo);
        notifyAll();
        return pedidoListo;
    }

    /** Invocar solo cuando ningun mozo pueda agregar otro pedido. */
    public synchronized void cerrarRecepcion() {
        recepcionAbierta = false;
        notifyAll();
    }

    public synchronized int getCantidadTrabajosActivos() {
        return trabajosActivos.size();
    }

    public synchronized boolean tieneTrabajo() { return !pedidos.isEmpty(); }

    public synchronized boolean estaCerrada() { return !recepcionAbierta; }
}
