import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { getToken, clearToken, worldsApi, World, ApiError } from './api/client';
import { LoginPage } from './pages/LoginPage';
import { WorldsPage } from './pages/WorldsPage';
import { WorldView } from './pages/WorldView';
import { SettingsPage } from './pages/SettingsPage';
import { GlobalTemplatesPanel } from './pages/GlobalTemplatesPanel';
import { GlobalStatblocksPanel } from './pages/GlobalStatblocksPanel';
import { GameSystemsPage } from './pages/GameSystemsPage';
import { WorldsNextPage } from './pages/WorldsNextPage';
import { WorldViewNext } from './pages/WorldViewNext';
import { AppSidebar } from './components/AppSidebar';
import { AppSidebarNext } from './components/AppSidebarNext';
import { NextStubPage } from './components/NextStubPage';
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
              <Route path="/" element={<Navigate to="/worlds" replace />} />
              <Route path="/worlds/:worldId/*" element={<WorldViewRoute onAuthExpired={handleLogout} />} />
              <Route path="/next/worlds/:worldId/*" element={<NextWorldViewRoute onAuthExpired={handleLogout} />} />
              <Route path="/next/*" element={<AppShellNext onAuthExpired={handleLogout} />} />
              <Route path="*" element={<AppShell onAuthExpired={handleLogout} />} />
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
function AppShell({ onAuthExpired }: { onAuthExpired: () => void }) {
  return (
    <SidebarProvider className="min-h-0 gap-6 sidebar-shell">
      <AppSidebar />
      <SidebarInset className="app-shell-content">
        <Routes>
          <Route path="/worlds" element={<WorldsPageRoute onAuthExpired={onAuthExpired} />} />
          <Route path="/templates/*" element={<TemplatesPageRoute onAuthExpired={onAuthExpired} />} />
          <Route path="/game-systems" element={<GameSystemsPage onAuthExpired={onAuthExpired} />} />
          <Route path="/settings/*" element={<SettingsPage onAuthExpired={onAuthExpired} />} />
          <Route path="*" element={<Navigate to="/worlds" replace />} />
        </Routes>
      </SidebarInset>
    </SidebarProvider>
  );
}

/**
 * /next shell (docs/ui-overhaul-plan.md Phase 1) — same shape as AppShell,
 * separate sidebar/routes so the old UI stays untouched as a reference
 * throughout the migration.
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
            <Route path="settings" element={<NextStubPage title="Settings" note="Phase 5 — reskin of the existing settings categories." />} />
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
 * reachable directly from AppSidebar (ADR-0098), so this is a bare route
 * switch rather than its own nav shell.
 */
function TemplatesPageRoute({ onAuthExpired }: { onAuthExpired: () => void }) {
  return (
    <Routes>
      <Route index element={<Navigate to="global" replace />} />
      <Route path="global" element={<GlobalTemplatesPanel onAuthExpired={onAuthExpired} />} />
      <Route path="global/:globalTemplateId" element={<GlobalTemplatesPanel onAuthExpired={onAuthExpired} />} />
      <Route path="statblocks" element={<GlobalStatblocksPanel onAuthExpired={onAuthExpired} />} />
      <Route
        path="statblocks/:globalStatblockId"
        element={<GlobalStatblocksPanel onAuthExpired={onAuthExpired} />}
      />
      <Route path="*" element={<Navigate to="global" replace />} />
    </Routes>
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
