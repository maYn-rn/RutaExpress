import axios from 'axios';
import { useCallback, useEffect, useState } from 'react';
import { useApiClient } from '../services/apiClient';
import { resolveErrorMessage } from '../utils/errorHandling';
import RoleGuard from '../auth/RoleGuard';
import { DELETE_ROLES, WRITE_ROLES } from '../auth/roles';

const SERVICE_TYPES = ['EXPRESS', 'ESTANDAR'];

const EMPTY_FORM = {
  nombre: '',
  tipo: '',
  tarifaBase: '',
  capacidadTotal: '',
  capacidadDisponible: '',
};

function isNonNegativeNumber(value) {
  return value !== '' && Number.isFinite(Number(value)) && Number(value) >= 0;
}

function validateServiceForm(form) {
  const errors = {};

  if (!form.nombre.trim()) {
    errors.nombre = 'El nombre es obligatorio.';
  }
  if (!form.tipo) {
    errors.tipo = 'Selecciona un tipo.';
  }
  if (!isNonNegativeNumber(form.tarifaBase)) {
    errors.tarifaBase = 'Ingresa una tarifa base valida (0 o mayor).';
  }
  if (!isNonNegativeNumber(form.capacidadTotal)) {
    errors.capacidadTotal = 'Ingresa una capacidad total valida (0 o mayor).';
  }
  if (!isNonNegativeNumber(form.capacidadDisponible)) {
    errors.capacidadDisponible = 'Ingresa una capacidad disponible valida (0 o mayor).';
  }

  return errors;
}

