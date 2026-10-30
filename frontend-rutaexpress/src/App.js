import { useState } from 'react';
import './App.css';
import { AuthenticatedTemplate, UnauthenticatedTemplate, useMsal } from '@azure/msal-react';
import { loginRequest } from './authConfig';
import ServiceList from './components/ServiceList';
import ShipmentList from './components/ShipmentList';
import SessionInfo from './components/SessionInfo';
import NotificationBell from './components/NotificationBell';
import { NotificationsProvider } from './notifications/NotificationsContext';
import { useUserRoles } from './auth/useUserRoles';
import { WRITE_ROLES, hasAnyRole } from './auth/roles';
import CustomerShipments from './components/CustomerShipments';

const TABS = {
  CATALOG: 'catalog',
  SHIPMENTS: 'shipments',
  SESSION: 'session',
};

function App() {
  const { instance, accounts } = useMsal();
  const roles = useUserRoles();
  const [activeTab, setActiveTab] = useState(TABS.CATALOG);
  // Igual que el BFF (CustomerScope): solo Admin y Operador administran;
  // cualquier otro usuario es un cliente y solo ve sus envios.
  const isStaff = hasAnyRole(roles, WRITE_ROLES);

  const handleLogin = () => {
    instance.loginRedirect(loginRequest).catch((error) => {
      console.error('Error al iniciar sesion:', error);
    });
  };

  const handleSignUp = () => {
    instance
      .loginRedirect({
        ...loginRequest,
        extraQueryParameters: { prompt: 'create' },
      })
      .catch((error) => {
        console.error('Error al registrarse:', error);
      });
  };

  const handleLogout = () => {
    instance.logoutRedirect().catch((error) => {
      console.error('Error al cerrar sesion:', error);
    });
  };

  return (
    <div className="App">
      <header className="App-header">
        <h1>RutaExpress</h1>

        <UnauthenticatedTemplate>
          <p>Debes iniciar sesion para ver el catalogo de servicios.</p>
          <button type="button" onClick={handleLogin}>
            Iniciar sesion
          </button>
          <p className="auth-secondary-action">
            ¿No tienes cuenta?{' '}
            <button type="button" className="link-button" onClick={handleSignUp}>
              Registrate
            </button>
          </p>
        </UnauthenticatedTemplate>

        <AuthenticatedTemplate>
          <NotificationsProvider>
            <div className="App-user-bar">
              <span>Hola, {accounts[0]?.name}</span>
              {isStaff &&
                roles.map((role) => (
                  <span key={role} className="chip">
                    {role}
                  </span>
                ))}
              <NotificationBell />
              <button type="button" onClick={handleLogout}>
                Cerrar sesion
              </button>
            </div>

            {isStaff ? (
              <>
                <nav className="app-tabs">
                  <button
                    type="button"
                    className={activeTab === TABS.CATALOG ? 'tab active' : 'tab'}
                    onClick={() => setActiveTab(TABS.CATALOG)}
                  >
                    Catalogo
                  </button>
                  <button
                    type="button"
                    className={activeTab === TABS.SHIPMENTS ? 'tab active' : 'tab'}
                    onClick={() => setActiveTab(TABS.SHIPMENTS)}
                  >
                    Envios
                  </button>
                  <button
                    type="button"
                    className={activeTab === TABS.SESSION ? 'tab active' : 'tab'}
                    onClick={() => setActiveTab(TABS.SESSION)}
                  >
                    Mi sesion
                  </button>
                </nav>

                {activeTab === TABS.CATALOG && <ServiceList />}
                {activeTab === TABS.SHIPMENTS && <ShipmentList />}
                {activeTab === TABS.SESSION && <SessionInfo />}
              </>
            ) : (
              <CustomerShipments email={accounts[0]?.username} />
            )}
          </NotificationsProvider>
        </AuthenticatedTemplate>
      </header>
    </div>
  );
}

export default App;
