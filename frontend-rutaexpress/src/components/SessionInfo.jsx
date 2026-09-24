import { useEffect, useState } from 'react';
import { useMsal } from '@azure/msal-react';
import { loginRequest } from '../authConfig';
import { useApiClient } from '../services/apiClient';
import { decodeJwtPayload, readRoles, readScopes } from '../auth/tokenClaims';
import { useUserRoles } from '../auth/useUserRoles';
import { resolveErrorMessage } from '../utils/errorHandling';

function Chips({ items }) {
  if (items.length === 0) {
    return <span className="muted">—</span>;
  }
  return items.map((item) => (
    <span key={item} className="chip">
      {item}
    </span>
  ));
}

/**
 * Muestra los roles (claim "roles") y scopes (claim "scp") leidos desde los
 * tokens de Azure AD y, para comparar, lo que el BFF extrajo del mismo token
 * tras validarlo (GET /api/me).
 */
function SessionInfo() {
  const { instance, accounts } = useMsal();
  const apiClient = useApiClient();
  const idTokenRoles = useUserRoles();
  const account = accounts[0];

  const [tokenClaims, setTokenClaims] = useState(null);
  const [tokenError, setTokenError] = useState(null);
  const [bffInfo, setBffInfo] = useState(null);
  const [bffError, setBffError] = useState(null);

  useEffect(() => {
    if (!account) {
      return;
    }
    instance
      .acquireTokenSilent({ ...loginRequest, account })
      .then((result) => setTokenClaims(decodeJwtPayload(result.accessToken)))
      .catch(() => setTokenError('No se pudo obtener el access token. Vuelve a iniciar sesion.'));
  }, [instance, account]);

  useEffect(() => {
    const controller = new AbortController();
    apiClient
      .get('/api/me', { signal: controller.signal })
      .then((response) => setBffInfo(response.data))
      .catch((error) => {
        if (!controller.signal.aborted) {
          setBffError(resolveErrorMessage(error, 'No se pudo consultar /api/me en el BFF.'));
        }
      });
    return () => controller.abort();
  }, [apiClient]);

  return (
    <div className="service-list">
      <h2>Mi sesion</h2>
      <dl className="claims">
        <dt>Nombre</dt>
        <dd>{account?.name}</dd>
        <dt>Usuario</dt>
        <dd>{account?.username}</dd>
        <dt>Roles (ID token)</dt>
        <dd>
          <Chips items={idTokenRoles} />
        </dd>
      </dl>

      <h2>Access token para el API</h2>
      {tokenError && (
        <p className="form-error" role="alert">
          {tokenError}
        </p>
      )}
      {!tokenError && !tokenClaims && <p>Obteniendo token...</p>}
      {tokenClaims && (
        <dl className="claims">
          <dt>Scopes (scp)</dt>
          <dd>
            <Chips items={readScopes(tokenClaims)} />
          </dd>
          <dt>Roles (roles)</dt>
          <dd>
            <Chips items={readRoles(tokenClaims)} />
          </dd>
          <dt>Audience (aud)</dt>
          <dd>
            <code>{String(tokenClaims.aud)}</code>
          </dd>
          <dt>Issuer (iss)</dt>
          <dd>
            <code>{tokenClaims.iss}</code>
          </dd>
          <dt>Expira</dt>
          <dd>{tokenClaims.exp ? new Date(tokenClaims.exp * 1000).toLocaleString() : '—'}</dd>
        </dl>
      )}

      <h2>Validado por el BFF (GET /api/me)</h2>
      {bffError && (
        <p className="form-error" role="alert">
          {bffError}
        </p>
      )}
      {!bffError && !bffInfo && <p>Consultando al BFF...</p>}
      {bffInfo && (
        <dl className="claims">
          <dt>Roles</dt>
          <dd>
            <Chips items={bffInfo.roles ?? []} />
          </dd>
          <dt>Scopes</dt>
          <dd>
            <Chips items={bffInfo.scopes ?? []} />
          </dd>
          <dt>Authorities</dt>
          <dd>
            <Chips items={bffInfo.authorities ?? []} />
          </dd>
        </dl>
      )}
    </div>
  );
}

export default SessionInfo;
