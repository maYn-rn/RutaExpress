// Deben coincidir con los "App roles" de la App Registration en Azure AD y
// con cl.duoc.rutaexpress.bff.security.AccessRules del BFF.
export const ROLES = {
  ADMIN: 'Admin',
  OPERADOR: 'Operador',
  CLIENTE: 'Cliente',
};

// Crear y modificar (POST/PUT en el BFF).
export const WRITE_ROLES = [ROLES.ADMIN, ROLES.OPERADOR];

// Crear envios (POST /api/shipments): el Cliente tambien puede solicitarlos.
export const SHIPMENT_CREATE_ROLES = [ROLES.ADMIN, ROLES.OPERADOR, ROLES.CLIENTE];

// Eliminar (DELETE en el BFF).
export const DELETE_ROLES = [ROLES.ADMIN];

export function hasAnyRole(userRoles, requiredRoles) {
  return requiredRoles.some((role) => userRoles.includes(role));
}
