package restaurante.coordinacion;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import restaurante.config.Configuracion.Parametros;
import restaurante.display.EstadoRestaurante;
import restaurante.mensajes.*;
import restaurante.modelo.*;
import restaurante.modelo.Estados.*;

/** Integra los gestores. El monitor de ingreso serializa cambios y sus snapshots.
 * Ninguna espera de un actor, cola o temporizacion retiene ese monitor.
 */
public final class Restaurante {
    private final Parametros parametros;
    private final List<Mesa> mesas;
    private final GestorIngreso ingreso;
    private final GestorMesas grupos = new GestorMesas();
    private final GestorCocina cocina = new GestorCocina();
    private final BlockingQueue<TareaMozo> tareas = new LinkedBlockingQueue<>();
    private final BlockingQueue<SolicitudCobro> cobros = new LinkedBlockingQueue<>();
    private final BlockingQueue<EventoSimulacion> eventos = new LinkedBlockingQueue<>();
    private final Map<Integer, EstadoCliente> clientes = new TreeMap<>();
    private final Map<Integer, EstadoMozo> mozos = new TreeMap<>();
    private final Map<Integer, EstadoCocinero> cocineros = new TreeMap<>();
    private final Map<Integer, EstadoCajero> cajeros = new TreeMap<>();
    private final CountDownLatch cierre = new CountDownLatch(1);
    private final CountDownLatch fallo = new CountDownLatch(1);
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private EstadoSimulacion estado = EstadoSimulacion.ABIERTO;
    private long secuencia;
    private int siguientePedido;

    public Restaurante(Parametros parametros) {
        this.parametros = Objects.requireNonNull(parametros);
        List<Mesa> nuevas = new ArrayList<>();
        for (int i = 1; i <= parametros.mesas(); i++) nuevas.add(new Mesa(i, parametros.personas()));
        mesas = List.copyOf(nuevas);
        ingreso = new GestorIngreso(mesas, parametros.personas(), this::cambiarCliente);
        for (int i = 1; i <= parametros.mozos(); i++) mozos.put(i, EstadoMozo.ESPERANDO_TAREA);
        for (int i = 1; i <= parametros.cocineros(); i++) cocineros.put(i, EstadoCocinero.ESPERANDO_TRABAJO);
        for (int i = 1; i <= parametros.cajeros(); i++) cajeros.put(i, EstadoCajero.ESPERANDO_CLIENTE);
        synchronized (ingreso) { publicar("Apertura del restaurante"); }
    }

