import axios from 'axios';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useApiClient } from '../services/apiClient';
import { resolveErrorMessage } from '../utils/errorHandling';
import RoleGuard from '../auth/RoleGuard';
import { DELETE_ROLES, SHIPMENT_CREATE_ROLES, WRITE_ROLES, hasAnyRole } from '../auth/roles';
import { useUserRoles } from '../auth/useUserRoles';
import { useNotifications } from '../notifications/NotificationsContext';
import ShipmentTracking from './ShipmentTracking';

// Espeja cl.duoc.rutaexpress.shipments.entity.ShipmentStatus#ALLOWED_TRANSITIONS
// del backend: solo se ofrecen como opciones los estados siguientes validos
// desde el estado actual. ENTREGADO y CANCELADO son finales (lista vacia).
const STATUS_TRANSITIONS = {
  CREADO: ['ACEPTADO', 'CANCELADO'],
  ACEPTADO: ['EN_TRANSITO', 'CANCELADO'],
  EN_TRANSITO: ['ENTREGADO', 'FALLIDO', 'CANCELADO'],
  FALLIDO: ['EN_TRANSITO', 'CANCELADO'],
  ENTREGADO: [],
  CANCELADO: [],
};

// Estados en los que se le explica el motivo al cliente en la notificacion.
const STATUSES_WITH_REASON = ['FALLIDO', 'CANCELADO'];

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const EMPTY_FORM = {
  origen: '',
  destino: '',
  pesoKg: '',
  clienteNombre: '',
  clienteEmail: '',
};

function isPositiveNumber(value) {
  return value !== '' && Number.isFinite(Number(value)) && Number(value) > 0;
}

// Admin/Operador indican el email del cliente; para un Cliente lo pone el BFF
// desde su token, por eso no se le pide.
function validateShipmentForm(form, askEmail) {
  const errors = {};

  if (!form.origen.trim()) {
    errors.origen = 'El origen es obligatorio.';
  }
  if (!form.destino.trim()) {
    errors.destino = 'El destino es obligatorio.';
  }
  if (!isPositiveNumber(form.pesoKg)) {
    errors.pesoKg = 'Ingresa un peso valido (mayor a 0).';
  }
  if (!form.clienteNombre.trim()) {
    errors.clienteNombre = 'El nombre del cliente es obligatorio.';
  }
  if (askEmail && !EMAIL_PATTERN.test(form.clienteEmail.trim())) {
    errors.clienteEmail = 'Ingresa un email valido: ahi se notifican los cambios de estado.';
  }

  return errors;
}

