import { decodeJwtPayload, readRoles, readScopes } from './tokenClaims';
import { hasAnyRole } from './roles';

function fakeJwt(payload) {
  const encode = (obj) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode(payload)}.firma`;
}

test('decodifica el payload y lee roles y scopes', () => {
  const claims = decodeJwtPayload(fakeJwt({ roles: ['Admin'], scp: 'access_as_user otro' }));
  expect(readRoles(claims)).toEqual(['Admin']);
  expect(readScopes(claims)).toEqual(['access_as_user', 'otro']);
});

test('devuelve null o listas vacias ante tokens invalidos', () => {
  expect(decodeJwtPayload('no-es-jwt')).toBeNull();
  expect(readRoles(null)).toEqual([]);
  expect(readScopes({})).toEqual([]);
});

test('hasAnyRole compara contra los roles requeridos', () => {
  expect(hasAnyRole(['Operador'], ['Admin', 'Operador'])).toBe(true);
  expect(hasAnyRole([], ['Admin'])).toBe(false);
});