function CreateServiceForm({ apiClient, onCreated }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  const handleChange = (field) => (event) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    const errors = validateServiceForm(form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    const payload = {
      nombre: form.nombre.trim(),
      tipo: form.tipo,
      tarifaBase: Number(form.tarifaBase),
      capacidadTotal: Number(form.capacidadTotal),
      capacidadDisponible: Number(form.capacidadDisponible),
    };

    apiClient
      .post('/api/catalog/services', payload)
      .then((response) => {
        onCreated(response.data);
        setForm(EMPTY_FORM);
        setFieldErrors({});
      })
      .catch((requestError) => {
        setSubmitError(resolveErrorMessage(requestError, 'No se pudo crear el servicio.'));
      })
      .finally(() => {
        setSubmitting(false);
      });
  };

  return (
    <form className="service-form" onSubmit={handleSubmit}>
      <h2>Nuevo servicio</h2>

      <div className="service-form-grid">
        <div className="form-field">
          <label htmlFor="nombre">Nombre</label>
          <input
            id="nombre"
            type="text"
            value={form.nombre}
            onChange={handleChange('nombre')}
            disabled={submitting}
          />
          {fieldErrors.nombre && <span className="field-error">{fieldErrors.nombre}</span>}
        </div>

        <div className="form-field">
          <label htmlFor="tipo">Tipo</label>
          <select id="tipo" value={form.tipo} onChange={handleChange('tipo')} disabled={submitting}>
            <option value="">Selecciona...</option>
            {SERVICE_TYPES.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
          {fieldErrors.tipo && <span className="field-error">{fieldErrors.tipo}</span>}
        </div>

        <div className="form-field">
          <label htmlFor="tarifaBase">Tarifa base</label>
          <input
            id="tarifaBase"
            type="number"
            min="0"
            step="0.01"
            value={form.tarifaBase}
            onChange={handleChange('tarifaBase')}
            disabled={submitting}
          />
          {fieldErrors.tarifaBase && <span className="field-error">{fieldErrors.tarifaBase}</span>}
        </div>

        <div className="form-field">
          <label htmlFor="capacidadTotal">Capacidad total</label>
          <input
            id="capacidadTotal"
            type="number"
            min="0"
            step="1"
            value={form.capacidadTotal}
            onChange={handleChange('capacidadTotal')}
            disabled={submitting}
          />
          {fieldErrors.capacidadTotal && <span className="field-error">{fieldErrors.capacidadTotal}</span>}
        </div>

        <div className="form-field">
          <label htmlFor="capacidadDisponible">Capacidad disponible</label>
          <input
            id="capacidadDisponible"
            type="number"
            min="0"
            step="1"
            value={form.capacidadDisponible}
            onChange={handleChange('capacidadDisponible')}
            disabled={submitting}
          />
          {fieldErrors.capacidadDisponible && (
            <span className="field-error">{fieldErrors.capacidadDisponible}</span>
          )}
        </div>
      </div>

      {submitError && (
        <p className="form-error" role="alert">
          {submitError}
        </p>
      )}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Creando...' : 'Crear servicio'}
      </button>
    </form>
  );
}

function ServiceRow({ service, apiClient, onUpdated, onDeleted }) {
  const [isEditing, setIsEditing] = useState(false);
  const [editValues, setEditValues] = useState({
    tarifaBase: service.tarifaBase,
    capacidadDisponible: service.capacidadDisponible,
  });
  const [editErrors, setEditErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [rowError, setRowError] = useState(null);

  const startEditing = () => {
    setEditValues({ tarifaBase: service.tarifaBase, capacidadDisponible: service.capacidadDisponible });
    setEditErrors({});
    setRowError(null);
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setEditErrors({});
    setRowError(null);
  };

  const handleEditChange = (field) => (event) => {
    setEditValues((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const handleSave = () => {
    const errors = {};
    if (!isNonNegativeNumber(editValues.tarifaBase)) {
      errors.tarifaBase = 'Debe ser 0 o mayor.';
    }
    if (!isNonNegativeNumber(editValues.capacidadDisponible)) {
      errors.capacidadDisponible = 'Debe ser 0 o mayor.';
    }
    setEditErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setSaving(true);
    setRowError(null);

    apiClient
      .put(`/api/catalog/services/${service.id}`, {
        tarifaBase: Number(editValues.tarifaBase),
        capacidadDisponible: Number(editValues.capacidadDisponible),
      })
      .then((response) => {
        onUpdated(response.data);
        setIsEditing(false);
      })
      .catch((requestError) => {
        setRowError(resolveErrorMessage(requestError, 'No se pudo actualizar el servicio.'));
      })
      .finally(() => {
        setSaving(false);
      });
  };

  const handleDelete = () => {
    if (!window.confirm(`¿Eliminar el servicio "${service.nombre}"?`)) {
      return;
    }

    setDeleting(true);
    setRowError(null);

    apiClient
      .delete(`/api/catalog/services/${service.id}`)
      .then(() => {
        onDeleted(service.id);
      })
      .catch((requestError) => {
        setRowError(resolveErrorMessage(requestError, 'No se pudo eliminar el servicio.'));
        setDeleting(false);
      });
  };

  const busy = saving || deleting;

  return (
    <>
      <tr>
        <td>{service.nombre}</td>
        <td>{service.tipo}</td>
        <td>
          {isEditing ? (
            <>
              <input
                type="number"
                min="0"
                step="0.01"
                value={editValues.tarifaBase}
                onChange={handleEditChange('tarifaBase')}
                disabled={saving}
              />
              {editErrors.tarifaBase && <span className="field-error">{editErrors.tarifaBase}</span>}
            </>
          ) : (
            service.tarifaBase
          )}
        </td>
        <td>
          {isEditing ? (
            <>
              <input
                type="number"
                min="0"
                step="1"
                value={editValues.capacidadDisponible}
                onChange={handleEditChange('capacidadDisponible')}
                disabled={saving}
              />
              {editErrors.capacidadDisponible && (
                <span className="field-error">{editErrors.capacidadDisponible}</span>
              )}
            </>
          ) : (
            service.capacidadDisponible
          )}
        </td>
        <td className="service-actions">
          {isEditing ? (
            <>
              <button type="button" onClick={handleSave} disabled={saving}>
                {saving ? 'Guardando...' : 'Guardar'}
              </button>
              <button type="button" onClick={cancelEditing} disabled={saving}>
                Cancelar
              </button>
            </>
          ) : (
            <>
              <RoleGuard roles={WRITE_ROLES} fallback={<span className="muted">Solo lectura</span>}>
                <button type="button" onClick={startEditing} disabled={busy}>
                  Editar
                </button>
              </RoleGuard>
              <RoleGuard roles={DELETE_ROLES}>
                <button type="button" className="danger" onClick={handleDelete} disabled={busy}>
                  {deleting ? 'Eliminando...' : 'Eliminar'}
                </button>
              </RoleGuard>
            </>
          )}
        </td>
      </tr>
      {rowError && (
        <tr>
          <td colSpan={5} className="row-error" role="alert">
            {rowError}
          </td>
        </tr>
      )}
    </>
  );
}

function ServiceList() {
  const apiClient = useApiClient();
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // signal es opcional: el efecto de carga pasa uno atado a un
  // AbortController para poder cancelar la peticion real si el componente
  // se desmonta (incluido el ciclo fantasma de React.StrictMode en
  // desarrollo); el boton "Reintentar" la llama sin signal.
  const loadServices = useCallback(
    (signal) => {
      setLoading(true);
      setLoadError(null);

      return apiClient
        .get('/api/catalog/services', { signal })
        .then((response) => {
          setServices(response.data ?? []);
        })
        .catch((requestError) => {
          if (axios.isCancel(requestError)) {
            return;
          }
          setLoadError(resolveErrorMessage(requestError, 'No se pudo cargar el catalogo de servicios.'));
        })
        .finally(() => {
          setLoading(false);
        });
    },
    [apiClient]
  );

  useEffect(() => {
    const controller = new AbortController();
    loadServices(controller.signal);
    return () => {
      controller.abort();
    };
  }, [loadServices]);

  const handleCreated = (created) => {
    setServices((prev) => [...prev, created]);
  };

  const handleUpdated = (updated) => {
    setServices((prev) => prev.map((service) => (service.id === updated.id ? updated : service)));
  };

  const handleDeleted = (id) => {
    setServices((prev) => prev.filter((service) => service.id !== id));
  };

  return (
    <div className="service-list">
      <RoleGuard roles={WRITE_ROLES}>
        <CreateServiceForm apiClient={apiClient} onCreated={handleCreated} />
      </RoleGuard>

      <h2>Catalogo de servicios</h2>

      {loading && <p>Cargando servicios...</p>}

      {!loading && loadError && (
        <div>
          <p className="form-error" role="alert">
            {loadError}
          </p>
          <button type="button" onClick={() => loadServices()}>
            Reintentar
          </button>
        </div>
      )}

      {!loading && !loadError && services.length === 0 && (
        <p>No hay servicios disponibles por el momento.</p>
      )}

      {!loading && !loadError && services.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Tipo</th>
              <th>Tarifa base</th>
              <th>Capacidad disponible</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {services.map((service) => (
              <ServiceRow
                key={service.id}
                service={service}
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

export default ServiceList;
