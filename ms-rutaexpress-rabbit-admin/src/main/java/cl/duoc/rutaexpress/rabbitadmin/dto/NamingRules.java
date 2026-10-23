package cl.duoc.rutaexpress.rabbitadmin.dto;

/**
 * Reglas de validacion para nombres de recursos RabbitMQ.
 */
public final class NamingRules {

    /**
     * Letras, numeros, punto, guion y guion bajo (1 a 200 caracteres). No puede
     * empezar con "amq.", prefijo reservado por RabbitMQ.
     */
    public static final String RESOURCE_NAME = "^(?!amq\\.)[A-Za-z0-9._-]{1,200}$";
    public static final String RESOURCE_NAME_MESSAGE =
            "solo puede contener letras, numeros, '.', '-' y '_' (max. 200) y no puede empezar con 'amq.'";

    /** Routing key: palabras separadas por punto; admite los comodines * y # (puede ser vacia). */
    public static final String ROUTING_KEY = "^[A-Za-z0-9._*#-]{0,255}$";
    public static final String ROUTING_KEY_MESSAGE =
            "solo puede contener letras, numeros, '.', '-', '_', '*' y '#' (max. 255)";

    private NamingRules() {
    }

}
