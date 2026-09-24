import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import ServiceList from './ServiceList';
import { useApiClient } from '../services/apiClient';
import { useUserRoles } from '../auth/useUserRoles';

jest.mock('../services/apiClient');
jest.mock('../auth/useUserRoles', () => ({ useUserRoles: jest.fn() }));

const sampleServices = [
  {
    id: 1,
    nombre: 'Envio Express',
    tipo: 'EXPRESS',
    tarifaBase: 5000,
    capacidadTotal: 100,
    capacidadDisponible: 80,
  },
];

describe('ServiceList bajo React.StrictMode', () => {
  beforeEach(() => {
    useUserRoles.mockReturnValue(['Admin']);
    useApiClient.mockReturnValue({
      get: jest.fn().mockResolvedValue({ data: sampleServices }),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
    });
  });

  test('muestra la lista tras cargar, sin quedar atascada en "Cargando..."', async () => {
    render(
      <React.StrictMode>
        <ServiceList />
      </React.StrictMode>
    );

    expect(screen.getByText(/cargando servicios/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText(/cargando servicios/i)).not.toBeInTheDocument();
    });

    const table = screen.getByRole('table');
    expect(within(table).getByText('Envio Express')).toBeInTheDocument();
    expect(within(table).getByText('EXPRESS')).toBeInTheDocument();
    expect(within(table).getByText('5000')).toBeInTheDocument();
  });
});

describe('ServiceList segun roles del token', () => {
  beforeEach(() => {
    useApiClient.mockReturnValue({
      get: jest.fn().mockResolvedValue({ data: sampleServices }),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
    });
  });

  test('sin roles solo muestra el catalogo en modo lectura', async () => {
    useUserRoles.mockReturnValue([]);
    render(<ServiceList />);
    await waitFor(() => expect(screen.queryByText(/cargando servicios/i)).not.toBeInTheDocument());

    expect(screen.queryByText(/nuevo servicio/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /editar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /eliminar/i })).not.toBeInTheDocument();
    expect(screen.getByText(/solo lectura/i)).toBeInTheDocument();
  });

  test('Operador puede crear y editar pero no eliminar', async () => {
    useUserRoles.mockReturnValue(['Operador']);
    render(<ServiceList />);
    await waitFor(() => expect(screen.queryByText(/cargando servicios/i)).not.toBeInTheDocument());

    expect(screen.getByText(/nuevo servicio/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /editar/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /eliminar/i })).not.toBeInTheDocument();
  });

  test('Admin puede eliminar', async () => {
    useUserRoles.mockReturnValue(['Admin']);
    render(<ServiceList />);
    await waitFor(() => expect(screen.queryByText(/cargando servicios/i)).not.toBeInTheDocument());

    expect(screen.getByRole('button', { name: /eliminar/i })).toBeInTheDocument();
  });
});
