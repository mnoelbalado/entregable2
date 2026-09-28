package restaurante.actores;

import restaurante.coordinacion.*;
import restaurante.util.Tiempos;

public final class Cocinero implements Runnable {
    private final int id;
    private final Restaurante restaurante;
    public Cocinero(int id, Restaurante restaurante) {
        this.id = id;
        this.restaurante = restaurante;
    }
    @Override public void run() {
        try {
            GestorCocina.Trabajo trabajo;
            while ((trabajo = restaurante.tomarTrabajo(id)) != null) {
                var menu = trabajo.getPlato().getMenu();
                Tiempos.esperarAleatorio(menu.getTiempoCoccionMin(), menu.getTiempoCoccionMax());
                restaurante.terminarPlato(trabajo, id);
            }
            restaurante.finalizarCocinero(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            restaurante.fallar(e);
        }
    }
}
