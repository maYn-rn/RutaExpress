import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import ShipmentList from './ShipmentList';
import { useApiClient } from '../services/apiClient';
import { useUserRoles } from '../auth/useUserRoles';

jest.mock('../services/apiClient');
jest.mock('../auth/useUserRoles', () => ({ useUserRoles: jest.fn() }));

// Payload real capturado de ms-rutaexpress-shipments corriendo en
// localhost:8082 (POST /api/shipments seguido de PUT .../estado a ACEPTADO).
const sampleShipment = {
  id: 1,
  origen: 'Santiago',
  destino: 'Valparaiso',
  pesoKg: 12.5,
  estado: 'ACEPTADO',
  clienteNombre: 'Juan Perez',
  fechaCreacion: '2026-09-21T00:32:39.221795',
};

describe('ShipmentList', () => {
  let apiClientMock;

  beforeEach(() => {
    useUserRoles.mockReturnValue(['Admin']);
    apiClientMock = {
      get: jest.fn().mockResolvedValue({ data: [sampleShipment] }),
      post: jest.fn(),
      put: jest.fn().mockResolvedValue({ data: { ...sampleShipment, estado: 'EN_TRANSITO' } }),
      delete: jest.fn().mockResolvedValue({}),
    };
    useApiClient.mockReturnValue(apiClientMock);
  });

  test('bajo React.StrictMode carga y muestra la lista sin quedar atascada', async () => {
    render(
      <React.StrictMode>
        <ShipmentList />
      </React.StrictMode>
    );

    expect(screen.getByText(/cargando envios/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument();
    });

    const table = screen.getByRole('table');
    expect(within(table).getByText('Santiago')).toBeInTheDocument();
    expect(within(table).getByText('Valparaiso')).toBeInTheDocument();
    expect(within(table).getByText('Juan Perez')).toBeInTheDocument();
    expect(within(table).getByText('ACEPTADO')).toBeInTheDocument();

    // La fecha debe mostrarse formateada (toLocaleString), no el ISO crudo.
    expect(within(table).queryByText(sampleShipment.fechaCreacion)).not.toBeInTheDocument();
  });

  test('el selector de estado solo ofrece las transiciones validas desde ACEPTADO', async () => {
    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    const select = screen.getByRole('combobox');
    const optionLabels = within(select)
      .getAllByRole('option')
      .map((option) => option.textContent);

    expect(optionLabels).toEqual(['Cambiar estado...', 'EN_TRANSITO', 'CANCELADO']);
  });

  test('no muestra selector de estado para envios en estado final (ENTREGADO)', async () => {
    apiClientMock.get.mockResolvedValue({ data: [{ ...sampleShipment, estado: 'ENTREGADO' }] });

    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.getByText('ENTREGADO')).toBeInTheDocument();
  });

  test('cambiar el estado hace PUT con el body correcto y refresca la fila', async () => {
    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'EN_TRANSITO' } });

    await waitFor(() => {
      expect(apiClientMock.put).toHaveBeenCalledWith('/api/shipments/1/estado', { estado: 'EN_TRANSITO' });
    });

    const table = screen.getByRole('table');
    await waitFor(() => {
      expect(within(table).getByText('EN_TRANSITO')).toBeInTheDocument();
    });
  });

  test('eliminar pide confirmacion y hace DELETE al id correcto', async () => {
    window.confirm = jest.fn().mockReturnValue(true);

    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /eliminar/i }));

    await waitFor(() => {
      expect(apiClientMock.delete).toHaveBeenCalledWith('/api/shipments/1');
    });

    await waitFor(() => {
      expect(screen.getByText(/no hay envios registrados/i)).toBeInTheDocument();
    });
  });

  test('sin roles no ofrece cambiar estado, crear ni eliminar', async () => {
    useUserRoles.mockReturnValue([]);
    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByText(/nuevo envio/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /eliminar/i })).not.toBeInTheDocument();
  });

  test('Operador puede cambiar estado pero no eliminar', async () => {
    useUserRoles.mockReturnValue(['Operador']);
    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    expect(screen.getByRole('combobox')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /eliminar/i })).not.toBeInTheDocument();
  });

  test('Cliente puede crear envios pero no cambiar estado ni eliminar', async () => {
    useUserRoles.mockReturnValue(['Cliente']);
    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    expect(screen.getByRole('button', { name: /crear envio/i })).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /eliminar/i })).not.toBeInTheDocument();
  });

  test('desde EN_TRANSITO se puede marcar FALLIDO y el motivo viaja en el PUT', async () => {
    apiClientMock.get.mockResolvedValue({ data: [{ ...sampleShipment, estado: 'EN_TRANSITO' }] });
    apiClientMock.put.mockResolvedValue({ data: { ...sampleShipment, estado: 'FALLIDO' } });
    window.prompt = jest.fn().mockReturnValue('Destinatario ausente');

    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    const options = within(screen.getByRole('combobox'))
      .getAllByRole('option')
      .map((option) => option.textContent);
    expect(options).toEqual(['Cambiar estado...', 'ENTREGADO', 'FALLIDO', 'CANCELADO']);

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'FALLIDO' } });

    await waitFor(() => {
      expect(apiClientMock.put).toHaveBeenCalledWith('/api/shipments/1/estado', {
        estado: 'FALLIDO',
        motivo: 'Destinatario ausente',
      });
    });
  });

  test('cancelar el prompt del motivo no cambia el estado', async () => {
    window.prompt = jest.fn().mockReturnValue(null);

    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'CANCELADO' } });

    expect(apiClientMock.put).not.toHaveBeenCalled();
  });

  test('Admin debe indicar el email del cliente al crear un envio', async () => {
    apiClientMock.post.mockResolvedValue({ data: { ...sampleShipment, id: 2 } });

    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('Origen'), { target: { value: 'Santiago' } });
    fireEvent.change(screen.getByLabelText('Destino'), { target: { value: 'Temuco' } });
    fireEvent.change(screen.getByLabelText('Peso (kg)'), { target: { value: '3' } });
    fireEvent.change(screen.getByLabelText('Cliente'), { target: { value: 'Ana' } });
    fireEvent.click(screen.getByRole('button', { name: /crear envio/i }));

    expect(await screen.findByText(/ingresa un email valido/i)).toBeInTheDocument();
    expect(apiClientMock.post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText('Email del cliente'), { target: { value: 'ana@rutaexpress.cl' } });
    fireEvent.click(screen.getByRole('button', { name: /crear envio/i }));

    await waitFor(() => {
      expect(apiClientMock.post).toHaveBeenCalledWith('/api/shipments', {
        origen: 'Santiago',
        destino: 'Temuco',
        pesoKg: 3,
        clienteNombre: 'Ana',
        clienteEmail: 'ana@rutaexpress.cl',
      });
    });
  });

  test('Cliente no ve el campo email: el BFF usa el de su token', async () => {
    useUserRoles.mockReturnValue(['Cliente']);
    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    expect(screen.queryByLabelText('Email del cliente')).not.toBeInTheDocument();
  });

  test('el boton Seguimiento muestra las etapas del envio', async () => {
    render(<ShipmentList />);
    await waitFor(() => expect(screen.queryByText(/cargando envios/i)).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Seguimiento' }));

    expect(screen.getByRole('list', { name: /seguimiento del envio 1/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ocultar seguimiento' })).toBeInTheDocument();
  });
});
