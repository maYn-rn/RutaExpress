import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('@azure/msal-react', () => ({
  useMsal: () => ({ instance: { loginRedirect: jest.fn(), logoutRedirect: jest.fn() }, accounts: [] }),
  AuthenticatedTemplate: () => null,
  UnauthenticatedTemplate: ({ children }) => children,
}));

test('muestra el boton de inicio de sesion cuando no hay usuario autenticado', () => {
  render(<App />);
  expect(screen.getByRole('button', { name: /iniciar sesion/i })).toBeInTheDocument();
});
