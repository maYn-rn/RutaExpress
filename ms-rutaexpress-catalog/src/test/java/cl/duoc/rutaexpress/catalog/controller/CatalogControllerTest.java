package cl.duoc.rutaexpress.catalog.controller;

import cl.duoc.rutaexpress.catalog.dto.ServiceResponseDTO;
import cl.duoc.rutaexpress.catalog.exception.ResourceNotFoundException;
import cl.duoc.rutaexpress.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;

    @Test
    void shouldReturnAllServices() throws Exception {
        ServiceResponseDTO service = ServiceResponseDTO.builder()
                .id(1L)
                .nombre("Envío Express")
                .tipo("EXPRESS")
                .tarifaBase(new BigDecimal("5000.00"))
                .capacidadTotal(100)
                .capacidadDisponible(80)
                .build();

        when(catalogService.findAll()).thenReturn(List.of(service));

        mockMvc.perform(get("/api/catalog/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("Envío Express"))
                .andExpect(jsonPath("$[0].tipo").value("EXPRESS"));
    }

    @Test
    void shouldReturnServiceById() throws Exception {
        ServiceResponseDTO service = ServiceResponseDTO.builder()
                .id(1L)
                .nombre("Envío Estándar")
                .tipo("ESTANDAR")
                .tarifaBase(new BigDecimal("2500.00"))
                .capacidadTotal(50)
                .capacidadDisponible(30)
                .build();

        when(catalogService.findById(1L)).thenReturn(service);

        mockMvc.perform(get("/api/catalog/services/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Envío Estándar"));
    }

    @Test
    void shouldReturn404WhenServiceDoesNotExist() throws Exception {
        when(catalogService.findById(eq(99L)))
                .thenThrow(new ResourceNotFoundException("Servicio no encontrado con id: 99"));

        mockMvc.perform(get("/api/catalog/services/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Servicio no encontrado con id: 99"))
                .andExpect(jsonPath("$.path").value("/api/catalog/services/99"));
    }

}
