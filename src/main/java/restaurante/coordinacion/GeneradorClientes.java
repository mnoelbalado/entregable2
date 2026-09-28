package restaurante.coordinacion;

import java.util.concurrent.ExecutorService;
import restaurante.actores.Cliente;

/** Llega un cliente por intervalo TP; cada cliente tiene su propia tarea. */
public final class GeneradorClientes implements Runnable {
    private final Restaurante restaurante;
    private final ExecutorService clientes;

    public GeneradorClientes(Restaurante restaurante, ExecutorService clientes) {
        this.restaurante = restaurante;
        this.clientes = clientes;
    }

    @Override public void run() {
        try {
            int id = 0;
            while (restaurante.estaAbierto()) {
                restaurante.parametros().llegada().esperar();
                if (!restaurante.estaAbierto()) return;
                id = Math.incrementExact(id);
                Cliente cliente = new Cliente(id, restaurante);
                clientes.execute(() -> {
                    try { cliente.run(); }
                    catch (Throwable e) { restaurante.fallar(e); }
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (restaurante.estaAbierto()) restaurante.fallar(e);
        }
    }
}
