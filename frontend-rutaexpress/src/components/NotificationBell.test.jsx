import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import NotificationBell from './NotificationBell';
import { NotificationsProvider } from '../notifications/NotificationsContext';
import { useApiClient } from '../services/apiClient';

jest.mock('../services/apiClient');

const delivered = {
  id: 2,
  shipmentId: 1,
  estadoEnvio: 'EN_TRANSITO',
  asunto: 'Envio #1 en ruta',
  mensaje: 'Hola Ana, tu envio #1 va en ruta hacia Temuco.',
  estado: 'ENVIADA',
  leida: false,
  creadaEn: '2026-10-30T15:00:00Z',
};

const failedEmail = {
  id: 1,
  shipmentId: 1,
  estadoEnvio: 'CREADO',
  asunto: 'Envio #1 registrado',
  mensaje: 'Hola Ana, registramos tu envio #1.',
  estado: 'FALLIDA',
  leida: true,
  creadaEn: '2026-10-30T14:00:00Z',
};

function renderBell(pollIntervalMs = 60000) {
  return render(
    <NotificationsProvider pollIntervalMs={pollIntervalMs}>
      <NotificationBell />
    </NotificationsProvider>
  );
}

describe('NotificationBell', () => {
  let apiClientMock;

  beforeEach(() => {
    apiClientMock = {
      get: jest.fn().mockResolvedValue({ data: [delivered, failedEmail] }),
      put: jest.fn().mockResolvedValue({ data: {} }),
    };
    useApiClient.mockReturnValue(apiClientMock);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('muestra la cantidad de avisos sin leer', async () => {
    renderBell();

    expect(await screen.findByRole('button', { name: 'Avisos (1 sin leer)' })).toBeInTheDocument();
    expect(apiClientMock.get).toHaveBeenCalledWith('/api/notifications/me', expect.anything());
  });

  test('el panel lista los avisos e indica cuando el email no se pudo enviar', async () => {
    renderBell();
    fireEvent.click(await screen.findByRole('button', { name: /avisos \(1 sin leer\)/i }));

    const panel = screen.getByRole('dialog', { name: 'Mis avisos' });
    expect(within(panel).getByText('Envio #1 en ruta')).toBeInTheDocument();
    expect(within(panel).getByText('En ruta')).toBeInTheDocument();
    expect(within(panel).getByText(/no pudimos enviarte este aviso por email/i)).toBeInTheDocument();
  });

  test('al hacer clic en un aviso se marca como leido', async () => {
    renderBell();
    fireEvent.click(await screen.findByRole('button', { name: /avisos \(1 sin leer\)/i }));

    fireEvent.click(screen.getByText('Envio #1 en ruta'));

    await waitFor(() => expect(apiClientMock.put).toHaveBeenCalledWith('/api/notifications/2/leida'));
    expect(screen.getByRole('button', { name: 'Avisos' })).toBeInTheDocument();
  });

  test('un aviso nuevo en la siguiente consulta aparece como aviso emergente', async () => {
    jest.useFakeTimers();
    renderBell(15000);
    await screen.findByRole('button', { name: /avisos \(1 sin leer\)/i });

    // En la carga inicial no se anuncia nada.
    expect(screen.queryByRole('status')).not.toBeInTheDocument();

    const entregado = {
      ...delivered,
      id: 3,
      estadoEnvio: 'ENTREGADO',
      asunto: 'Envio #1 entregado',
      mensaje: 'Hola Ana, tu envio #1 fue entregado en Temuco.',
    };
    apiClientMock.get.mockResolvedValue({ data: [entregado, delivered, failedEmail] });

    await act(async () => {
      jest.advanceTimersByTime(15000);
    });

    const toast = await screen.findByRole('status');
    expect(within(toast).getByText('Envio #1 entregado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Avisos (2 sin leer)' })).toBeInTheDocument();
  });

  test('sin avisos muestra un mensaje vacio', async () => {
    apiClientMock.get.mockResolvedValue({ data: [] });
    renderBell();

    fireEvent.click(screen.getByRole('button', { name: 'Avisos' }));

    expect(await screen.findByText(/aun no tienes avisos/i)).toBeInTheDocument();
  });
});
