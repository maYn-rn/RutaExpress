import { useMsal } from '@azure/msal-react';

/**
 * Roles del usuario leidos desde el claim "roles" del ID token emitido por
 * Azure AD (App roles asignados en Enterprise applications).
 */
export function useUserRoles() {
  const { instance, accounts } = useMsal();
  const account = instance.getActiveAccount?.() ?? accounts[0];
  const roles = account?.idTokenClaims?.roles;
  return Array.isArray(roles) ? roles : [];
}
