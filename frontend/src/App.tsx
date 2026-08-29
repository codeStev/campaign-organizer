import { useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { getToken, clearToken, worldsApi, World, ApiError } from './api/client';
import { LoginPage } from './pages/LoginPage';
import { WorldsPage } from './pages/WorldsPage';
import { WorldView } from './pages/WorldView';
import { SettingsPage } from './pages/SettingsPage';
import { ThemeToggle } from './components/ThemeToggle';
import { Button } from './components/ui/button';
import { TooltipProvider } from './components/ui/tooltip';
import { Toaster } from './components/ui/sonner';

export function App() {
  const [authed, setAuthed] = useState(() => getToken() !== null);

  function handleLogout() {
    clearToken();
    setAuthed(false);
  }

  return (
    <TooltipProvider>
      <div className="app">
        <header className="app-header">
          <h1>Campaign Organizer</h1>
          <div className="app-header-actions">
            <ThemeToggle />
            {authed && (
              <Button variant="link" asChild>
                <Link to="/settings" title="Settings">
                  ⚙ Settings
                </Link>
              </Button>
            )}
            {authed && (
              <Button variant="link" onClick={handleLogout}>
                Log out
              </Button>
            )}
          </div>
        </header>
        <main>
          {!authed ? (
            <LoginPage onLoggedIn={() => setAuthed(true)} />
          ) : (
            <Routes>
              <Route path="/" element={<Navigate to="/worlds" replace />} />
              <Route path="/worlds" element={<WorldsPageRoute onAuthExpired={handleLogout} />} />
              <Route path="/worlds/:worldId/*" element={<WorldViewRoute onAuthExpired={handleLogout} />} />
              <Route path="/settings/*" element={<SettingsPage onAuthExpired={handleLogout} />} />
              <Route path="*" element={<Navigate to="/worlds" replace />} />
            </Routes>
          )}
        </main>
        <Toaster position="bottom-right" />
      </div>
    </TooltipProvider>
  );
}

function WorldsPageRoute({ onAuthExpired }: { onAuthExpired: () => void }) {
  const navigate = useNavigate();
  return (
    <WorldsPage
      onOpenWorld={(world) => navigate(`/worlds/${world.id}`)}
      onAuthExpired={onAuthExpired}
    />
  );
}

/** Resolves :worldId to a World (deep links only carry the id) before rendering WorldView. */
function WorldViewRoute({ onAuthExpired }: { onAuthExpired: () => void }) {
  const { worldId } = useParams<{ worldId: string }>();
  const navigate = useNavigate();
  const [world, setWorld] = useState<World | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!worldId) return;
    setWorld(null);
    setNotFound(false);
    worldsApi
      .get(worldId)
      .then(setWorld)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          onAuthExpired();
          return;
        }
        setNotFound(true);
      });
  }, [worldId, onAuthExpired]);

  if (!worldId || notFound) return <Navigate to="/worlds" replace />;
  if (!world) return <p className="muted">Loading…</p>;

  return (
    <WorldView
      worldId={world.id}
      worldName={world.name}
      onBack={() => navigate('/worlds')}
      onAuthExpired={onAuthExpired}
    />
  );
}
