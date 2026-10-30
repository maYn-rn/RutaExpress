import { useNotifications } from '../notifications/NotificationsContext';
import { TRACKING_STEPS, formatDateTime, statusLabel } from '../utils/shipmentStatus';

function stepIndex(estado) {
  if (estado === 'FALLIDO') {
    return TRACKING_STEPS.indexOf('EN_TRANSITO');
  }
  return TRACKING_STEPS.indexOf(estado);
}

/**
 * Seguimiento de un envio: etapas (Registrado -> Aceptado -> En ruta ->
 * Entregado) segun el estado actual, y el historial de avisos que recibio
 * el usuario sobre ese envio, del mas antiguo al mas reciente.
 */
function ShipmentTracking({ shipment }) {
  const { notifications } = useNotifications();
  const current = stepIndex(shipment.estado);
  const cancelled = shipment.estado === 'CANCELADO';
  const failed = shipment.estado === 'FALLIDO';

  const history = notifications
    .filter((n) => n.shipmentId === shipment.id)
    .sort((a, b) => new Date(a.creadaEn) - new Date(b.creadaEn));

  return (
    <div className="shipment-tracking">
      {cancelled && (
        <p className="tracking-banner tracking-cancelled" role="status">
          Este envio fue cancelado.
        </p>
      )}

      <ol className="tracking-steps" aria-label={`Seguimiento del envio ${shipment.id}`}>
        {TRACKING_STEPS.map((step, index) => {
          let state = 'pending';
          if (!cancelled && index < current) {
            state = 'done';
          } else if (!cancelled && index === current) {
            state = failed ? 'failed' : 'current';
          }
          const label = state === 'failed' ? statusLabel('FALLIDO') : statusLabel(step);
          return (
            <li key={step} className={`tracking-step ${state}`} aria-current={state === 'current' ? 'step' : undefined}>
              <span className="tracking-dot" aria-hidden="true" />
              <span className="tracking-label">{label}</span>
            </li>
          );
        })}
      </ol>

      <h3 className="tracking-history-title">Historial de avisos</h3>
      {history.length === 0 ? (
        <p className="muted">No hay avisos registrados para tu usuario en este envio.</p>
      ) : (
        <ul className="tracking-history">
          {history.map((n) => (
            <li key={n.id}>
              <span className="tracking-history-date">{formatDateTime(n.creadaEn)}</span>
              <span className={`status-badge status-${(n.estadoEnvio ?? '').toLowerCase()}`}>
                {statusLabel(n.estadoEnvio)}
              </span>
              <span>{n.mensaje}</span>
              {n.estado === 'FALLIDA' && <span className="notification-email-failed"> (email no enviado)</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default ShipmentTracking;
