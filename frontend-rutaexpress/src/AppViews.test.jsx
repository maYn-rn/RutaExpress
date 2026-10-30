import { render, screen } from '@testing-library/react';
import App from './App';
import { useUserRoles } from './auth/useUserRoles';
import { useApiClient } from './services/apiClient';

jest.mock('@azure/msal-react', () => ({
  useMsal: () => ({
    instance: { loginRedirect: jest.fn(), logoutRedirect: jest.fn() },
    accounts: [{ name: 'Ana Cliente', username: 'ana@rutaexpress.cl' }],
  }),
  AuthenticatedTemplate: ({ children }) => children,
  UnauthenticatedTemplate: () => null,
}));
jest.mock('./auth/useUserRoles', () => ({ useUserRoles: jest.fn() }));
jest.mock('./services/apiClient');

describe('Vista segun el rol', () => {
  beforeEach(() => {
    useApiClient.mockReturnValue({ get: jest.fn().mockResolvedValue({ data: [] }), put: jest.fn() });
  });

  test.each([[['Cliente']], [[]]])('roles %p ven solo "Mis envios", sin catalogo ni pestanas', async (roles) => {
    useUserRoles.mockReturnValue(roles);
    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Mis envios' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Catalogo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mi sesion' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /avisos/i })).toBeInTheDocument();
  });

  test('Operador mantiene las pestanas de administracion', async () => {
    useUserRoles.mockReturnValue(['Operador']);
    render(<App />);

    expect(await screen.findByRole('button', { name: 'Catalogo' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Envios' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Mis envios' })).not.toBeInTheDocument();
  });
});
