import axios from 'axios';
import { useMemo } from 'react';
import { useMsal } from '@azure/msal-react';
import { loginRequest } from '../authConfig';

const BFF_URL = process.env.REACT_APP_BFF_URL || 'http://localhost:8080';

/**
 * Devuelve una instancia de axios apuntando al BFF que adjunta
 * automaticamente el access token de MSAL en cada peticion.
 */
export function useApiClient() {
  const { instance, accounts } = useMsal();

  const apiClient = useMemo(() => {
    const client = axios.create({ baseURL: BFF_URL });

    client.interceptors.request.use(async (config) => {
      const account = accounts[0];
      const tokenRequest = { ...loginRequest, account };

      try {
        const tokenResponse = await instance.acquireTokenSilent(tokenRequest);
        config.headers.Authorization = `Bearer ${tokenResponse.accessToken}`;
        return config;
      } catch (silentError) {
        console.warn('acquireTokenSilent fallo, se redirige para renovar el token:', silentError);

        // A diferencia de acquireTokenPopup, acquireTokenRedirect no
        // devuelve un token: navega el navegador fuera de la pagina actual.
        // Esta peticion nunca se completa en esta carga de pagina; al volver
        // del redirect, MsalProvider procesa el nuevo token y el componente
        // que disparo la llamada original debe reintentarla.
        await instance.acquireTokenRedirect(tokenRequest);
        return Promise.reject(new Error('Redirigiendo para renovar el token de acceso'));
      }
    });

    return client;
  }, [instance, accounts]);

  return apiClient;
}
