package cl.duoc.rutaexpress.bff.security;

/**
 * Reglas de autorizacion (SpEL) usadas en los @PreAuthorize del BFF.
 * Los roles corresponden a los "App roles" definidos en la App Registration
 * de Azure AD (claim "roles") y el scope al expuesto en "Expose an API"
 * (claim "scp"). SecurityConfig#jwtAuthenticationConverter los traduce a
 * ROLE_x y SCOPE_x respectivamente.
 */
public final class AccessRules {

    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_OPERADOR = "Operador";
    public static final String ROLE_CLIENTE = "Cliente";
    public static final String API_SCOPE = "access_as_user";

    /** Cualquier usuario autenticado cuyo token incluya el scope del API. */
    public static final String CAN_READ = "hasAuthority('SCOPE_" + API_SCOPE + "')";

    /** Crear y modificar: Admin u Operador. */
    public static final String CAN_WRITE = CAN_READ
            + " and hasAnyRole('" + ROLE_ADMIN + "','" + ROLE_OPERADOR + "')";

    /** Crear envios: ademas de Admin y Operador, el Cliente puede solicitar envios. */
    public static final String CAN_CREATE_SHIPMENT = CAN_READ
            + " and hasAnyRole('" + ROLE_ADMIN + "','" + ROLE_OPERADOR + "','" + ROLE_CLIENTE + "')";

    /** Eliminar: solo Admin. */
    public static final String CAN_DELETE = CAN_READ + " and hasRole('" + ROLE_ADMIN + "')";

    private AccessRules() {
    }

}
