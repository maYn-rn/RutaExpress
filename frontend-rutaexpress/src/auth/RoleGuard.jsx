import { hasAnyRole } from './roles';
import { useUserRoles } from './useUserRoles';

/**
 * Guard de UI: renderiza children solo si el usuario tiene alguno de los
 * roles indicados; si no, muestra fallback. La autorizacion real la aplica
 * el BFF (403), esto solo evita ofrecer acciones que seran rechazadas.
 */
function RoleGuard({ roles, fallback = null, children }) {
  const userRoles = useUserRoles();
  return hasAnyRole(userRoles, roles) ? children : fallback;
}

export default RoleGuard;
