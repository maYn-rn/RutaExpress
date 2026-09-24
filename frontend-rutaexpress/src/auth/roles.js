// Deben coincidir con los "App roles" de la App Registration en Azure AD y
// con cl.duoc.rutaexpress.bff.security.AccessRules del BFF.
export const ROLES = {
  ADMIN: 'Admin',
  OPERADOR: 'Operador',
};

// Crear y modificar (POST/PUT en el BFF).
export const WRITE_ROLES = [ROLES.ADMIN, ROLES.OPERADOR];

// Eliminar (DELETE en el BFF).
export const DELETE_ROLES = [ROLES.ADMIN];

export function hasAnyRole(userRoles, requiredRoles) {
  return requiredRoles.some((role) => userRoles.includes(role));
}
