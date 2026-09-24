package cl.duoc.rutaexpress.shipments.controller;

import cl.duoc.rutaexpress.shipments.dto.ShipmentResponseDTO;
import cl.duoc.rutaexpress.shipments.dto.ShipmentStatusUpdateDTO;
import cl.duoc.rutaexpress.shipments.entity.ShipmentStatus;
import cl.duoc.rutaexpress.shipments.exception.InvalidStatusTransitionException;
import cl.duoc.rutaexpress.shipments.exception.ResourceNotFoundException;
import cl.duoc.rutaexpress.shipments.service.ShipmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void shouldReturnAllShipments() throws Exception {
        ShipmentResponseDTO shipment = ShipmentResponseDTO.builder()
                .id(1L)
                .origen("Santiago")
                .destino("Valparaiso")
                .pesoKg(new BigDecimal("12.50"))
                .estado(ShipmentStatus.CREADO)
                .clienteNombre("Juan Perez")
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(shipmentService.findAll()).thenReturn(List.of(shipment));

        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].origen").value("Santiago"))
                .andExpect(jsonPath("$[0].estado").value("CREADO"));
    }

    @Test
    void shouldReturn404WhenShipmentDoesNotExist() throws Exception {
        when(shipmentService.findById(eq(99L)))
                .thenThrow(new ResourceNotFoundException("Envio no encontrado con id: 99"));

        mockMvc.perform(get("/api/shipments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Envio no encontrado con id: 99"))
                .andExpect(jsonPath("$.path").value("/api/shipments/99"));
    }

    @Test
    void shouldReturn400WhenStatusTransitionIsInvalid() throws Exception {
        when(shipmentService.updateStatus(eq(1L), any(ShipmentStatusUpdateDTO.class)))
                .thenThrow(new InvalidStatusTransitionException(
                        "No se puede cambiar el estado de ENTREGADO a EN_TRANSITO"));

        mockMvc.perform(put("/api/shipments/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShipmentStatusUpdateDTO("EN_TRANSITO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("No se puede cambiar el estado de ENTREGADO a EN_TRANSITO"));
    }

}
