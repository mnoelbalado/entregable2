package restaurante;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import restaurante.config.Configuracion.*;
import restaurante.coordinacion.*;
import restaurante.modelo.*;
import restaurante.modelo.Estados.*;
import restaurante.mensajes.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class CierreTest {
    @TempDir Path temporal;

    private Parametros parametros(long duracion, int personas, long seleccion, long pedido) {
        return new Parametros(duracion, 2, personas, 3, 3, 2,
                new Rango(1, 2), new Rango(seleccion, seleccion),
                new Rango(1, 3), new Rango(pedido, pedido), new Rango(1, 2),
                new Rango(1, 2), new Rango(1, 3), List.of(new Menu("Rapido", 1, 2), new Menu("Lento", 2, 4)));
    }

    @RepeatedTest(5) void simulacionCompletaDrenaLogYLimpiaMesas() throws Exception {
        Path log = temporal.resolve("simulacion.log");
        Simulacion simulacion = new Simulacion(parametros(150, 2, 1, 1), log);
        simulacion.ejecutar();
        var estado = simulacion.getRestaurante().snapshot();
        assertEquals(EstadoSimulacion.FINALIZADO, estado.getEstadoSimulacion());
        assertFalse(estado.getClientes().isEmpty());
        assertTrue(estado.getClientes().values().stream().allMatch(e -> e == EstadoCliente.RETIRADO));
        assertTrue(estado.getMozos().values().stream().allMatch(e -> e == EstadoMozo.FINALIZADO));
        assertTrue(estado.getCocineros().values().stream().allMatch(e -> e == EstadoCocinero.FINALIZADO));
        assertTrue(estado.getCajeros().values().stream().allMatch(e -> e == EstadoCajero.FINALIZADO));
        assertTrue(estado.getMesas().values().stream().allMatch(e -> e.contains("estado=LIBRE")));
        List<String> eventos = Files.readAllLines(log).stream().filter(s -> s.startsWith("[")).toList();
        for (int i = 0; i < eventos.size(); i++) assertTrue(eventos.get(i).startsWith("[" + (i + 1) + "] "));
        assertTrue(eventos.getLast().endsWith("Fin de las notificaciones."));
        assertTrue(eventos.stream().anyMatch(e -> e.contains("aceptado:")));
        assertTrue(eventos.stream().anyMatch(e -> e.contains("Cobro completado")));
        int cierre = indice(eventos, "Cierre:");
        assertTrue(cierre >= 0);
        assertTrue(eventos.subList(cierre, eventos.size()).stream().noneMatch(e -> e.contains("aceptado:")));
        assertSinHilos();
    }

    @Test void cierreDuranteSeleccionRetiraSinPedidoNiCobro() throws Exception {
        Path log = temporal.resolve("seleccion.log");
        new Simulacion(parametros(70, 2, 5000, 1), log).ejecutar();
        String texto = Files.readString(log);
        assertTrue(texto.contains("SELECCIONANDO_MENU"));
        assertFalse(texto.contains("aceptado:"));
        assertFalse(texto.contains("Cobro completado"));
        assertSinHilos();
    }

    @Test void cierreMientrasMozoAnotaCancelaPedido() throws Exception {
        Restaurante r = new Restaurante(parametros(100, 1, 0, 0));
        var sesion = r.ingresar(1);
        r.seleccionar(sesion, 1, r.parametros().carta().getFirst());
        TareaMozo tarea = r.tomarTarea();
        assertTrue(r.iniciarPedido(tarea.getMesa(), 1));
        r.cerrar();
        r.aceptarPedido(tarea.getMesa());
        assertFalse(sesion.esperarDecision());
        assertNull(sesion.getPedido());
        r.salir(sesion, 1);
        assertEquals(TareaMozo.Tipo.LIMPIAR_MESA, r.tomarTarea().getTipo());
    }

    @Test void pedidoAceptadoAntesDelCierreCompletaServicioYCobro() throws Exception {
        Restaurante r = new Restaurante(parametros(100, 1, 0, 0));
        var sesion = r.ingresar(1);
        Menu menu = r.parametros().carta().getFirst();
        r.seleccionar(sesion, 1, menu);
        var tarea = r.tomarTarea();
        assertTrue(r.iniciarPedido(tarea.getMesa(), 1));
        r.aceptarPedido(tarea.getMesa());
        r.cerrar();
        assertTrue(sesion.esperarDecision());
        var trabajo = r.tomarTrabajo(1);
        r.terminarPlato(trabajo, 1);
        var entrega = r.tomarTarea();
        assertEquals(TareaMozo.Tipo.SERVIR_PEDIDO, entrega.getTipo());
        ExecutorService cliente = Executors.newSingleThreadExecutor();
        try {
            Future<?> espera = cliente.submit(() -> { sesion.esperarComida(); return null; });
            assertThrows(TimeoutException.class, () -> espera.get(30, TimeUnit.MILLISECONDS));
            r.iniciarEntrega(entrega, 1);
            r.servir(sesion.getMesa());
            espera.get(1, TimeUnit.SECONDS);
            assertEquals(EstadoPedido.ENTREGADO, sesion.getPedido().getEstado());
            var cobro = r.solicitarCobro(1, menu);
            Future<?> pago = cliente.submit(() -> { cobro.esperarCobro(); return null; });
            assertThrows(TimeoutException.class, () -> pago.get(30, TimeUnit.MILLISECONDS));
            assertSame(cobro, r.tomarCobro(1));
            r.terminarCobro(cobro, 1);
            pago.get(1, TimeUnit.SECONDS);
            r.salir(sesion, 1);
            var limpieza = r.tomarTarea();
            r.iniciarLimpieza(limpieza.getMesa(), 1);
            r.terminarLimpieza(limpieza.getMesa());
            r.finalizar();
            assertEquals(EstadoSimulacion.FINALIZADO, r.snapshot().getEstadoSimulacion());
        } finally { cliente.shutdownNow(); }
    }

    @Test void colaDeCajaEsUnicaYFifo() throws Exception {
        Restaurante r = new Restaurante(parametros(100, 1, 0, 0));
        Menu menu = r.parametros().carta().getFirst();
        var primero = r.solicitarCobro(1, menu);
        var segundo = r.solicitarCobro(2, menu);
        var tercero = r.solicitarCobro(3, menu);
        assertSame(primero, r.tomarCobro(2));
        assertSame(segundo, r.tomarCobro(1));
        assertSame(tercero, r.tomarCobro(2));
    }

    @Test void errorDeLogDetieneTodosLosHilos() {
        // Escribir sobre un directorio debe fallar, nunca dejar clientes bloqueados.
        Simulacion simulacion = new Simulacion(parametros(1000, 2, 1, 1), temporal);
        assertThrows(IllegalStateException.class, simulacion::ejecutar);
        assertNotNull(simulacion.getRestaurante().error());
        assertSinHilos();
    }

    private static int indice(List<String> eventos, String texto) {
        for (int i = 0; i < eventos.size(); i++) if (eventos.get(i).contains(texto)) return i;
        return -1;
    }
    private static void assertSinHilos() {
        List<String> vivos = Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive).map(Thread::getName)
                .filter(n -> n.matches("(cliente|mozo|cocinero|cajero)-\\d+|display|generador")).toList();
        assertTrue(vivos.isEmpty(), "Hilos vivos: " + vivos);
    }
}
