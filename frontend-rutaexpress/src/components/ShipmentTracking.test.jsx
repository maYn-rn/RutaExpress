import { render, screen, within } from '@testing-library/react';
import ShipmentTracking from './ShipmentTracking';
import { useNotifications } from '../notifications/NotificationsContext';

jest.mock('../notifications/NotificationsContext', () => ({ useNotifications: jest.fn() }));

const shipment = { id: 1, estado: 'EN_TRANSITO' };

const notification = (id, estadoEnvio, mensaje, creadaEn, extra = {}) => ({
  id,
  shipmentId: 1,
  estadoEnvio,
  mensaje,
  creadaEn,
  estado: 'ENVIADA',
  leida: true,
  ...extra,
});

function stepStates() {
  return within(screen.getByRole('list', { name: /seguimiento del envio 1/i }))
    .getAllByRole('listitem')
    .map((item) => `${item.textContent}:${item.className.replace('tracking-step ', '')}`);
}

describe('ShipmentTracking', () => {
  beforeEach(() => {
    useNotifications.mockReturnValue({ notifications: [] });
  });

  test('marca las etapas completadas y la actual', () => {
    render(<ShipmentTracking shipment={shipment} />);

    expect(stepStates()).toEqual(['Registrado:done', 'Aceptado:done', 'En ruta:current', 'Entregado:pending']);
  });

  test('una entrega fallida se muestra en la etapa En ruta', () => {
    render(<ShipmentTracking shipment={{ ...shipment, estado: 'FALLIDO' }} />);

    expect(stepStates()).toEqual(['Registrado:done', 'Aceptado:done', 'Entrega fallida:failed', 'Entregado:pending']);
  });

  test('un envio cancelado muestra el aviso y ninguna etapa activa', () => {
    render(<ShipmentTracking shipment={{ ...shipment, estado: 'CANCELADO' }} />);

    expect(screen.getByText(/este envio fue cancelado/i)).toBeInTheDocument();
    expect(stepStates().every((s) => s.endsWith(':pending'))).toBe(true);
  });

  test('el historial muestra solo los avisos de ese envio, del mas antiguo al mas reciente', () => {
    useNotifications.mockReturnValue({
      notifications: [
        notification(3, 'FALLIDO', 'No pudimos entregar tu envio #1. Motivo: Destinatario ausente.', '2026-10-30T16:00:00Z'),
        notification(1, 'CREADO', 'Registramos tu envio #1.', '2026-10-30T14:00:00Z', { estado: 'FALLIDA' }),
        notification(9, 'CREADO', 'Otro envio', '2026-10-30T13:00:00Z', { shipmentId: 2 }),
      ],
    });

    render(<ShipmentTracking shipment={shipment} />);

    const history = screen.getAllByRole('list')[1];
    const items = within(history).getAllByRole('listitem').map((li) => li.textContent);
    expect(items).toHaveLength(2);
    expect(items[0]).toContain('Registramos tu envio #1.');
    expect(items[0]).toContain('(email no enviado)');
    expect(items[1]).toContain('Motivo: Destinatario ausente');
  });

  test('sin avisos lo indica', () => {
    render(<ShipmentTracking shipment={shipment} />);

    expect(screen.getByText(/no hay avisos registrados/i)).toBeInTheDocument();
  });
});
