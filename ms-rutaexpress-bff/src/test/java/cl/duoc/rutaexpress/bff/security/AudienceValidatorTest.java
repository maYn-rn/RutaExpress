package cl.duoc.rutaexpress.bff.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private static final String CLIENT_ID = "c00f97ec-90e5-442b-905f-fe9641efd268";
    private static final String API_URI_AUDIENCE = "api://" + CLIENT_ID;

    private final AudienceValidator validator = new AudienceValidator(CLIENT_ID);

    @Test
    void shouldSucceedWhenAudienceIsThePlainClientId() {
        Jwt jwt = buildJwt(List.of(CLIENT_ID));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldSucceedWhenAudienceHasTheApiUriPrefix() {
        Jwt jwt = buildJwt(List.of(API_URI_AUDIENCE));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldFailWhenAudienceDoesNotMatch() {
        Jwt jwt = buildJwt(List.of("api://otra-aplicacion-distinta"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldFailWhenAudienceClaimIsMissing() {
        Jwt jwt = buildJwt(List.of());

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt buildJwt(List<String> audience) {
        return Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", "user-123")
                .audience(audience)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

}
