package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.service.CatalogProxyService;
import cl.duoc.rutaexpress.bff.service.ShipmentProxyService;
import org.junit.jupiter.api.Test;
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

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la autorizacion por rol/scope definida en AccessRules:
 * lectura = scope access_as_user, escritura = Admin u Operador,
 * crear envios = Admin, Operador o Cliente, eliminacion = solo Admin. Las authorities se inyectan tal como las
 * produciria SecurityConfig#jwtAuthenticationConverter.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleAuthorizationTest {

    private static final String SCOPE = "SCOPE_access_as_user";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CatalogProxyService catalogProxyService;

    @MockBean
    private ShipmentProxyService shipmentProxyService;

    private static JwtRequestPostProcessor user(String... authorities) {
        return jwt()
                .jwt(jwt -> jwt.claim("sub", "user-123")
                        .claim("scp", "access_as_user")
                        .claim("roles", Arrays.stream(authorities)
                                .filter(a -> a.startsWith("ROLE_"))
                                .map(a -> a.substring("ROLE_".length()))
                                .toList()))
                .authorities(Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    @Test
    void readWithoutApiScopeReturns403() throws Exception {
        mockMvc.perform(get("/api/catalog/services").with(user("ROLE_Admin")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
        verifyNoInteractions(catalogProxyService);
    }

    @Test
    void readWithApiScopeAndNoRoleIsAllowed() throws Exception {
        when(shipmentProxyService.getAllShipments()).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/api/shipments").with(user(SCOPE)))
                .andExpect(status().isOk());
    }

    @Test
    void createWithoutRoleReturns403() throws Exception {
        mockMvc.perform(post("/api/catalog/services")
                        .with(user(SCOPE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogProxyService);
    }

    @Test
    void createWithOperadorRoleIsAllowed() throws Exception {
        when(catalogProxyService.createService(anyString()))
                .thenReturn(ResponseEntity.status(201).body("{\"id\":1}"));

        mockMvc.perform(post("/api/catalog/services")
                        .with(user(SCOPE, "ROLE_Operador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Express\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateShipmentStatusWithOperadorRoleIsAllowed() throws Exception {
        when(shipmentProxyService.updateShipmentStatus(anyLong(), any()))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"estado\":\"ACEPTADO\"}"));

        mockMvc.perform(put("/api/shipments/1/estado")
                        .with(user(SCOPE, "ROLE_Operador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACEPTADO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteWithOperadorRoleReturns403() throws Exception {
        mockMvc.perform(delete("/api/shipments/1").with(user(SCOPE, "ROLE_Operador")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shipmentProxyService);
    }

    @Test
    void deleteWithAdminRoleIsAllowed() throws Exception {
        when(catalogProxyService.deleteService(1L)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/api/catalog/services/1").with(user(SCOPE, "ROLE_Admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void meReturnsRolesAndScopesFromToken() throws Exception {
        mockMvc.perform(get("/api/me").with(user(SCOPE, "ROLE_Admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("Admin"))
                .andExpect(jsonPath("$.scopes[0]").value("access_as_user"))
                .andExpect(jsonPath("$.authorities").value(hasItems("ROLE_Admin", SCOPE)));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void clienteCanCreateShipment() throws Exception {
        when(shipmentProxyService.createShipment(anyString()))
                .thenReturn(ResponseEntity.status(201).body("{\"id\":1}"));

        mockMvc.perform(post("/api/shipments")
                        .with(user(SCOPE, "ROLE_Cliente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origen\":\"Santiago\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void clienteCannotCreateCatalogService() throws Exception {
        mockMvc.perform(post("/api/catalog/services")
                        .with(user(SCOPE, "ROLE_Cliente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogProxyService);
    }

    @Test
    void clienteCannotChangeShipmentStatus() throws Exception {
        mockMvc.perform(put("/api/shipments/1/estado")
                        .with(user(SCOPE, "ROLE_Cliente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACEPTADO\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shipmentProxyService);
    }

    @Test
    void clienteCannotDelete() throws Exception {
        mockMvc.perform(delete("/api/shipments/1").with(user(SCOPE, "ROLE_Cliente")))
                .andExpect(status().isForbidden());
    }

}
