package restaurante.coordinacion;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import restaurante.modelo.*;

/** Una sesion por ocupacion: las barreras nunca se reutilizan con otro grupo. */
public final class GestorMesas {
    public static final class Sesion {
        private final Mesa mesa;
        private final Map<Integer, Menu> elecciones = new LinkedHashMap<>();
        private final CountDownLatch seleccionados;
        private final CountDownLatch decision = new CountDownLatch(1);
        private final CountDownLatch servidos = new CountDownLatch(1);
        private volatile Pedido pedido;
        private boolean cancelada;

        private Sesion(Mesa mesa) {
            this.mesa = mesa;
            seleccionados = new CountDownLatch(mesa.getCapacidad());
        }
        public Mesa getMesa() { return mesa; }
        public Pedido getPedido() { return pedido; }
        public boolean esperarDecision() throws InterruptedException {
            decision.await();
            return pedido != null;
        }
        public void esperarComida() throws InterruptedException { servidos.await(); }
    }

    private final Map<Integer, Sesion> sesiones = new HashMap<>();
    private boolean cerrado;

    public synchronized Sesion sesion(Mesa mesa) {
        Sesion sesion = sesiones.computeIfAbsent(mesa.getIdMesa(), id -> new Sesion(mesa));
        if (cerrado && sesion.pedido == null) cancelar(sesion);
        return sesion;
    }

    /** Solo la ultima eleccion produce una llamada al mozo. */
    public synchronized boolean seleccionar(Sesion sesion, int cliente, Menu menu) {
        if (sesion.cancelada) return false;
        if (!sesion.mesa.getClientes().contains(cliente) || menu == null
                || sesion.elecciones.containsKey(cliente)) {
            throw new IllegalArgumentException("Eleccion invalida o duplicada.");
        }
        sesion.elecciones.put(cliente, menu);
        sesion.seleccionados.countDown();
        return sesion.seleccionados.getCount() == 0;
    }

    public synchronized Pedido aceptar(Sesion sesion, int idPedido) {
        if (cerrado || sesion.cancelada) return null;
        if (sesion.pedido != null || sesion.seleccionados.getCount() != 0) {
            throw new IllegalStateException("El grupo no esta listo o ya realizo el pedido.");
        }
        List<Plato> platos = sesion.elecciones.entrySet().stream()
                .map(e -> new Plato(e.getKey(), e.getValue())).toList();
        Pedido pedido = new Pedido(idPedido, sesion.mesa.getIdMesa(), platos);
        sesion.mesa.registrarPedido(pedido);
        sesion.pedido = pedido;
        sesion.decision.countDown();
        return pedido;
    }

    public synchronized void servir(Sesion sesion) {
        sesion.pedido.finalizarEntrega();
        sesion.servidos.countDown();
    }

    public synchronized void liberar(Mesa mesa) { sesiones.remove(mesa.getIdMesa()); }

    public synchronized void cerrar() {
        cerrado = true;
        for (Sesion sesion : sesiones.values()) {
            if (sesion.pedido == null) cancelar(sesion);
        }
    }

    private void cancelar(Sesion sesion) {
        sesion.cancelada = true;
        sesion.decision.countDown();
    }
}
