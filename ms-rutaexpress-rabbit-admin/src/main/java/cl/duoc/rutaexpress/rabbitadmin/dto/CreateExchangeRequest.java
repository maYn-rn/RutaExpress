package cl.duoc.rutaexpress.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Datos para crear un exchange. */
public record CreateExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = NamingRules.RESOURCE_NAME, message = "El nombre del exchange " + NamingRules.RESOURCE_NAME_MESSAGE)
        String name,

        @NotNull(message = "El tipo de exchange es obligatorio (direct, topic o fanout)")
        ExchangeType type,

        Boolean durable
) {

    public boolean durableOrDefault() {
        return durable == null || durable;
    }

}
