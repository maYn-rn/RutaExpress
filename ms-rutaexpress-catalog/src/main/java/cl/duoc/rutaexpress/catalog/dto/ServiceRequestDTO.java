package cl.duoc.rutaexpress.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    @NotNull(message = "La tarifa base es obligatoria")
    @PositiveOrZero(message = "La tarifa base no puede ser negativa")
    private BigDecimal tarifaBase;

    @NotNull(message = "La capacidad total es obligatoria")
    @Positive(message = "La capacidad total debe ser mayor a 0")
    private Integer capacidadTotal;

    @NotNull(message = "La capacidad disponible es obligatoria")
    @PositiveOrZero(message = "La capacidad disponible no puede ser negativa")
    private Integer capacidadDisponible;

}
