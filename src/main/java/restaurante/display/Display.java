package restaurante.display;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import restaurante.mensajes.EventoSimulacion;

public final class Display implements Runnable {

    private final BlockingQueue<EventoSimulacion> colaEventos;
    private final Path archivoLog;

    private volatile Throwable error;

    public Display(
            BlockingQueue<EventoSimulacion> colaEventos,
            Path archivoLog) {

        this.colaEventos = Objects.requireNonNull(
                colaEventos,
                "La cola de eventos no puede ser null.");

        this.archivoLog = Objects.requireNonNull(
                archivoLog,
                "La ruta del log no puede ser null.");
    }

    @Override
    public void run() {
        try {
            Path directorio = archivoLog.toAbsolutePath().getParent();
            Files.createDirectories(directorio);

            try (BufferedWriter escritor = Files.newBufferedWriter(
                    archivoLog,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {

                procesarEventos(escritor);
            }

        } catch (InterruptedException e) {
            error = e;
            Thread.currentThread().interrupt();

            System.err.println(
                    "El display fue interrumpido antes de completar el log.");

        } catch (IOException e) {
            error = e;

            throw new UncheckedIOException(
                    "No se pudo escribir el log de la simulacion.", e);
        }
    }

    private void procesarEventos(BufferedWriter escritor)
            throws InterruptedException, IOException {

        while (true) {
            EventoSimulacion evento = colaEventos.take();

            String texto = evento.toString();

            System.out.println(texto);
            System.out.println();

            escritor.write(texto);
            escritor.newLine();
            escritor.newLine();
            escritor.flush();

            if (evento.getTipo() == EventoSimulacion.Tipo.FINALIZAR) {
                return;
            }
        }
    }

    /** Permite al coordinador detectar una finalización con errores. */
    public Throwable getError() {
        return error;
    }
}