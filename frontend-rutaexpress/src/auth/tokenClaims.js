/**
 * Decodifica el payload de un JWT sin validarlo. Solo se usa para mostrar
 * claims en la UI; la validacion real (firma, issuer, audience, exp) la hacen
 * el API Gateway y el BFF.
 */
export function decodeJwtPayload(token) {
  const parts = typeof token === 'string' ? token.split('.') : [];
  if (parts.length !== 3) {
    return null;
  }
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('')
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/** Claim "roles": App roles asignados al usuario. */
export function readRoles(claims) {
  return Array.isArray(claims?.roles) ? claims.roles : [];
}

/** Claim "scp": scopes delegados separados por espacio. */
export function readScopes(claims) {
  return typeof claims?.scp === 'string' ? claims.scp.split(' ').filter(Boolean) : [];
}
