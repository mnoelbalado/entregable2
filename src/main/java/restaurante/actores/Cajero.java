package restaurante.actores;

import restaurante.coordinacion.Restaurante;
import restaurante.mensajes.SolicitudCobro;

public final class Cajero implements Runnable {
    private final int id;
    private final Restaurante restaurante;
    public Cajero(int id, Restaurante restaurante) {
        this.id = id;
        this.restaurante = restaurante;
    }
    @Override public void run() {
        try {
            while (true) {
                SolicitudCobro solicitud = restaurante.tomarCobro(id);
                if (solicitud.getTipo() == SolicitudCobro.Tipo.FINALIZAR) {
                    restaurante.finalizarCajero(id);
                    return;
                }
                restaurante.parametros().cobro().esperar();
                restaurante.terminarCobro(solicitud, id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            restaurante.fallar(e);
        }
    }
}
