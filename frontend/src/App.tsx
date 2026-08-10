import { useState } from 'react';
import { getToken, clearToken } from './api/client';
import { LoginPage } from './pages/LoginPage';
import { WorldsPage } from './pages/WorldsPage';

export function App() {
  const [authed, setAuthed] = useState(() => getToken() !== null);

  function handleLogout() {
    clearToken();
    setAuthed(false);
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Campaign Organizer</h1>
        {authed && (
          <button className="link-button" onClick={handleLogout}>
            Log out
          </button>
        )}
      </header>
      <main>
        {authed ? (
          <WorldsPage onAuthExpired={handleLogout} />
        ) : (
          <LoginPage onLoggedIn={() => setAuthed(true)} />
        )}
      </main>
    </div>
  );
}
