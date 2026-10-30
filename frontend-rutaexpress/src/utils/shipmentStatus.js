// Nombres legibles de cl.duoc.rutaexpress.shipments.entity.ShipmentStatus.
export const STATUS_LABELS = {
  CREADO: 'Registrado',
  ACEPTADO: 'Aceptado',
  EN_TRANSITO: 'En ruta',
  ENTREGADO: 'Entregado',
  FALLIDO: 'Entrega fallida',
  CANCELADO: 'Cancelado',
};

// Camino normal de un envio; FALLIDO ocurre en la etapa EN_TRANSITO.
export const TRACKING_STEPS = ['CREADO', 'ACEPTADO', 'EN_TRANSITO', 'ENTREGADO'];

export function statusLabel(status) {
  return STATUS_LABELS[status] ?? status;
}

export function formatDateTime(value) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}
