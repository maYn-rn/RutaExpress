package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.service.CatalogProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * jwt() de spring-security-test inyecta la Authentication directamente en el
 * SecurityContext, sin pasar por el JwtDecoder real, por lo que el caso de
 * audience invalido se simula haciendo que el JwtDecoder (mockeado) lance la
 * excepcion que lanzaria el DelegatingOAuth2TokenValidator real ante un
 * token con audience incorrecto. La logica del validador de audience en si
 * se prueba de forma aislada en AudienceValidatorTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogProxyControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CatalogProxyService catalogProxyService;

    @Test
    void shouldReturn401WhenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/catalog/services"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldAllowAccessWithValidJwt() throws Exception {
        when(catalogProxyService.getAllServices())
                .thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/api/catalog/services")
                        .with(jwt().jwt(jwt -> jwt
                                .issuer("https://login.microsoftonline.com/9a929219-a0de-43ee-8628-b1821809e400/v2.0")
                                .audience(List.of("api://c00f97ec-90e5-442b-905f-fe9641efd268"))
                                .claim("sub", "user-123"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenAudienceIsInvalid() throws Exception {
        OAuth2Error invalidAudienceError = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "El token no contiene el audience esperado",
                null
        );
        when(jwtDecoder.decode(anyString()))
                .thenThrow(new InvalidBearerTokenException(
                        invalidAudienceError.getDescription(),
                        new JwtValidationException(invalidAudienceError.getDescription(), List.of(invalidAudienceError))));

        mockMvc.perform(get("/api/catalog/services")
                        .header("Authorization", "Bearer token-con-audience-incorrecto"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

}
