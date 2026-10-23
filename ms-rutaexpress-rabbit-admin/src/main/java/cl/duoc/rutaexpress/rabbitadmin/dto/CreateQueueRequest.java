package cl.duoc.rutaexpress.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Datos para crear una cola. Opcionalmente se le asigna una DLX (dead letter
 * exchange) y un TTL de mensajes.
 */
public record CreateQueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = NamingRules.RESOURCE_NAME, message = "El nombre de la cola " + NamingRules.RESOURCE_NAME_MESSAGE)
        String name,

        Boolean durable,

        @Pattern(regexp = NamingRules.RESOURCE_NAME, message = "El dead letter exchange " + NamingRules.RESOURCE_NAME_MESSAGE)
        String deadLetterExchange,

        @Pattern(regexp = NamingRules.ROUTING_KEY, message = "La dead letter routing key " + NamingRules.ROUTING_KEY_MESSAGE)
        String deadLetterRoutingKey,

        @Positive(message = "El TTL de mensajes debe ser mayor a 0 ms")
        @Max(value = 604_800_000, message = "El TTL de mensajes no puede superar 7 dias (604800000 ms)")
        Integer messageTtlMs
) {

    /** Por defecto las colas son durables (sobreviven al reinicio del broker). */
    public boolean durableOrDefault() {
        return durable == null || durable;
    }

    @AssertTrue(message = "Para usar deadLetterRoutingKey tambien debe indicar deadLetterExchange")
    public boolean isDeadLetterConfigValid() {
        return deadLetterRoutingKey == null || deadLetterRoutingKey.isEmpty() || deadLetterExchange != null;
    }

}
