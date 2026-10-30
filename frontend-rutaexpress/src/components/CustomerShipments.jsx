import axios from 'axios';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useApiClient } from '../services/apiClient';
import { resolveErrorMessage } from '../utils/errorHandling';
import { useNotifications } from '../notifications/NotificationsContext';
import { formatDateTime, statusLabel } from '../utils/shipmentStatus';

// Estados en los que el cliente necesita saber el motivo (viene en el aviso).
const STATUSES_WITH_REASON = ['FALLIDO', 'CANCELADO'];

/**
 * Vista del cliente: solo sus envios (el BFF filtra por el email del token)
 * y en que estado estan. Se actualiza sola cuando llega un aviso nuevo.
 */
function CustomerShipments({ email }) {
  const apiClient = useApiClient();
  const { notifications, latestId } = useNotifications();
  const [shipments, setShipments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const previousLatestId = useRef(latestId);

  const loadShipments = useCallback(
    (signal, { silent = false } = {}) => {
      if (!silent) {
        setLoading(true);
        setLoadError(null);
      }
      return apiClient
        .get('/api/shipments', { signal })
        .then((response) => {
          setShipments([...(response.data ?? [])].sort((a, b) => b.id - a.id));
        })
        .catch((requestError) => {
          if (axios.isCancel(requestError) || silent) {
            return;
          }
          setLoadError(resolveErrorMessage(requestError, 'No se pudieron cargar tus envios.'));
        })
        .finally(() => {
          if (!silent) {
            setLoading(false);
          }
        });
    },
    [apiClient]
  );

  useEffect(() => {
    const controller = new AbortController();
    loadShipments(controller.signal);
    return () => controller.abort();
  }, [loadShipments]);

  useEffect(() => {
    if (latestId === previousLatestId.current) {
      return;
    }
    const isFirstLoad = previousLatestId.current === null;
    previousLatestId.current = latestId;
    if (!isFirstLoad) {
      loadShipments(undefined, { silent: true });
    }
  }, [latestId, loadShipments]);

  // Ultimo aviso de cada envio: da la fecha de actualizacion y el motivo.
  const latestNotice = (shipmentId) =>
    notifications
      .filter((n) => n.shipmentId === shipmentId)
      .reduce((latest, n) => (!latest || new Date(n.creadaEn) > new Date(latest.creadaEn) ? n : latest), null);

  return (
    <div className="service-list customer-shipments">
      <h2>Mis envios</h2>
      {email && <p className="muted">Envios asociados a {email}</p>}

      {loading && <p>Cargando tus envios...</p>}

      {!loading && loadError && (
        <div>
          <p className="form-error" role="alert">
            {loadError}
          </p>
          <button type="button" onClick={() => loadShipments()}>
            Reintentar
          </button>
        </div>
      )}

      {!loading && !loadError && shipments.length === 0 && (
        <p>Aun no tienes envios asociados a tu correo.</p>
      )}

      {!loading && !loadError && shipments.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>N° envio</th>
              <th>Ruta</th>
              <th>Estado</th>
              <th>Ultima actualizacion</th>
            </tr>
          </thead>
          <tbody>
            {shipments.map((shipment) => {
              const notice = latestNotice(shipment.id);
              const showReason =
                STATUSES_WITH_REASON.includes(shipment.estado) && notice?.estadoEnvio === shipment.estado;
              return (
                <tr key={shipment.id}>
                  <td>#{shipment.id}</td>
                  <td>
                    {shipment.origen} → {shipment.destino}
                  </td>
                  <td>
                    <span className={`status-badge status-${shipment.estado.toLowerCase()}`}>
                      {statusLabel(shipment.estado)}
                    </span>
                    {showReason && <div className="muted customer-reason">{notice.mensaje}</div>}
                  </td>
                  <td>{formatDateTime(notice?.creadaEn ?? shipment.fechaCreacion)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default CustomerShipments;
