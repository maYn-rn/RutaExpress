import axios from 'axios';
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useApiClient } from '../services/apiClient';
import { resolveErrorMessage } from '../utils/errorHandling';

// Cada cuanto se consultan los avisos del usuario (GET /api/notifications/me).
export const POLL_INTERVAL_MS = 15000;

// Valor por defecto fuera del proveedor: sin avisos y sin consultas al BFF.
const NotificationsContext = createContext({
  notifications: [],
  unreadCount: 0,
  latestId: null,
  loaded: false,
  error: null,
  markAsRead: () => {},
  markAllAsRead: () => {},
});

/**
 * Consulta periodicamente los avisos del usuario autenticado y los comparte
 * con la campana, el seguimiento de envios y la lista (que se refresca sola
 * cuando llega un aviso nuevo). El BFF filtra por el email del token.
 */
export function NotificationsProvider({ children, pollIntervalMs = POLL_INTERVAL_MS }) {
  const apiClient = useApiClient();
  const [notifications, setNotifications] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(
    (signal) =>
      apiClient
        .get('/api/notifications/me', { signal })
        .then((response) => {
          setNotifications(response.data ?? []);
          setError(null);
          setLoaded(true);
        })
        .catch((requestError) => {
          if (axios.isCancel(requestError)) {
            return;
          }
          setError(resolveErrorMessage(requestError, 'No se pudieron cargar los avisos.'));
        }),
    [apiClient]
  );

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    const timer = setInterval(() => load(controller.signal), pollIntervalMs);
    return () => {
      clearInterval(timer);
      controller.abort();
    };
  }, [load, pollIntervalMs]);

  const markAsRead = useCallback(
    (id) => {
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, leida: true } : n)));
      return apiClient.put(`/api/notifications/${id}/leida`).catch((requestError) => {
        setError(resolveErrorMessage(requestError, 'No se pudo marcar el aviso como leido.'));
      });
    },
    [apiClient]
  );

  const markAllAsRead = useCallback(() => {
    notifications.filter((n) => !n.leida).forEach((n) => markAsRead(n.id));
  }, [notifications, markAsRead]);

  const value = useMemo(
    () => ({
      notifications,
      unreadCount: notifications.filter((n) => !n.leida).length,
      latestId: notifications.reduce((max, n) => (max === null || n.id > max ? n.id : max), null),
      loaded,
      error,
      markAsRead,
      markAllAsRead,
    }),
    [notifications, loaded, error, markAsRead, markAllAsRead]
  );

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>;
}

export function useNotifications() {
  return useContext(NotificationsContext);
}