    public Parametros parametros() { return parametros; }
    public BlockingQueue<EventoSimulacion> eventos() { return eventos; }
    public Throwable error() { return error.get(); }
    public void fallar(Throwable causa) {
        if (error.compareAndSet(null, causa)) fallo.countDown();
    }
    public boolean esperarFallo(long millis) throws InterruptedException {
        return fallo.await(millis, TimeUnit.MILLISECONDS);
    }
    public boolean estaAbierto() {
        synchronized (ingreso) { return estado == EstadoSimulacion.ABIERTO; }
    }
    public boolean esperarSeleccion(long millis) throws InterruptedException {
        return !cierre.await(millis, TimeUnit.MILLISECONDS);
    }
    public GestorMesas.Sesion ingresar(int id) throws InterruptedException {
        Mesa mesa = ingreso.ingresarYEsperarMesa(id);
        if (mesa == null) return null;
        synchronized (ingreso) { return grupos.sesion(mesa); }
    }
    public void seleccionar(GestorMesas.Sesion sesion, int id, Menu menu) {
        synchronized (ingreso) {
            if (estado != EstadoSimulacion.ABIERTO) return;
            boolean completo = grupos.seleccionar(sesion, id, menu);
            clientes.put(id, EstadoCliente.ESPERANDO_MOZO);
            publicar("Cliente " + id + " elige " + menu.getNombre());
            if (completo) tareas.add(TareaMozo.tomarPedido(sesion.getMesa()));
        }
    }
    public boolean iniciarPedido(Mesa mesa, int mozo) {
        synchronized (ingreso) {
            if (estado != EstadoSimulacion.ABIERTO) return false;
            mozos.put(mozo, EstadoMozo.TOMANDO_PEDIDO);
            for (int id : mesa.getClientes()) clientes.put(id, EstadoCliente.REALIZANDO_PEDIDO);
            publicar("Mozo " + mozo + " toma pedido de mesa " + mesa.getIdMesa());
            return true;
        }
    }
    public void aceptarPedido(Mesa mesa) {
        synchronized (ingreso) {
            if (estado != EstadoSimulacion.ABIERTO) return;
            Pedido pedido = grupos.aceptar(grupos.sesion(mesa), ++siguientePedido);
            cocina.recibirPedido(pedido);
            for (int id : mesa.getClientes()) clientes.put(id, EstadoCliente.ESPERANDO_COMIDA);
            publicar("Pedido " + pedido.getIdPedido() + " aceptado: " + pedido);
        }
    }
    public TareaMozo tomarTarea() throws InterruptedException { return tareas.take(); }
    public GestorCocina.Trabajo tomarTrabajo(int id) throws InterruptedException {
        // wait libera el monitor global; la reserva y su snapshot son atomicos.
        synchronized (ingreso) {
            while (!cocina.tieneTrabajo() && !cocina.estaCerrada()) ingreso.wait();
            GestorCocina.Trabajo trabajo = cocina.tomarTrabajo();
            if (trabajo != null) {
                cocineros.put(id, EstadoCocinero.COCINANDO);
                publicar("Cocinero " + id + " prepara cliente " + trabajo.getPlato().getIdCliente()
                        + ", pedido " + trabajo.getPedido().getIdPedido());
            }
            return trabajo;
        }
    }
    public void terminarPlato(GestorCocina.Trabajo trabajo, int id) {
        synchronized (ingreso) {
            boolean listo = cocina.finalizarTrabajo(trabajo);
            cocineros.put(id, EstadoCocinero.ESPERANDO_TRABAJO);
            publicar("Plato listo para cliente " + trabajo.getPlato().getIdCliente());
            if (listo) tareas.add(TareaMozo.servirPedido(
                    mesas.get(trabajo.getPedido().getIdMesa() - 1), trabajo.getPedido()));
        }
    }
    public void iniciarEntrega(TareaMozo tarea, int id) {
        synchronized (ingreso) {
            tarea.getPedido().iniciarEntrega();
            mozos.put(id, EstadoMozo.SIRVIENDO_PEDIDO);
            publicar("Mozo " + id + " entrega pedido " + tarea.getPedido().getIdPedido());
        }
    }
    public void servir(Mesa mesa) {
        synchronized (ingreso) {
            grupos.servir(grupos.sesion(mesa));
            publicar("Mesa " + mesa.getIdMesa() + ": todos los platos servidos");
        }
    }
    public SolicitudCobro solicitarCobro(int cliente, Menu menu) {
        synchronized (ingreso) {
            SolicitudCobro solicitud = SolicitudCobro.cobrar(cliente, menu);
            clientes.put(cliente, EstadoCliente.ESPERANDO_CAJA);
            publicar("Cliente " + cliente + " entra en la cola unica de caja");
            cobros.add(solicitud);
            ingreso.notifyAll();
            return solicitud;
        }
    }
    public SolicitudCobro tomarCobro(int cajero) throws InterruptedException {
        synchronized (ingreso) {
            while (cobros.isEmpty()) ingreso.wait();
            SolicitudCobro solicitud = cobros.remove();
            if (solicitud.getTipo() == SolicitudCobro.Tipo.COBRAR) {
                cajeros.put(cajero, EstadoCajero.COBRANDO);
                clientes.put(solicitud.getIdCliente(), EstadoCliente.PAGANDO);
                publicar("Cajero " + cajero + " cobra a cliente " + solicitud.getIdCliente());
            }
            return solicitud;
        }
    }
    public void terminarCobro(SolicitudCobro solicitud, int cajero) {
        synchronized (ingreso) {
            cajeros.put(cajero, EstadoCajero.ESPERANDO_CLIENTE);
            publicar("Cobro completado para cliente " + solicitud.getIdCliente());
            solicitud.marcarCobrado();
        }
    }
    public void salir(GestorMesas.Sesion sesion, int id) {
        synchronized (ingreso) {
            if (ingreso.registrarSalida(sesion.getMesa(), id)) {
                tareas.add(TareaMozo.limpiarMesa(sesion.getMesa()));
            }
        }
    }
    public void iniciarLimpieza(Mesa mesa, int mozo) {
        synchronized (ingreso) {
            mesa.iniciarLimpieza();
            mozos.put(mozo, EstadoMozo.LIMPIANDO_MESA);
            publicar("Mozo " + mozo + " limpia mesa " + mesa.getIdMesa());
        }
    }
    public void terminarLimpieza(Mesa mesa) {
        synchronized (ingreso) {
            mesa.finalizarLimpieza();
            grupos.liberar(mesa);
            publicar("Mesa " + mesa.getIdMesa() + " limpia y libre");
            ingreso.avisarMesaLibre(mesa);
        }
    }
    public void cambiarCliente(int id, EstadoCliente nuevo) {
        synchronized (ingreso) {
            if (clientes.put(id, nuevo) != nuevo) publicar("Cliente " + id + ": " + nuevo);
        }
    }
    public void cambiarMozo(int id, EstadoMozo nuevo) {
        synchronized (ingreso) { mozos.put(id, nuevo); publicar("Mozo " + id + ": " + nuevo); }
    }
    public void finalizarCocinero(int id) {
        synchronized (ingreso) { cocineros.put(id, EstadoCocinero.FINALIZADO); publicar("Fin cocinero " + id); }
    }
    public void finalizarCajero(int id) {
        synchronized (ingreso) { cajeros.put(id, EstadoCajero.FINALIZADO); publicar("Fin cajero " + id); }
    }
    public void cerrar() {
        synchronized (ingreso) {
            if (estado != EstadoSimulacion.ABIERTO) return;
            estado = EstadoSimulacion.CERRANDO;
            ingreso.cerrar();
            grupos.cerrar();
            cierre.countDown();
            publicar("Cierre: no se admiten clientes ni nuevos pedidos");
        }
    }
    /** Solo despues de que terminen todos los clientes. */
    public void detenerCocina() {
        synchronized (ingreso) { cocina.cerrarRecepcion(); ingreso.notifyAll(); }
    }
    public void detenerMozos() {
        for (int i = 0; i < parametros.mozos(); i++) tareas.add(TareaMozo.finalizar());
    }
    public void detenerCajeros() {
        synchronized (ingreso) {
            for (int i = 0; i < parametros.cajeros(); i++) cobros.add(SolicitudCobro.finalizar());
            ingreso.notifyAll();
        }
    }
    public void finalizar() {
        synchronized (ingreso) {
            if (ingreso.getCantidadClientesDentro() != 0
                    || mesas.stream().anyMatch(m -> m.getEstado() != EstadoMesa.LIBRE)) {
                throw new IllegalStateException("Quedan clientes o mesas sin limpiar.");
            }
            estado = EstadoSimulacion.FINALIZADO;
            publicar("Simulacion finalizada");
            eventos.add(EventoSimulacion.finalizar(++secuencia, Instant.now()));
        }
    }
    public EstadoRestaurante snapshot() {
        synchronized (ingreso) {
            Map<Integer, String> copiaMesas = new TreeMap<>();
            for (Mesa mesa : mesas) {
                copiaMesas.put(mesa.getIdMesa(), mesa + "; " + mesa.getPedido());
            }
            return new EstadoRestaurante(estado, clientes, mozos, cocineros, cajeros, copiaMesas);
        }
    }
    private void publicar(String descripcion) {
        eventos.add(EventoSimulacion.cambio(++secuencia, Instant.now(), descripcion, snapshot()));
        ingreso.notifyAll();
    }
}
