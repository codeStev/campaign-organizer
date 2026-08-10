import { useState } from 'react';
import { getToken, clearToken, World } from './api/client';
import { LoginPage } from './pages/LoginPage';
import { WorldsPage } from './pages/WorldsPage';
import { WorldView } from './pages/WorldView';

export function App() {
  const [authed, setAuthed] = useState(() => getToken() !== null);
  const [world, setWorld] = useState<World | null>(null);

  function handleLogout() {
    clearToken();
    setWorld(null);
    setAuthed(false);
  }

  function renderMain() {
    if (!authed) {
      return <LoginPage onLoggedIn={() => setAuthed(true)} />;
    }
    if (world) {
      return (
        <WorldView
          worldId={world.id}
          worldName={world.name}
          onBack={() => setWorld(null)}
          onAuthExpired={handleLogout}
        />
      );
    }
    return <WorldsPage onOpenWorld={setWorld} onAuthExpired={handleLogout} />;
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
      <main>{renderMain()}</main>
    </div>
  );
}
