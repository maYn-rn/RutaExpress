package cl.duoc.rutaexpress.shipments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdateDTO {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    /** Opcional: explica el cambio al cliente (ej. por que FALLIDO o CANCELADO). */
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String motivo;

    public ShipmentStatusUpdateDTO(String estado) {
        this(estado, null);
    }

}
