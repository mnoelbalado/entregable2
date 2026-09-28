package restaurante.coordinacion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import restaurante.modelo.Estados.EstadoCliente;

import restaurante.modelo.Estados.EstadoMesa;
import restaurante.modelo.Mesa;

/** Controla el aforo y asigna mesas a grupos completos de clientes. */
public final class GestorIngreso {

    private final List<Mesa> mesas;
    private final int personasPorMesa;
    private final int capacidad;

    private final Set<Integer> idsRegistrados = new HashSet<>();
    private final Set<Integer> clientesDentro = new HashSet<>();
    private final ArrayDeque<Integer> esperandoGrupo = new ArrayDeque<>();
    private final Map<Integer, Mesa> mesasAsignadas = new HashMap<>();

    private boolean abierto = true;
    private final BiConsumer<Integer, EstadoCliente> notificar;

    public GestorIngreso(List<Mesa> mesas, int personasPorMesa) {
        this(mesas, personasPorMesa, (id, estado) -> { });
    }

    /** El observador se ejecuta bajo este monitor; no debe bloquear. */
    public GestorIngreso(List<Mesa> mesas, int personasPorMesa,
            BiConsumer<Integer, EstadoCliente> notificar) {
        this.notificar = Objects.requireNonNull(notificar);
        Objects.requireNonNull(mesas, "La lista de mesas no puede ser null.");
        if (mesas.isEmpty() || personasPorMesa <= 0) {
            throw new IllegalArgumentException("Se requieren mesas y personas por mesa.");
        }

        Set<Integer> idsMesas = new HashSet<>();
        for (Mesa mesa : mesas) {
            if (mesa == null || mesa.getCapacidad() != personasPorMesa
                    || mesa.getEstado() != EstadoMesa.LIBRE
                    || !idsMesas.add(mesa.getIdMesa())) {
                throw new IllegalArgumentException(
                        "Las mesas deben ser libres, distintas y de capacidad P.");
            }
        }

        this.mesas = List.copyOf(mesas);
        this.personasPorMesa = personasPorMesa;
        this.capacidad = Math.multiplyExact(mesas.size(), personasPorMesa);
    }

    /**
     * Espera lugar y un grupo completo. Devuelve null si el local cierra
     * antes de que el cliente obtenga una mesa.
     */
    public synchronized Mesa ingresarYEsperarMesa(int idCliente)
            throws InterruptedException {
        if (idCliente <= 0 || !idsRegistrados.add(idCliente)) {
            throw new IllegalArgumentException("El identificador debe ser positivo y unico.");
        }

        try {
            notificar.accept(idCliente, EstadoCliente.ESPERANDO_AFUERA);
            while (abierto && clientesDentro.size() == capacidad) {
                wait();
            }
            if (!abierto) {
                notificar.accept(idCliente, EstadoCliente.RETIRADO);
                return null;
            }

            clientesDentro.add(idCliente);
            esperandoGrupo.addLast(idCliente);
            notificar.accept(idCliente, EstadoCliente.ESPERANDO_GRUPO);
            formarGrupos();

            while (true) {
                Mesa asignada = mesasAsignadas.remove(idCliente);
                if (asignada != null) {
                    return asignada;
                }
                if (!abierto) {
                    retirarDeEspera(idCliente);
                    return null;
                }
                wait();
            }
        } catch (InterruptedException e) {
            // Un grupo ya sentado debe poder salir mediante Mesa.registrarSalida().
            Mesa asignada = mesasAsignadas.remove(idCliente);
            if (asignada != null) {
                Thread.currentThread().interrupt();
                return asignada;
            }
            retirarDeEspera(idCliente);
            throw e;
        }
    }

    /** Registra una salida; true indica que salió el último de la mesa. */
    public synchronized boolean registrarSalida(Mesa mesa, int idCliente) {
        if (mesa == null || !mesas.contains(mesa)
                || !clientesDentro.contains(idCliente)) {
            throw new IllegalArgumentException("Cliente o mesa no registrados.");
        }

        boolean ultimo = mesa.registrarSalida(idCliente);
        clientesDentro.remove(idCliente);
        notificar.accept(idCliente, EstadoCliente.RETIRADO);
        notifyAll(); // Puede haber clientes esperando afuera por aforo.
        return ultimo;
    }

    /** Llamar después de que el mozo ejecute mesa.finalizarLimpieza(). */
    public synchronized void avisarMesaLibre(Mesa mesa) {
        if (mesa == null || !mesas.contains(mesa)
                || mesa.getEstado() != EstadoMesa.LIBRE) {
            throw new IllegalArgumentException("La mesa no esta libre o no pertenece al local.");
        }
        formarGrupos();
        notifyAll();
    }

    /** Cierra el ingreso y despierta a quienes esperan afuera o sin grupo. */
    public synchronized void cerrar() {
        abierto = false;
        notifyAll();
    }

    public synchronized boolean estaAbierto() {
        return abierto;
    }

    public synchronized int getCantidadClientesDentro() {
        return clientesDentro.size();
    }

    private void retirarDeEspera(int idCliente) {
        esperandoGrupo.remove(idCliente);
        if (clientesDentro.remove(idCliente)) {
            notifyAll();
        }
        notificar.accept(idCliente, EstadoCliente.RETIRADO);
    }

    // Todos los métodos que lo invocan ya poseen el monitor del gestor.
    private void formarGrupos() {
        if (!abierto) {
            return;
        }

        while (esperandoGrupo.size() >= personasPorMesa) {
            Mesa libre = null;
            for (Mesa mesa : mesas) {
                if (mesa.getEstado() == EstadoMesa.LIBRE) {
                    libre = mesa;
                    break;
                }
            }
            if (libre == null) {
                for (int id : esperandoGrupo) {
                    notificar.accept(id, EstadoCliente.ESPERANDO_MESA);
                }
                return;
            }

            List<Integer> grupo = new ArrayList<>(personasPorMesa);
            for (int i = 0; i < personasPorMesa; i++) {
                grupo.add(esperandoGrupo.removeFirst());
            }
            libre.ocupar(grupo);
            for (int id : grupo) {
                mesasAsignadas.put(id, libre);
                notificar.accept(id, EstadoCliente.SELECCIONANDO_MENU);
            }
            notifyAll();
        }
    }
}
