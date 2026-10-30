package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.service.NotificationProxyService;
import cl.duoc.rutaexpress.bff.service.ShipmentProxyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quien no es Admin ni Operador (Cliente o sin rol) ve y crea unicamente
 * envios a su nombre, y cada usuario ve solo sus notificaciones. El email siempre sale del token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerScopeAuthorizationTest {

    private static final String SCOPE = "SCOPE_access_as_user";
    private static final String EMAIL = "ana@rutaexpress.cl";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ShipmentProxyService shipmentProxyService;

    @MockBean
    private NotificationProxyService notificationProxyService;

    private static JwtRequestPostProcessor user(String email, String... authorities) {
        return jwt()
                .jwt(jwt -> {
                    jwt.claim("sub", "user-123").claim("name", "Ana Token");
                    if (email != null) {
                        jwt.claim("preferred_username", email);
                    }
                })
                .authorities(Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @Test
    void clienteListsOnlyOwnShipments() throws Exception {
        when(shipmentProxyService.getShipmentsByCustomer(EMAIL)).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/api/shipments").with(user("Ana@RutaExpress.cl", SCOPE, "ROLE_Cliente")))
                .andExpect(status().isOk());

        verify(shipmentProxyService).getShipmentsByCustomer(EMAIL);
        verify(shipmentProxyService, never()).getAllShipments();
    }

    @Test
    void userWithoutRoleListsOnlyOwnShipments() throws Exception {
        when(shipmentProxyService.getShipmentsByCustomer(EMAIL)).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/api/shipments").with(user(EMAIL, SCOPE)))
                .andExpect(status().isOk());

        verify(shipmentProxyService).getShipmentsByCustomer(EMAIL);
        verify(shipmentProxyService, never()).getAllShipments();
    }

    @Test
    void userWithoutRoleCannotReadSomeoneElsesShipment() throws Exception {
        when(shipmentProxyService.getShipmentById(5L))
                .thenReturn(ResponseEntity.ok("{\"id\":5,\"clienteEmail\":\"luis@rutaexpress.cl\"}"));

        mockMvc.perform(get("/api/shipments/5").with(user(EMAIL, SCOPE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void operadorListsAllShipmentsEvenIfAlsoCliente() throws Exception {
        when(shipmentProxyService.getAllShipments()).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/api/shipments").with(user(EMAIL, SCOPE, "ROLE_Operador", "ROLE_Cliente")))
                .andExpect(status().isOk());

        verify(shipmentProxyService).getAllShipments();
    }

    @Test
    void clienteWithoutEmailInTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/shipments").with(user(null, SCOPE, "ROLE_Cliente")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shipmentProxyService);
    }

    @Test
    void clienteCannotReadSomeoneElsesShipment() throws Exception {
        when(shipmentProxyService.getShipmentById(5L))
                .thenReturn(ResponseEntity.ok("{\"id\":5,\"clienteEmail\":\"luis@rutaexpress.cl\"}"));

        mockMvc.perform(get("/api/shipments/5").with(user(EMAIL, SCOPE, "ROLE_Cliente")))
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteCanReadOwnShipment() throws Exception {
        when(shipmentProxyService.getShipmentById(5L))
                .thenReturn(ResponseEntity.ok("{\"id\":5,\"clienteEmail\":\"ana@rutaexpress.cl\"}"));

        mockMvc.perform(get("/api/shipments/5").with(user(EMAIL, SCOPE, "ROLE_Cliente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void clienteCreatesShipmentWithEmailFromTokenNotFromBody() throws Exception {
        when(shipmentProxyService.createShipment(anyString())).thenReturn(ResponseEntity.status(201).body("{}"));

        mockMvc.perform(post("/api/shipments")
                        .with(user(EMAIL, SCOPE, "ROLE_Cliente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"Santiago\",\"clienteEmail\":\"otro@rutaexpress.cl\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(shipmentProxyService).createShipment(body.capture());
        assertThat(body.getValue())
                .contains("\"clienteEmail\":\"ana@rutaexpress.cl\"")
                .contains("\"clienteNombre\":\"Ana Token\"")
                .doesNotContain("otro@rutaexpress.cl");
    }

    @Test
    void adminCreatesShipmentForAnyCustomer() throws Exception {
        String body = "{\"origen\":\"Santiago\",\"clienteEmail\":\"luis@rutaexpress.cl\"}";
        when(shipmentProxyService.createShipment(body)).thenReturn(ResponseEntity.status(201).body("{}"));

        mockMvc.perform(post("/api/shipments")
                        .with(user(EMAIL, SCOPE, "ROLE_Admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(shipmentProxyService).createShipment(body);
    }

    @Test
    void notificationsAreRequestedWithTokenEmail() throws Exception {
        when(notificationProxyService.getCustomerNotifications(EMAIL)).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/api/notifications/me").with(user(EMAIL, SCOPE, "ROLE_Cliente")))
                .andExpect(status().isOk());

        verify(notificationProxyService).getCustomerNotifications(EMAIL);
    }

    @Test
    void markAsReadUsesTokenEmail() throws Exception {
        when(notificationProxyService.markAsRead(3L, EMAIL)).thenReturn(ResponseEntity.ok("{}"));

        mockMvc.perform(put("/api/notifications/3/leida").with(user(EMAIL, SCOPE, "ROLE_Cliente")))
                .andExpect(status().isOk());

        verify(notificationProxyService).markAsRead(3L, EMAIL);
    }

    @Test
    void notificationsWithoutTokenReturn401() throws Exception {
        mockMvc.perform(get("/api/notifications/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteCannotSeeMessagingMetrics() throws Exception {
        mockMvc.perform(get("/api/messaging/metrics").with(user(EMAIL, SCOPE, "ROLE_Cliente")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notificationProxyService);
    }

    @Test
    void operadorCanSeeMessagingMetrics() throws Exception {
        when(notificationProxyService.getMessagingMetrics()).thenReturn(ResponseEntity.ok("{}"));

        mockMvc.perform(get("/api/messaging/metrics").with(user(EMAIL, SCOPE, "ROLE_Operador")))
                .andExpect(status().isOk());
    }

}
