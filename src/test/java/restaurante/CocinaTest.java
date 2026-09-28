package restaurante;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import restaurante.coordinacion.GestorCocina;
import restaurante.modelo.*;
import restaurante.modelo.Estados.EstadoPedido;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class CocinaTest {
    private Pedido pedido(int id, int desde, int cantidad) {
        List<Plato> platos = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) platos.add(new Plato(desde + i, new Menu("Menu", 0, 0)));
        return new Pedido(id, id, platos);
    }

    @Test void asignaTodosLosPlatosDelPrimerPedidoAntesDelSiguiente() throws Exception {
        GestorCocina cocina = new GestorCocina();
        Pedido primero = pedido(1, 1, 2), segundo = pedido(2, 3, 1);
        cocina.recibirPedido(primero);
        cocina.recibirPedido(segundo);
        var uno = cocina.tomarTrabajo();
        var dos = cocina.tomarTrabajo();
        var tres = cocina.tomarTrabajo();
        assertSame(primero, uno.getPedido());
        assertSame(primero, dos.getPedido());
        assertSame(segundo, tres.getPedido());
        assertFalse(cocina.finalizarTrabajo(dos));
        assertThrows(IllegalStateException.class, primero::iniciarEntrega);
        assertTrue(cocina.finalizarTrabajo(uno));
        assertEquals(EstadoPedido.LISTO, primero.getEstado());
        assertThrows(IllegalArgumentException.class, () -> cocina.finalizarTrabajo(uno));
        assertTrue(cocina.finalizarTrabajo(tres));
        cocina.cerrarRecepcion();
        assertNull(cocina.tomarTrabajo());
    }

    @Test void variosCocinerosNoDuplicanPlatosNiAvisos() throws Exception {
        GestorCocina cocina = new GestorCocina();
        Set<Plato> preparados = ConcurrentHashMap.newKeySet();
        AtomicInteger avisos = new AtomicInteger();
        for (int i = 1; i <= 20; i++) cocina.recibirPedido(pedido(i, i * 10, 4));
        cocina.cerrarRecepcion();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futuros = new ArrayList<>();
            for (int i = 0; i < 8; i++) futuros.add(pool.submit(() -> {
                GestorCocina.Trabajo trabajo;
                while ((trabajo = cocina.tomarTrabajo()) != null) {
                    assertTrue(preparados.add(trabajo.getPlato()));
                    if (cocina.finalizarTrabajo(trabajo)) avisos.incrementAndGet();
                }
                return null;
            }));
            for (Future<?> futuro : futuros) futuro.get(2, TimeUnit.SECONDS);
            assertEquals(80, preparados.size());
            assertEquals(20, avisos.get());
            assertEquals(0, cocina.getCantidadTrabajosActivos());
        } finally { pool.shutdownNow(); }
    }

    @Test void cocineroEsperandoDespiertaAlCerrar() throws Exception {
        GestorCocina cocina = new GestorCocina();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<GestorCocina.Trabajo> futuro = pool.submit(cocina::tomarTrabajo);
            assertThrows(TimeoutException.class, () -> futuro.get(30, TimeUnit.MILLISECONDS));
            cocina.cerrarRecepcion();
            assertNull(futuro.get(1, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
}
