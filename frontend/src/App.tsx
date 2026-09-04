import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { getToken, clearToken, worldsApi, World, ApiError } from './api/client';
import { LoginPage } from './pages/LoginPage';
import { NextGlobalTemplatesPanel } from './pages/NextGlobalTemplatesPanel';
import { NextGlobalStatblocksPanel } from './pages/NextGlobalStatblocksPanel';
import { GameSystemsPage } from './pages/GameSystemsPage';
import { WorldsNextPage } from './pages/WorldsNextPage';
import { NextSettingsPage } from './pages/NextSettingsPage';
import { WorldViewNext } from './pages/WorldViewNext';
import { AppSidebarNext } from './components/AppSidebarNext';
import { NextTopBar } from './components/NextTopBar';
import { ThemeToggle } from './components/ThemeToggle';
import { Button } from './components/ui/button';
import { TooltipProvider } from './components/ui/tooltip';
import { Toaster } from './components/ui/sonner';
import { SidebarInset, SidebarProvider } from './components/ui/sidebar';

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
              <Button variant="link" onClick={handleLogout}>
                Log out
              </Button>
            )}
          </div>
        </header>
        <div className="app-body">
          {!authed ? (
            <LoginPage onLoggedIn={() => setAuthed(true)} />
          ) : (
            <Routes>
              <Route path="/" element={<Navigate to="/next" replace />} />
              <Route path="/next/worlds/:worldId/*" element={<NextWorldViewRoute onAuthExpired={handleLogout} />} />
              <Route path="/next/*" element={<AppShellNext onAuthExpired={handleLogout} />} />
              <Route path="*" element={<Navigate to="/next" replace />} />
            </Routes>
          )}
        </div>
        <Toaster position="bottom-right" />
      </div>
    </TooltipProvider>
  );
}

/**
 * Shell for every top-level, world-independent route (ADR-0098): a
 * persistent sidebar beside the routed content. A World takes over the
 * screen with its own in-world sidebar instead of nesting under this one.
 */
function AppShellNext({ onAuthExpired }: { onAuthExpired: () => void }) {
  return (
    <div className="next-shell">
      <NextTopBar />
      <SidebarProvider className="min-h-0 sidebar-shell-next">
        <AppSidebarNext />
        <SidebarInset className="next-shell-content" style={{ alignSelf: 'stretch', height: 'auto' }}>
          <Routes>
            <Route path="worlds" element={<WorldsNextPage onAuthExpired={onAuthExpired} />} />
            <Route path="templates/*" element={<NextTemplatesPageRoute onAuthExpired={onAuthExpired} />} />
            <Route path="game-systems" element={<GameSystemsPage onAuthExpired={onAuthExpired} />} />
            <Route path="settings/*" element={<NextSettingsPage onAuthExpired={onAuthExpired} />} />
            <Route path="*" element={<Navigate to="/next/worlds" replace />} />
          </Routes>
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}

/** Resolves :worldId to a World, then renders the /next world shell. */
function NextWorldViewRoute({ onAuthExpired }: { onAuthExpired: () => void }) {
  const { worldId } = useParams<{ worldId: string }>();
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

  if (!worldId || notFound) return <Navigate to="/next/worlds" replace />;
  if (!world) return <p className="muted">Loading…</p>;

  return <WorldViewNext worldId={world.id} worldName={world.name} onAuthExpired={onAuthExpired} />;
}

/**
 * World-independent global catalogs — not nested under any world. Templates
 * (ADR-0093) and statblocks (ADR-0096) are separate sub-pages; both are
 * reachable directly from AppSidebarNext (ADR-0098), so this is a bare
 * route switch rather than its own nav shell.
 */
function NextTemplatesPageRoute({ onAuthExpired }: { onAuthExpired: () => void }) {
  return (
    <Routes>
      <Route index element={<Navigate to="global" replace />} />
      <Route path="global" element={<NextGlobalTemplatesPanel onAuthExpired={onAuthExpired} />} />
      <Route path="global/:globalTemplateId" element={<NextGlobalTemplatesPanel onAuthExpired={onAuthExpired} />} />
      <Route path="statblocks" element={<NextGlobalStatblocksPanel onAuthExpired={onAuthExpired} />} />
      <Route
        path="statblocks/:globalStatblockId"
        element={<NextGlobalStatblocksPanel onAuthExpired={onAuthExpired} />}
      />
      <Route path="*" element={<Navigate to="global" replace />} />
    </Routes>
  );
}
