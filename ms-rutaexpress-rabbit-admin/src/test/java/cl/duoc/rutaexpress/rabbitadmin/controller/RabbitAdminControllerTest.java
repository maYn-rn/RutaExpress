package cl.duoc.rutaexpress.rabbitadmin.controller;

import cl.duoc.rutaexpress.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.rutaexpress.rabbitadmin.exception.ResourceNotFoundException;
import cl.duoc.rutaexpress.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitAdminController.class)
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdminService rabbitAdminService;

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void createsValidQueue() throws Exception {
        when(rabbitAdminService.createQueue(any())).thenReturn(new QueueInfoResponse("pagos.queue", 0, 0));

        postJson("/api/admin/rabbit/queues", "{\"name\":\"pagos.queue\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("pagos.queue"));
    }

    @Test
    void rejectsQueueWithEmptyName() throws Exception {
        postJson("/api/admin/rabbit/queues", "{\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").value(hasItem("El nombre de la cola es obligatorio")));
        verifyNoInteractions(rabbitAdminService);
    }

    @Test
    void rejectsReservedOrInvalidQueueNames() throws Exception {
        postJson("/api/admin/rabbit/queues", "{\"name\":\"amq.secreta\"}").andExpect(status().isBadRequest());
        postJson("/api/admin/rabbit/queues", "{\"name\":\"cola con espacios\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdminService);
    }

    @Test
    void rejectsInvalidQueueConfiguration() throws Exception {
        postJson("/api/admin/rabbit/queues", "{\"name\":\"pagos.queue\",\"messageTtlMs\":-5}")
                .andExpect(status().isBadRequest());
        postJson("/api/admin/rabbit/queues", "{\"name\":\"pagos.queue\",\"deadLetterRoutingKey\":\"pagos.dead\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors")
                        .value(hasItem("Para usar deadLetterRoutingKey tambien debe indicar deadLetterExchange")));
        verifyNoInteractions(rabbitAdminService);
    }

    @Test
    void rejectsUnknownExchangeType() throws Exception {
        postJson("/api/admin/rabbit/exchanges", "{\"name\":\"pagos.exchange\",\"type\":\"headers2\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/admin/rabbit/exchanges", "{\"name\":\"pagos.exchange\"}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdminService);
    }

    @Test
    void createsExchangeCaseInsensitiveType() throws Exception {
        postJson("/api/admin/rabbit/exchanges", "{\"name\":\"pagos.exchange\",\"type\":\"topic\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("topic"));
    }

    @Test
    void rejectsBindingWithoutRoutingKey() throws Exception {
        postJson("/api/admin/rabbit/bindings", "{\"exchange\":\"pagos.exchange\",\"queue\":\"pagos.queue\"}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdminService);
    }

    @Test
    void invalidPathVariableIsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/admin/rabbit/queues/amq.gen-123"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdminService);
    }

    @Test
    void missingQueueIsNotFound() throws Exception {
        when(rabbitAdminService.getQueue("no.existe")).thenThrow(new ResourceNotFoundException("La cola 'no.existe' no existe"));

        mockMvc.perform(get("/api/admin/rabbit/queues/no.existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La cola 'no.existe' no existe"));
    }

    @Test
    void brokerDownIsServiceUnavailable() throws Exception {
        doThrow(new AmqpConnectException(new RuntimeException("down"))).when(rabbitAdminService).deleteQueue("pagos.queue");

        mockMvc.perform(delete("/api/admin/rabbit/queues/pagos.queue"))
                .andExpect(status().isServiceUnavailable());
    }

}
