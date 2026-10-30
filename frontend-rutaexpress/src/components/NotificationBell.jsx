import { useEffect, useRef, useState } from 'react';
import { useNotifications } from '../notifications/NotificationsContext';
import { formatDateTime, statusLabel } from '../utils/shipmentStatus';

const TOAST_DURATION_MS = 6000;

function NotificationItem({ notification, onRead }) {
  const className = `notification-item${notification.leida ? '' : ' unread'}`;
  return (
    <li className={className}>
      <button type="button" className="notification-item-button" onClick={() => onRead(notification.id)}>
        <span className="notification-item-header">
          <span className={`status-badge status-${(notification.estadoEnvio ?? '').toLowerCase()}`}>
            {statusLabel(notification.estadoEnvio)}
          </span>
          <strong>{notification.asunto}</strong>
        </span>
        <span className="notification-item-message">{notification.mensaje}</span>
        <span className="notification-item-meta">
          {formatDateTime(notification.creadaEn)}
          {notification.estado === 'FALLIDA' && (
            <span className="notification-email-failed"> · No pudimos enviarte este aviso por email</span>
          )}
        </span>
      </button>
    </li>
  );
}

/**
 * Campana en la barra superior: contador de avisos sin leer, panel con el
 * historial y un aviso emergente cuando llega un cambio de estado nuevo.
 */
function NotificationBell() {
  const { notifications, unreadCount, loaded, error, markAsRead, markAllAsRead } = useNotifications();
  const [open, setOpen] = useState(false);
  const [toast, setToast] = useState(null);
  const seenIds = useRef(null);

  // Solo se anuncian los avisos que llegan despues de la primera carga.
  useEffect(() => {
    if (!loaded) {
      return;
    }
    if (seenIds.current === null) {
      seenIds.current = new Set(notifications.map((n) => n.id));
      return;
    }
    const fresh = notifications.filter((n) => !seenIds.current.has(n.id));
    fresh.forEach((n) => seenIds.current.add(n.id));
    if (fresh.length > 0) {
      setToast(fresh.reduce((newest, n) => (n.id > newest.id ? n : newest)));
    }
  }, [notifications, loaded]);

  useEffect(() => {
    if (!toast) {
      return undefined;
    }
    const timer = setTimeout(() => setToast(null), TOAST_DURATION_MS);
    return () => clearTimeout(timer);
  }, [toast]);

  const label = unreadCount > 0 ? `Avisos (${unreadCount} sin leer)` : 'Avisos';

  return (
    <div className="notification-bell">
      <button
        type="button"
        className="bell-button"
        aria-label={label}
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
      >
        <span aria-hidden="true">🔔</span>
        {unreadCount > 0 && <span className="bell-count">{unreadCount}</span>}
      </button>

      {open && (
        <div className="notification-panel" role="dialog" aria-label="Mis avisos">
          <div className="notification-panel-header">
            <strong>Mis avisos</strong>
            {unreadCount > 0 && (
              <button type="button" className="link-button" onClick={markAllAsRead}>
                Marcar todo como leido
              </button>
            )}
          </div>

          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}

          {notifications.length === 0 ? (
            <p className="muted notification-empty">
              {loaded ? 'Aun no tienes avisos. Te avisaremos cada cambio de estado de tus envios.' : 'Cargando avisos...'}
            </p>
          ) : (
            <ul className="notification-list">
              {notifications.map((notification) => (
                <NotificationItem key={notification.id} notification={notification} onRead={markAsRead} />
              ))}
            </ul>
          )}
        </div>
      )}

      {toast && (
        <div className="notification-toast" role="status">
          <strong>{toast.asunto}</strong>
          <span>{toast.mensaje}</span>
          <button type="button" className="link-button" onClick={() => setToast(null)}>
            Cerrar
          </button>
        </div>
      )}
    </div>
  );
}

export default NotificationBell;
