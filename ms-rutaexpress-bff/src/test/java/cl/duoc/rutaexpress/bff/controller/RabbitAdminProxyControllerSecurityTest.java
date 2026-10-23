package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.service.RabbitAdminProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Administrar RabbitMQ a traves del BFF exige token valido y rol Admin. */
@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminProxyControllerSecurityTest {

    private static final SimpleGrantedAuthority SCOPE = new SimpleGrantedAuthority("SCOPE_access_as_user");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private RabbitAdminProxyService rabbitAdminProxyService;

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/rabbit/queues").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(rabbitAdminProxyService);
    }

    @Test
    void operadorCannotAdministerRabbit() throws Exception {
        mockMvc.perform(post("/api/admin/rabbit/queues")
                        .with(jwt().authorities(SCOPE, new SimpleGrantedAuthority("ROLE_Operador")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rabbitAdminProxyService);
    }

    @Test
    void adminCanCreateQueue() throws Exception {
        when(rabbitAdminProxyService.createQueue(anyString()))
                .thenReturn(ResponseEntity.status(201).body("{\"name\":\"pagos.queue\"}"));

        mockMvc.perform(post("/api/admin/rabbit/queues")
                        .with(jwt().authorities(SCOPE, new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"pagos.queue\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanDeleteQueue() throws Exception {
        when(rabbitAdminProxyService.deleteQueue("pagos.queue")).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/api/admin/rabbit/queues/pagos.queue")
                        .with(jwt().authorities(SCOPE, new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isNoContent());
    }

}
