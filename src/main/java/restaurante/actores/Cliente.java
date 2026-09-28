package restaurante.actores;

import java.util.concurrent.ThreadLocalRandom;
import restaurante.coordinacion.*;
import restaurante.modelo.Menu;
import restaurante.modelo.Estados.EstadoCliente;

public final class Cliente implements Runnable {
    private final int id;
    private final Restaurante restaurante;

    public Cliente(int id, Restaurante restaurante) {
        this.id = id;
        this.restaurante = restaurante;
    }

    @Override public void run() {
        try {
            GestorMesas.Sesion sesion = restaurante.ingresar(id);
            if (sesion == null) return;
            var parametros = restaurante.parametros();
            Menu menu = parametros.carta().get(ThreadLocalRandom.current().nextInt(parametros.carta().size()));
            if (restaurante.esperarSeleccion(parametros.seleccion().sortear())) {
                restaurante.seleccionar(sesion, id, menu);
            }
            if (sesion.esperarDecision()) {
                sesion.esperarComida();
                restaurante.cambiarCliente(id, EstadoCliente.COMIENDO);
                parametros.comida().esperar();
                restaurante.solicitarCobro(id, menu).esperarCobro();
            }
            restaurante.salir(sesion, id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            restaurante.fallar(e);
        }
    }
}
