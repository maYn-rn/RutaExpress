package cl.duoc.rutaexpress.shipments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdateDTO {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

}
