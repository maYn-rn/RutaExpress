package cl.duoc.rutaexpress.shipments.dto;

import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponseDTO {

    private Long id;
    private String origen;
    private String destino;
    private BigDecimal pesoKg;
    private ShipmentStatus estado;
    private String clienteNombre;
    private String clienteEmail;
    /**
     * Instante absoluto (se serializa en UTC con "Z"): el navegador lo
     * muestra en la hora local del usuario.
     */
    private Instant fechaCreacion;

}
