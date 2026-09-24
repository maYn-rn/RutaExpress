import { useState } from 'react';
import './App.css';
import { AuthenticatedTemplate, UnauthenticatedTemplate, useMsal } from '@azure/msal-react';
import { loginRequest } from './authConfig';
import ServiceList from './components/ServiceList';
import ShipmentList from './components/ShipmentList';
import SessionInfo from './components/SessionInfo';
import { useUserRoles } from './auth/useUserRoles';

const TABS = {
  CATALOG: 'catalog',
  SHIPMENTS: 'shipments',
  SESSION: 'session',
};

function App() {
  const { instance, accounts } = useMsal();
  const roles = useUserRoles();
  const [activeTab, setActiveTab] = useState(TABS.CATALOG);

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
          <div className="App-user-bar">
            <span>Hola, {accounts[0]?.name}</span>
            {roles.length > 0 ? (
              roles.map((role) => (
                <span key={role} className="chip">
                  {role}
                </span>
              ))
            ) : (
              <span className="chip muted-chip">Solo lectura</span>
            )}
            <button type="button" onClick={handleLogout}>
              Cerrar sesion
            </button>
          </div>

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
        </AuthenticatedTemplate>
      </header>
    </div>
  );
}

export default App;