function formatFecha(value) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function CreateShipmentForm({ apiClient, onCreated }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const askEmail = hasAnyRole(useUserRoles(), WRITE_ROLES);

  const handleChange = (field) => (event) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    const errors = validateShipmentForm(form, askEmail);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    const payload = {
      origen: form.origen.trim(),
      destino: form.destino.trim(),
      pesoKg: Number(form.pesoKg),
      clienteNombre: form.clienteNombre.trim(),
    };
    if (askEmail) {
      payload.clienteEmail = form.clienteEmail.trim();
    }

    apiClient
      .post('/api/shipments', payload)
      .then((response) => {
        onCreated(response.data);
        setForm(EMPTY_FORM);
        setFieldErrors({});
      })
      .catch((requestError) => {
        setSubmitError(resolveErrorMessage(requestError, 'No se pudo crear el envio.'));
      })
      .finally(() => {
        setSubmitting(false);
      });
  };

  return (
    <form className="service-form" onSubmit={handleSubmit}>
      <h2>Nuevo envio</h2>

      <div className="service-form-grid">
        <div className="form-field">
          <label htmlFor="origen">Origen</label>
          <input
            id="origen"
            type="text"
            value={form.origen}
            onChange={handleChange('origen')}
            disabled={submitting}
          />
          {fieldErrors.origen && <span className="field-error">{fieldErrors.origen}</span>}
        </div>

        <div className="form-field">
          <label htmlFor="destino">Destino</label>
          <input
            id="destino"
            type="text"
            value={form.destino}
            onChange={handleChange('destino')}
            disabled={submitting}
          />
          {fieldErrors.destino && <span className="field-error">{fieldErrors.destino}</span>}
        </div>

        <div className="form-field">
          <label htmlFor="pesoKg">Peso (kg)</label>
          <input
            id="pesoKg"
            type="number"
            min="0"
            step="0.01"
            value={form.pesoKg}
            onChange={handleChange('pesoKg')}
            disabled={submitting}
          />
          {fieldErrors.pesoKg && <span className="field-error">{fieldErrors.pesoKg}</span>}
        </div>

        <div className="form-field">
          <label htmlFor="clienteNombre">Cliente</label>
          <input
            id="clienteNombre"
            type="text"
            value={form.clienteNombre}
            onChange={handleChange('clienteNombre')}
            disabled={submitting}
          />
          {fieldErrors.clienteNombre && <span className="field-error">{fieldErrors.clienteNombre}</span>}
        </div>

        {askEmail && (
          <div className="form-field">
            <label htmlFor="clienteEmail">Email del cliente</label>
            <input
              id="clienteEmail"
              type="email"
              value={form.clienteEmail}
              onChange={handleChange('clienteEmail')}
              disabled={submitting}
            />
            {fieldErrors.clienteEmail && <span className="field-error">{fieldErrors.clienteEmail}</span>}
          </div>
        )}
      </div>

      {submitError && (
        <p className="form-error" role="alert">
          {submitError}
        </p>
      )}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Creando...' : 'Crear envio'}
      </button>
    </form>
  );
}

