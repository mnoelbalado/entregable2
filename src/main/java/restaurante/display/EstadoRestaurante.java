package restaurante.display;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import restaurante.modelo.Estados.EstadoCajero;
import restaurante.modelo.Estados.EstadoCliente;
import restaurante.modelo.Estados.EstadoCocinero;
import restaurante.modelo.Estados.EstadoMozo;
import restaurante.modelo.Estados.EstadoSimulacion;

public final class EstadoRestaurante {

    private final EstadoSimulacion estadoSimulacion;

    private final Map<Integer, EstadoCliente> clientes;
    private final Map<Integer, EstadoMozo> mozos;
    private final Map<Integer, EstadoCocinero> cocineros;
    private final Map<Integer, EstadoCajero> cajeros;

    private final Map<Integer, String> mesas;

    public EstadoRestaurante(
            EstadoSimulacion estadoSimulacion,
            Map<Integer, EstadoCliente> clientes,
            Map<Integer, EstadoMozo> mozos,
            Map<Integer, EstadoCocinero> cocineros,
            Map<Integer, EstadoCajero> cajeros,
            Map<Integer, String> mesas) {

        this.estadoSimulacion = Objects.requireNonNull(
                estadoSimulacion,
                "El estado de la simulacion no puede ser null.");

        this.clientes = copiarMapa(clientes);
        this.mozos = copiarMapa(mozos);
        this.cocineros = copiarMapa(cocineros);
        this.cajeros = copiarMapa(cajeros);
        this.mesas = copiarMapa(mesas);
    }

    public EstadoSimulacion getEstadoSimulacion() {
        return estadoSimulacion;
    }

    public Map<Integer, EstadoCliente> getClientes() {
        return clientes;
    }

    public Map<Integer, EstadoMozo> getMozos() {
        return mozos;
    }

    public Map<Integer, EstadoCocinero> getCocineros() {
        return cocineros;
    }

    public Map<Integer, EstadoCajero> getCajeros() {
        return cajeros;
    }

    public Map<Integer, String> getMesas() {
        return mesas;
    }

    private static <V> Map<Integer, V> copiarMapa(
            Map<Integer, V> original) {

        Objects.requireNonNull(
                original,
                "Los mapas de estado no pueden ser null.");

        // Copia independiente y rechazo de claves o valores null.
        Map<Integer, V> copia = Map.copyOf(original);

        // Orden por identificador y prohibición de modificaciones.
        return Collections.unmodifiableMap(new TreeMap<>(copia));
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder();

        texto.append("Restaurante: ")
                .append(estadoSimulacion)
                .append('\n');

        agregarSeccion(texto, "Clientes", clientes);
        agregarSeccion(texto, "Mozos", mozos);
        agregarSeccion(texto, "Cocineros", cocineros);
        agregarSeccion(texto, "Cajeros", cajeros);
        agregarSeccion(texto, "Mesas", mesas);

        return texto.toString();
    }

    private static <V> void agregarSeccion(
            StringBuilder texto,
            String titulo,
            Map<Integer, V> estados) {

        texto.append(titulo).append(":\n");

        if (estados.isEmpty()) {
            texto.append("  Sin registros\n");
            return;
        }

        estados.forEach((id, estado) ->
                texto.append("  ")
                        .append(id)
                        .append(": ")
                        .append(estado)
                        .append('\n'));
    }
}