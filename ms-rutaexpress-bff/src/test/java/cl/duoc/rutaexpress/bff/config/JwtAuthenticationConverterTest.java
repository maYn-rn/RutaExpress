package cl.duoc.rutaexpress.bff.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que los claims "scp" y "roles" de Azure AD se traduzcan a las
 * authorities SCOPE_x y ROLE_x que usan los @PreAuthorize del BFF.
 */
class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new SecurityConfig(null, null).jwtAuthenticationConverter();

    private static Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
    }

    private List<String> authoritiesOf(Jwt jwt) {
        return converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    void mapsScopesAndRoles() {
        Jwt jwt = baseJwt()
                .claim("scp", "access_as_user")
                .claim("roles", List.of("Admin", "Operador"))
                .build();

        assertThat(authoritiesOf(jwt))
                .containsExactlyInAnyOrder("SCOPE_access_as_user", "ROLE_Admin", "ROLE_Operador");
    }

    @Test
    void tokenWithoutRolesOnlyHasScopes() {
        Jwt jwt = baseJwt().claim("scp", "access_as_user").build();

        assertThat(authoritiesOf(jwt)).containsExactly("SCOPE_access_as_user");
    }

}
