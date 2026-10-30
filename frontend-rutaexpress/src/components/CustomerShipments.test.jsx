import { render, screen, waitFor, within } from '@testing-library/react';
import CustomerShipments from './CustomerShipments';
import { useApiClient } from '../services/apiClient';
import { useNotifications } from '../notifications/NotificationsContext';

jest.mock('../services/apiClient');
jest.mock('../notifications/NotificationsContext', () => ({ useNotifications: jest.fn() }));

const shipments = [
  { id: 3, origen: 'Santiago', destino: 'Temuco', estado: 'FALLIDO', fechaCreacion: '2026-10-30T14:00:00Z' },
  { id: 5, origen: 'Santiago', destino: 'Arica', estado: 'EN_TRANSITO', fechaCreacion: '2026-10-30T15:00:00Z' },
];

describe('CustomerShipments', () => {
  let apiClientMock;

  beforeEach(() => {
    apiClientMock = { get: jest.fn().mockResolvedValue({ data: shipments }) };
    useApiClient.mockReturnValue(apiClientMock);
    useNotifications.mockReturnValue({
      latestId: 7,
      notifications: [
        {
          id: 7,
          shipmentId: 3,
          estadoEnvio: 'FALLIDO',
          mensaje: 'No pudimos entregar tu envio #3. Motivo: Destinatario ausente.',
          creadaEn: '2026-10-30T16:00:00Z',
        },
      ],
    });
  });

  test('muestra solo numero, ruta, estado legible y fecha, del mas reciente al mas antiguo', async () => {
    render(<CustomerShipments email="ana@rutaexpress.cl" />);

    const table = await screen.findByRole('table');
    const rows = within(table).getAllByRole('row').slice(1);
    expect(rows).toHaveLength(2);
    expect(within(rows[0]).getByText('#5')).toBeInTheDocument();
    expect(within(rows[0]).getByText('Santiago → Arica')).toBeInTheDocument();
    expect(within(rows[0]).getByText('En ruta')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Entrega fallida')).toBeInTheDocument();

    const headers = within(table).getAllByRole('columnheader').map((th) => th.textContent);
    expect(headers).toEqual(['N° envio', 'Ruta', 'Estado', 'Ultima actualizacion']);
    expect(screen.getByText(/envios asociados a ana@rutaexpress.cl/i)).toBeInTheDocument();
  });

  test('una entrega fallida muestra el motivo del ultimo aviso', async () => {
    render(<CustomerShipments email="ana@rutaexpress.cl" />);

    expect(await screen.findByText(/motivo: destinatario ausente/i)).toBeInTheDocument();
  });

  test('no ofrece acciones de administracion', async () => {
    render(<CustomerShipments email="ana@rutaexpress.cl" />);
    await screen.findByRole('table');

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  test('sin envios lo indica', async () => {
    apiClientMock.get.mockResolvedValue({ data: [] });
    render(<CustomerShipments email="ana@rutaexpress.cl" />);

    expect(await screen.findByText(/aun no tienes envios asociados a tu correo/i)).toBeInTheDocument();
  });

  test('si el BFF rechaza la consulta muestra el error', async () => {
    apiClientMock.get.mockRejectedValue({
      response: { status: 403, data: { message: 'El token no incluye el email del cliente' } },
    });
    render(<CustomerShipments email="ana@rutaexpress.cl" />);

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });
});
