package restaurante.actores;

import restaurante.coordinacion.Restaurante;
import restaurante.mensajes.TareaMozo;
import restaurante.modelo.Estados.EstadoMozo;

public final class Mozo implements Runnable {
    private final int id;
    private final Restaurante restaurante;

    public Mozo(int id, Restaurante restaurante) {
        this.id = id;
        this.restaurante = restaurante;
    }

    @Override public void run() {
        try {
            var parametros = restaurante.parametros();
            while (true) {
                TareaMozo tarea = restaurante.tomarTarea();
                switch (tarea.getTipo()) {
                    case FINALIZAR -> {
                        restaurante.cambiarMozo(id, EstadoMozo.FINALIZADO);
                        return;
                    }
                    case TOMAR_PEDIDO -> {
                        if (restaurante.iniciarPedido(tarea.getMesa(), id)) {
                            parametros.pedido().esperar();
                            restaurante.aceptarPedido(tarea.getMesa());
                        }
                    }
                    case SERVIR_PEDIDO -> {
                        restaurante.iniciarEntrega(tarea, id);
                        parametros.entrega().esperar();
                        restaurante.servir(tarea.getMesa());
                    }
                    case LIMPIAR_MESA -> {
                        restaurante.iniciarLimpieza(tarea.getMesa(), id);
                        parametros.limpieza().esperar();
                        restaurante.terminarLimpieza(tarea.getMesa());
                    }
                }
                restaurante.cambiarMozo(id, EstadoMozo.ESPERANDO_TAREA);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            restaurante.fallar(e);
        }
    }
}
