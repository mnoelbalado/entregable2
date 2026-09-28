package restaurante;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import restaurante.coordinacion.GestorIngreso;
import restaurante.modelo.Mesa;
import restaurante.modelo.Estados.EstadoMesa;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class IngresoTest {
    @Test void aforoGrupoCompletoYLimpiezaAntesDeReutilizar() throws Exception {
        Mesa mesa = new Mesa(1, 2);
        GestorIngreso ingreso = new GestorIngreso(List.of(mesa), 2);
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            Future<Mesa> primero = pool.submit(() -> ingreso.ingresarYEsperarMesa(1));
            esperarDentro(ingreso, 1);
            assertFalse(primero.isDone(), "No se sienta un grupo incompleto");
            Future<Mesa> segundo = pool.submit(() -> ingreso.ingresarYEsperarMesa(2));
            assertSame(mesa, primero.get(1, TimeUnit.SECONDS));
            assertSame(mesa, segundo.get(1, TimeUnit.SECONDS));
            CountDownLatch terceroLlego = new CountDownLatch(1);
            Future<Mesa> tercero = pool.submit(() -> {
                terceroLlego.countDown();
                return ingreso.ingresarYEsperarMesa(3);
            });
            assertTrue(terceroLlego.await(1, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> tercero.get(30, TimeUnit.MILLISECONDS));
            assertEquals(2, ingreso.getCantidadClientesDentro());
            assertFalse(ingreso.registrarSalida(mesa, 1));
            esperarDentro(ingreso, 2);
            assertTrue(ingreso.registrarSalida(mesa, 2));
            Future<Mesa> cuarto = pool.submit(() -> ingreso.ingresarYEsperarMesa(4));
            esperarDentro(ingreso, 2);
            assertEquals(EstadoMesa.PENDIENTE_LIMPIEZA, mesa.getEstado());
            assertFalse(tercero.isDone());
            mesa.iniciarLimpieza();
            assertFalse(cuarto.isDone());
            mesa.finalizarLimpieza();
            ingreso.avisarMesaLibre(mesa);
            assertSame(mesa, tercero.get(1, TimeUnit.SECONDS));
            assertSame(mesa, cuarto.get(1, TimeUnit.SECONDS));
            assertEquals(List.of(3, 4), mesa.getClientes());
        } finally {
            ingreso.cerrar();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test void cierreDespiertaAlGrupoIncompletoYLiberaAforo() throws Exception {
        GestorIngreso ingreso = new GestorIngreso(List.of(new Mesa(1, 2)), 2);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Mesa> cliente = pool.submit(() -> ingreso.ingresarYEsperarMesa(1));
            esperarDentro(ingreso, 1);
            ingreso.cerrar();
            assertNull(cliente.get(1, TimeUnit.SECONDS));
            assertEquals(0, ingreso.getCantidadClientesDentro());
            assertNull(ingreso.ingresarYEsperarMesa(2));
        } finally { pool.shutdownNow(); }
    }

    @Test void interrumpirEsperaNoPierdeCapacidad() throws Exception {
        GestorIngreso ingreso = new GestorIngreso(List.of(new Mesa(1, 2)), 2);
        Thread cliente = new Thread(() -> {
            try { ingreso.ingresarYEsperarMesa(1); fail("Debia interrumpirse"); }
            catch (InterruptedException esperado) { Thread.currentThread().interrupt(); }
        });
        cliente.start();
        try {
            esperarDentro(ingreso, 1);
            cliente.interrupt();
            cliente.join(1000);
            assertFalse(cliente.isAlive());
            assertEquals(0, ingreso.getCantidadClientesDentro());
        } finally { ingreso.cerrar(); cliente.interrupt(); cliente.join(1000); }
    }

    private static void esperarDentro(GestorIngreso ingreso, int cantidad) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (ingreso.getCantidadClientesDentro() != cantidad && System.nanoTime() < limite) Thread.sleep(1);
        assertEquals(cantidad, ingreso.getCantidadClientesDentro());
    }
}
