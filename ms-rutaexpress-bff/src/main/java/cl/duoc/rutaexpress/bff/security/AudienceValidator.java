package cl.duoc.rutaexpress.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;

/**
 * Azure AD no siempre incluye la validacion de audience por defecto al usar
 * issuer-uri, por lo que se valida explicitamente que el claim "aud" del
 * token coincida con el Client ID configurado en AZURE_AD_AUDIENCE (que
 * contiene solo el Client ID puro). Segun el tipo de token que emita Azure
 * AD, el claim "aud" puede llegar como el Client ID puro o con el prefijo
 * "api://", por lo que ambas variantes se derivan y aceptan.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final String API_URI_PREFIX = "api://";

    private static final OAuth2Error INVALID_AUDIENCE_ERROR = new OAuth2Error(
            "invalid_token",
            "El token no contiene el audience esperado",
            null
    );

    private final Set<String> acceptedAudiences;

    public AudienceValidator(String expectedClientId) {
        this.acceptedAudiences = Set.of(expectedClientId, API_URI_PREFIX + expectedClientId);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        if (audiences != null && audiences.stream().anyMatch(acceptedAudiences::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE_ERROR);
    }

}
