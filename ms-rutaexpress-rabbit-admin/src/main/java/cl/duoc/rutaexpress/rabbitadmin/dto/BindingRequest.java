package cl.duoc.rutaexpress.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Union entre un exchange y una cola mediante una routing key. */
public record BindingRequest(
        @NotBlank(message = "El exchange es obligatorio")
        @Pattern(regexp = NamingRules.RESOURCE_NAME, message = "El exchange " + NamingRules.RESOURCE_NAME_MESSAGE)
        String exchange,

        @NotBlank(message = "La cola es obligatoria")
        @Pattern(regexp = NamingRules.RESOURCE_NAME, message = "La cola " + NamingRules.RESOURCE_NAME_MESSAGE)
        String queue,

        @NotNull(message = "La routing key es obligatoria (use \"\" para exchanges fanout)")
        @Pattern(regexp = NamingRules.ROUTING_KEY, message = "La routing key " + NamingRules.ROUTING_KEY_MESSAGE)
        String routingKey
) {
}
