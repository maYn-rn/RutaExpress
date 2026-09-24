package cl.duoc.rutaexpress.bff.controller;

import cl.duoc.rutaexpress.bff.security.AccessRules;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Devuelve los datos que el BFF obtuvo del JWT ya validado (issuer,
 * audience, firma y vigencia): identidad, roles, scopes y las authorities
 * que Spring Security usa para autorizar.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    @PreAuthorize(AccessRules.CAN_READ)
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        List<String> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", jwt.getClaimAsString("name"));
        body.put("username", jwt.getClaimAsString("preferred_username"));
        body.put("subject", jwt.getSubject());
        body.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        body.put("audience", jwt.getAudience());
        body.put("roles", jwt.hasClaim("roles") ? jwt.getClaimAsStringList("roles") : List.of());
        body.put("scopes", jwt.hasClaim("scp") ? List.of(jwt.getClaimAsString("scp").split(" ")) : List.of());
        body.put("authorities", authorities);
        body.put("expiresAt", jwt.getExpiresAt());
        return body;
    }

}
