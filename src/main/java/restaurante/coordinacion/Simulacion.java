package restaurante.coordinacion;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import restaurante.actores.*;
import restaurante.config.Configuracion.Parametros;
import restaurante.display.Display;

/** Cierre por etapas: clientes, cocina, mozos (incluida limpieza), cajas, display. */
public final class Simulacion {
    private final Restaurante restaurante;
    private final Path archivoLog;
    private boolean ejecutada;

    public Simulacion(Parametros parametros, Path archivoLog) {
        restaurante = new Restaurante(parametros);
        this.archivoLog = java.util.Objects.requireNonNull(archivoLog);
    }
    public Restaurante getRestaurante() { return restaurante; }

    public void ejecutar() throws InterruptedException {
        synchronized (this) {
            if (ejecutada) throw new IllegalStateException("La simulacion ya fue ejecutada.");
            ejecutada = true;
        }
        var p = restaurante.parametros();
        // Los clientes bloqueados no deben ocupar los hilos reservados a empleados.
        ExecutorService clientes = Executors.newCachedThreadPool(Thread.ofPlatform().name("cliente-", 1).factory());
        ExecutorService mozos = Executors.newFixedThreadPool(p.mozos(), Thread.ofPlatform().name("mozo-", 1).factory());
        ExecutorService cocineros = Executors.newFixedThreadPool(p.cocineros(), Thread.ofPlatform().name("cocinero-", 1).factory());
        ExecutorService cajeros = Executors.newFixedThreadPool(p.cajeros(), Thread.ofPlatform().name("cajero-", 1).factory());
        List<ExecutorService> pools = List.of(clientes, mozos, cocineros, cajeros);
        Display display = new Display(restaurante.eventos(), archivoLog);
        Thread pantalla = new Thread(protegido(() -> {
            display.run();
            if (display.getError() != null) restaurante.fallar(display.getError());
        }), "display");
        Thread generador = new Thread(protegido(new GeneradorClientes(restaurante, clientes)), "generador");
        boolean completo = false;
        try {
            pantalla.start();
            for (int i = 1; i <= p.mozos(); i++) mozos.execute(protegido(new Mozo(i, restaurante)));
            for (int i = 1; i <= p.cocineros(); i++) cocineros.execute(protegido(new Cocinero(i, restaurante)));
            for (int i = 1; i <= p.cajeros(); i++) cajeros.execute(protegido(new Cajero(i, restaurante)));
            generador.start();
            restaurante.esperarFallo(p.duracion());
            comprobarError();
            restaurante.cerrar();
            generador.interrupt();
            esperar(generador);
            clientes.shutdown();
            esperar(clientes);
            restaurante.detenerCocina();
            cocineros.shutdown();
            esperar(cocineros);
            restaurante.detenerMozos();
            mozos.shutdown();
            esperar(mozos);
            restaurante.detenerCajeros();
            cajeros.shutdown();
            esperar(cajeros);
            restaurante.finalizar();
            esperar(pantalla);
            comprobarError();
            completo = true;
        } finally {
            if (!completo) {
                restaurante.cerrar();
                generador.interrupt();
                pools.forEach(ExecutorService::shutdownNow);
                pantalla.interrupt();
                // Incluso si el coordinador fue interrumpido, intentar recoger todos los hilos.
                boolean interrumpido = Thread.interrupted();
                try {
                    generador.join(5000);
                    for (ExecutorService pool : pools) pool.awaitTermination(5, TimeUnit.SECONDS);
                    pantalla.join(5000);
                } catch (InterruptedException e) {
                    interrumpido = true;
                } finally {
                    if (interrumpido) Thread.currentThread().interrupt();
                }
            }
        }
    }
    private Runnable protegido(Runnable tarea) {
        return () -> {
            try { tarea.run(); }
            catch (Throwable e) { restaurante.fallar(e); }
        };
    }
    private void comprobarError() {
        if (restaurante.error() != null) {
            throw new IllegalStateException("La simulacion fallo.", restaurante.error());
        }
    }
    private void esperar(ExecutorService pool) throws InterruptedException {
        while (!pool.awaitTermination(50, TimeUnit.MILLISECONDS)) comprobarError();
        comprobarError();
    }
    private void esperar(Thread hilo) throws InterruptedException {
        while (hilo.isAlive()) { hilo.join(50); comprobarError(); }
        comprobarError();
    }
}
