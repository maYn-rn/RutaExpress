package cl.duoc.rutaexpress.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseDTO {

    private Long id;
    private String nombre;
    private String tipo;
    private BigDecimal tarifaBase;
    private Integer capacidadTotal;
    private Integer capacidadDisponible;

}
