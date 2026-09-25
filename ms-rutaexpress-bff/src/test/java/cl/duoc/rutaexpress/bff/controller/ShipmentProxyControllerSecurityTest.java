package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.service.ShipmentProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mismo patron que CatalogProxyControllerSecurityTest: jwt() inyecta la
 * Authentication directamente en el SecurityContext sin pasar por el
 * JwtDecoder real, por lo que aqui solo se cubren los casos sin token y con
 * JWT valido; el caso de audience invalido ya esta cubierto por
 * AudienceValidatorTest y CatalogProxyControllerSecurityTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShipmentProxyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ShipmentProxyService shipmentProxyService;

    @Test
    void shouldReturn401WhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldAllowAccessWithValidJwt() throws Exception {
        when(shipmentProxyService.getAllShipments())
                .thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/api/shipments")
                        .with(jwt().jwt(jwt -> jwt
                                .issuer("https://login.microsoftonline.com/9a929219-a0de-43ee-8628-b1821809e400/v2.0")
                                .audience(List.of("api://c00f97ec-90e5-442b-905f-fe9641efd268"))
                                .claim("sub", "user-123"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
                .andExpect(status().isOk());
    }

}
