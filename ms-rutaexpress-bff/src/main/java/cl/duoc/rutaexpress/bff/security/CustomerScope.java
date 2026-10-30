package cl.duoc.rutaexpress.bff.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Identidad del cliente para los datos "propios" (sus envios y sus
 * notificaciones). El email SIEMPRE sale del token ya validado, nunca del
 * navegador: un Cliente no puede pedir los envios de otra persona.
 *
 * Solo Admin y Operador ven todos los envios. Cualquier otro usuario
 * (Cliente o sin rol asignado) queda limitado a los suyos: si falta un rol
 * en Entra ID, el resultado es ver menos, nunca ver datos ajenos.
 */
@Component
@RequiredArgsConstructor
public class CustomerScope {

    private static final String[] EMAIL_CLAIMS = {"email", "preferred_username", "upn"};

    private final ObjectMapper objectMapper;

    public boolean canSeeAllShipments(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return authorities.contains("ROLE_" + AccessRules.ROLE_ADMIN)
                || authorities.contains("ROLE_" + AccessRules.ROLE_OPERADOR);
    }

    /** Email del usuario autenticado en minusculas, o vacio si el token no lo trae. */
    public Optional<String> email(Jwt jwt) {
        for (String claim : EMAIL_CLAIMS) {
            String value = jwt.getClaimAsString(claim);
            if (value != null && value.contains("@")) {
                return Optional.of(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }

    /**
     * Fuerza el clienteEmail del cuerpo al del token. Si el cliente no
     * escribio su nombre, usa el del token. Si el cuerpo no es JSON se
     * devuelve tal cual y el microservicio responde 400.
     */
    public String assignToCustomer(String body, String email, String tokenName) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (!(node instanceof ObjectNode object)) {
                return body;
            }
            object.put("clienteEmail", email);
            JsonNode name = object.get("clienteNombre");
            if ((name == null || name.asText().isBlank()) && tokenName != null) {
                object.put("clienteNombre", tokenName);
            }
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            return body;
        }
    }

    /** true si el JSON del envio pertenece a ese email. */
    public boolean ownsShipment(String shipmentJson, String email) {
        try {
            JsonNode owner = objectMapper.readTree(shipmentJson).get("clienteEmail");
            return owner != null && owner.asText().equalsIgnoreCase(email);
        } catch (JsonProcessingException ex) {
            return false;
        }
    }

}