function ShipmentRow({ shipment, apiClient, onUpdated, onDeleted }) {
  const [selectedStatus, setSelectedStatus] = useState('');
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [rowError, setRowError] = useState(null);
  const [showTracking, setShowTracking] = useState(false);

  const canWrite = hasAnyRole(useUserRoles(), WRITE_ROLES);
  const nextStates = canWrite ? STATUS_TRANSITIONS[shipment.estado] ?? [] : [];

  const handleStatusChange = (event) => {
    const nuevoEstado = event.target.value;
    setSelectedStatus(nuevoEstado);

    if (!nuevoEstado) {
      return;
    }

    const body = { estado: nuevoEstado };
    if (STATUSES_WITH_REASON.includes(nuevoEstado)) {
      const motivo = window.prompt(`Motivo (${nuevoEstado}) que se le informara al cliente:`, '');
      if (motivo === null) {
        setSelectedStatus('');
        return;
      }
      if (motivo.trim()) {
        body.motivo = motivo.trim();
      }
    }

    setUpdatingStatus(true);
    setRowError(null);

    apiClient
      .put(`/api/shipments/${shipment.id}/estado`, body)
      .then((response) => {
        onUpdated(response.data);
        setSelectedStatus('');
      })
      .catch((requestError) => {
        setRowError(resolveErrorMessage(requestError, 'No se pudo actualizar el estado.'));
        setSelectedStatus('');
      })
      .finally(() => {
        setUpdatingStatus(false);
      });
  };

  const handleDelete = () => {
    if (
      !window.confirm(`¿Eliminar el envio de "${shipment.clienteNombre}" (${shipment.origen} -> ${shipment.destino})?`)
    ) {
      return;
    }

    setDeleting(true);
    setRowError(null);

    apiClient
      .delete(`/api/shipments/${shipment.id}`)
      .then(() => {
        onDeleted(shipment.id);
      })
      .catch((requestError) => {
        setRowError(resolveErrorMessage(requestError, 'No se pudo eliminar el envio.'));
        setDeleting(false);
      });
  };

  const busy = updatingStatus || deleting;

  return (
    <>
      <tr>
        <td>{shipment.origen}</td>
        <td>{shipment.destino}</td>
        <td>{shipment.pesoKg}</td>
        <td>
          {shipment.clienteNombre}
          {shipment.clienteEmail && <div className="muted">{shipment.clienteEmail}</div>}
        </td>
        <td>
          <span className={`status-badge status-${shipment.estado.toLowerCase()}`}>{shipment.estado}</span>
          {nextStates.length > 0 && (
            <select value={selectedStatus} onChange={handleStatusChange} disabled={busy}>
              <option value="">Cambiar estado...</option>
              {nextStates.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          )}
        </td>
        <td>{formatFecha(shipment.fechaCreacion)}</td>
        <td className="service-actions">
          <button type="button" onClick={() => setShowTracking((prev) => !prev)} aria-expanded={showTracking}>
            {showTracking ? 'Ocultar seguimiento' : 'Seguimiento'}
          </button>
          <RoleGuard roles={DELETE_ROLES} fallback={<span className="muted">—</span>}>
            <button type="button" className="danger" onClick={handleDelete} disabled={busy}>
              {deleting ? 'Eliminando...' : 'Eliminar'}
            </button>
          </RoleGuard>
        </td>
      </tr>
      {showTracking && (
        <tr className="tracking-row">
          <td colSpan={7}>
            <ShipmentTracking shipment={shipment} />
          </td>
        </tr>
      )}
      {rowError && (
        <tr>
          <td colSpan={7} className="row-error" role="alert">
            {rowError}
          </td>
        </tr>
      )}
    </>
  );
}

function ShipmentList() {
  const apiClient = useApiClient();
  const [shipments, setShipments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const { latestId } = useNotifications();
  const previousLatestId = useRef(latestId);

  // Mismo patron que ServiceList: signal opcional atado a un AbortController
  // para no quedar pegado en "Cargando..." bajo React.StrictMode.
  // silent: refresco en segundo plano, sin reemplazar la tabla por "Cargando...".
  const loadShipments = useCallback(
    (signal, { silent = false } = {}) => {
      if (!silent) {
        setLoading(true);
        setLoadError(null);
      }

      return apiClient
        .get('/api/shipments', { signal })
        .then((response) => {
          setShipments(response.data ?? []);
        })
        .catch((requestError) => {
          if (axios.isCancel(requestError) || silent) {
            return;
          }
          setLoadError(resolveErrorMessage(requestError, 'No se pudo cargar los envios.'));
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
    return () => {
      controller.abort();
    };
  }, [loadShipments]);

  // Llego un aviso nuevo (un envio cambio de estado): se refresca la tabla
  // para que el cliente vea el estado actual sin recargar la pagina.
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

  const handleCreated = (created) => {
    setShipments((prev) => [...prev, created]);
  };

  const handleUpdated = (updated) => {
    setShipments((prev) => prev.map((shipment) => (shipment.id === updated.id ? updated : shipment)));
  };

  const handleDeleted = (id) => {
    setShipments((prev) => prev.filter((shipment) => shipment.id !== id));
  };

  return (
    <div className="service-list">
      <RoleGuard roles={SHIPMENT_CREATE_ROLES}>
        <CreateShipmentForm apiClient={apiClient} onCreated={handleCreated} />
      </RoleGuard>

      <h2>Envios</h2>

      {loading && <p>Cargando envios...</p>}

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
        <p>No hay envios registrados por el momento.</p>
      )}

      {!loading && !loadError && shipments.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Origen</th>
              <th>Destino</th>
              <th>Peso (kg)</th>
              <th>Cliente</th>
              <th>Estado</th>
              <th>Fecha creacion</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {shipments.map((shipment) => (
              <ShipmentRow
                key={shipment.id}
                shipment={shipment}
                apiClient={apiClient}
                onUpdated={handleUpdated}
                onDeleted={handleDeleted}
              />
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default ShipmentList;
