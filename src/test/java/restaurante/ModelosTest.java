package restaurante;

import org.junit.jupiter.api.Test;
import restaurante.config.Configuracion;
import restaurante.coordinacion.*;
import restaurante.display.EstadoRestaurante;
import restaurante.mensajes.*;
import restaurante.modelo.*;
import restaurante.modelo.Estados.*;
import restaurante.util.Tiempos;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelosTest {
    @Test void rechazaParametrosYTransicionesInvalidas() {
        assertDoesNotThrow(Configuracion::validar);
        assertThrows(IllegalArgumentException.class, () -> new Menu(" ", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Menu("Menu", 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new Configuracion.Rango(-1, 0));
        Menu menu = new Menu("Menu", 0, 1);
        Plato plato = new Plato(1, menu);
        assertThrows(IllegalStateException.class, plato::marcarListo);
        assertThrows(IllegalArgumentException.class, () -> new Pedido(1, 1, List.of(plato, plato)));
        Mesa mesa = new Mesa(1, 2);
        assertThrows(IllegalArgumentException.class, () -> mesa.ocupar(List.of(1)));
        assertThrows(IllegalArgumentException.class, () -> mesa.ocupar(List.of(1, 1)));
        mesa.ocupar(List.of(1, 2));
        assertThrows(IllegalStateException.class, mesa::iniciarLimpieza);
        assertThrows(IllegalArgumentException.class, () -> mesa.registrarSalida(3));
        assertThrows(IllegalArgumentException.class, () -> mesa.registrarPedido(
                new Pedido(1, 1, List.of(plato, new Plato(3, menu)))));
        SolicitudCobro cobro = SolicitudCobro.cobrar(1, menu);
        cobro.marcarCobrado();
        assertThrows(IllegalStateException.class, cobro::marcarCobrado);
    }

    @Test void tiemposIncluyenExtremosYSonSegurosConLongMaximo() {
        assertEquals(0, Tiempos.aleatorio(0, 0));
        assertEquals(Long.MAX_VALUE, Tiempos.aleatorio(Long.MAX_VALUE, Long.MAX_VALUE));
        for (int i = 0; i < 1000; i++) {
            assertTrue(Tiempos.aleatorio(0, Long.MAX_VALUE) >= 0);
            long tiempo = Tiempos.aleatorio(Long.MAX_VALUE - 1, Long.MAX_VALUE);
            assertTrue(tiempo >= Long.MAX_VALUE - 1);
        }
        assertThrows(IllegalArgumentException.class, () -> Tiempos.aleatorio(2, 1));
    }

    @Test void snapshotNoCambiaConLosMapasOriginales() {
        Map<Integer, EstadoCliente> clientes = new HashMap<>();
        clientes.put(1, EstadoCliente.COMIENDO);
        EstadoRestaurante copia = new EstadoRestaurante(EstadoSimulacion.ABIERTO,
                clientes, Map.of(), Map.of(), Map.of(), Map.of(1, "OCUPADA"));
        clientes.put(1, EstadoCliente.RETIRADO);
        assertEquals(EstadoCliente.COMIENDO, copia.getClientes().get(1));
        assertThrows(UnsupportedOperationException.class, () -> copia.getClientes().clear());
    }

    @Test void todosEligenAntesDeUnaUnicaLlamadaYLaSesionSeRenueva() throws Exception {
        GestorMesas gestor = new GestorMesas();
        Mesa mesa = new Mesa(1, 2);
        Menu menu = new Menu("Menu", 0, 0);
        mesa.ocupar(List.of(1, 2));
        var sesion = gestor.sesion(mesa);
        assertFalse(gestor.seleccionar(sesion, 1, menu));
        assertThrows(IllegalStateException.class, () -> gestor.aceptar(sesion, 1));
        assertTrue(gestor.seleccionar(sesion, 2, menu));
        assertThrows(IllegalArgumentException.class, () -> gestor.seleccionar(sesion, 2, menu));
        assertNotNull(gestor.aceptar(sesion, 1));
        assertTrue(sesion.esperarDecision());
        mesa.registrarSalida(1);
        mesa.registrarSalida(2);
        mesa.iniciarLimpieza();
        mesa.finalizarLimpieza();
        gestor.liberar(mesa);
        mesa.ocupar(List.of(3, 4));
        assertNotSame(sesion, gestor.sesion(mesa));
    }
}
