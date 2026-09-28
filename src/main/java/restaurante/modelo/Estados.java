package restaurante.modelo;

public final class Estados {

    private Estados() {
    }

    public enum EstadoSimulacion {
        ABIERTO,
        CERRANDO,
        FINALIZADO
    }

    public enum EstadoCliente {
        ESPERANDO_AFUERA,
        ESPERANDO_GRUPO,
        ESPERANDO_MESA,
        SELECCIONANDO_MENU,
        ESPERANDO_MOZO,
        REALIZANDO_PEDIDO,
        ESPERANDO_COMIDA,
        COMIENDO,
        ESPERANDO_CAJA,
        PAGANDO,
        RETIRADO
    }

    public enum EstadoMozo {
        ESPERANDO_TAREA,
        TOMANDO_PEDIDO,
        SIRVIENDO_PEDIDO,
        LIMPIANDO_MESA,
        FINALIZADO
    }

    public enum EstadoCocinero {
        ESPERANDO_TRABAJO,
        COCINANDO,
        FINALIZADO
    }

    public enum EstadoCajero {
        ESPERANDO_CLIENTE,
        COBRANDO,
        FINALIZADO
    }

    public enum EstadoMesa {
        LIBRE,
        OCUPADA,
        PENDIENTE_LIMPIEZA,
        EN_LIMPIEZA
    }

    public enum EstadoPedido {
        REGISTRADO,
        EN_PREPARACION,
        LISTO,
        EN_ENTREGA,
        ENTREGADO
    }

    public enum EstadoPlato {
        PENDIENTE,
        EN_PREPARACION,
        LISTO,
        SERVIDO
    }
}
