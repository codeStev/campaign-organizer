import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';
import {
  getToken,
  setToken,
  clearToken,
  getCurrentAccount,
  worldsApi,
  World,
  Role,
  MfaMethod,
  LoginResponse,
  ApiError,
} from './api/client';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { RecoverPasswordPage } from './pages/RecoverPasswordPage';
import { MfaSetupPage } from './pages/MfaSetupPage';
import { MfaChallengePage } from './pages/MfaChallengePage';
import { NextGlobalTemplatesPanel } from './pages/NextGlobalTemplatesPanel';
import { NextGlobalStatblocksPanel } from './pages/NextGlobalStatblocksPanel';
import { GameSystemsPage } from './pages/GameSystemsPage';
import { WorldsNextPage } from './pages/WorldsNextPage';
import { NextSettingsPage } from './pages/NextSettingsPage';
import { AccountsPage } from './pages/AccountsPage';
import { WorldViewNext } from './pages/WorldViewNext';
import { AppSidebarNext } from './components/AppSidebarNext';
import { NextTopBar } from './components/NextTopBar';
import { ThemeToggle } from './components/ThemeToggle';
import { Button } from './components/ui/button';
import { TooltipProvider } from './components/ui/tooltip';
import { Toaster } from './components/ui/sonner';
import { SidebarInset, SidebarProvider } from './components/ui/sidebar';

/**
 * A correct password never grants full access on its own (ADR-0111, mandatory MFA) — login
 * lands in 'mfa-setup' or 'mfa-challenge' with a PASSWORD-only pending token, never straight
 * into 'authenticated'.
 */
type AuthStage =
  | { kind: 'anonymous' }
  | { kind: 'register' }
  | { kind: 'recover-password' }
  | { kind: 'mfa-setup'; pendingToken: string }
  | { kind: 'mfa-challenge'; pendingToken: string; method: MfaMethod }
  | { kind: 'authenticated' };

export function App() {
  const [stage, setStage] = useState<AuthStage>(() =>
    getToken() !== null ? { kind: 'authenticated' } : { kind: 'anonymous' },
  );
  const [role, setRole] = useState<Role | null>(null);

  useEffect(() => {
    if (stage.kind !== 'authenticated') {
      setRole(null);
      return;
    }
    getCurrentAccount()
      .then((account) => setRole(account.role))
      .catch(() => handleLogout());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stage]);

  function handleLogout() {
    clearToken();
    setStage({ kind: 'anonymous' });
  }

  function handleLoginResult(result: LoginResponse) {
    setStage(
      result.status === 'MFA_SETUP_REQUIRED'
        ? { kind: 'mfa-setup', pendingToken: result.token }
        : { kind: 'mfa-challenge', pendingToken: result.token, method: result.method! },
    );
  }

  function handleAuthenticated(fullToken: string) {
    setToken(fullToken);
    setStage({ kind: 'authenticated' });
  }

  function handleNeedsSetup(pendingToken: string) {
    setStage({ kind: 'mfa-setup', pendingToken });
  }

  return (
    <TooltipProvider>
      <div className="app">
        <header className="app-header">
          <h1>Campaign Organizer</h1>
          <div className="app-header-actions">
            <ThemeToggle />
            {stage.kind === 'authenticated' && (
              <Button variant="link" onClick={handleLogout}>
                Log out
              </Button>
            )}
          </div>
        </header>
        <div className="app-body">
          {stage.kind === 'anonymous' && (
            <LoginPage
              onLoginResult={handleLoginResult}
              onRegister={() => setStage({ kind: 'register' })}
              onForgotPassword={() => setStage({ kind: 'recover-password' })}
            />
          )}
          {stage.kind === 'register' && (
            <RegisterPage onBackToLogin={() => setStage({ kind: 'anonymous' })} />
          )}
          {stage.kind === 'recover-password' && (
            <RecoverPasswordPage onBackToLogin={() => setStage({ kind: 'anonymous' })} />
          )}
          {stage.kind === 'mfa-setup' && (
            <MfaSetupPage pendingToken={stage.pendingToken} onAuthenticated={handleAuthenticated} />
          )}
          {stage.kind === 'mfa-challenge' && (
            <MfaChallengePage
              pendingToken={stage.pendingToken}
              method={stage.method}
              onAuthenticated={handleAuthenticated}
              onNeedsSetup={handleNeedsSetup}
            />
          )}
          {stage.kind === 'authenticated' &&
            (!role ? (
              <p className="muted">Loading…</p>
            ) : (
              <Routes>
                <Route path="/" element={<Navigate to="/next" replace />} />
                <Route
                  path="/next/worlds/:worldId/*"
                  element={<NextWorldViewRoute onAuthExpired={handleLogout} />}
                />
                <Route path="/next/*" element={<AppShellNext onAuthExpired={handleLogout} role={role} />} />
                <Route path="*" element={<Navigate to="/next" replace />} />
              </Routes>
            ))}
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
function AppShellNext({ onAuthExpired, role }: { onAuthExpired: () => void; role: Role }) {
  const location = useLocation();
  const [navOpen, setNavOpen] = useState(false);
  // Tapping a nav link doesn't otherwise close the mobile drawer — the
  // link's own navigation and the drawer's open state are unrelated.
  useEffect(() => {
    setNavOpen(false);
  }, [location.pathname]);

  return (
    <div className="next-shell">
      <NextTopBar onMenuClick={() => setNavOpen(true)} />
      <SidebarProvider className="min-h-0 sidebar-shell-next" openMobile={navOpen} onOpenMobileChange={setNavOpen}>
        <AppSidebarNext role={role} />
        <SidebarInset className="next-shell-content" style={{ alignSelf: 'stretch', height: 'auto' }}>
          <Routes>
            <Route path="worlds" element={<WorldsNextPage onAuthExpired={onAuthExpired} />} />
            <Route path="templates/*" element={<NextTemplatesPageRoute onAuthExpired={onAuthExpired} />} />
            <Route path="game-systems" element={<GameSystemsPage onAuthExpired={onAuthExpired} />} />
            <Route path="settings/*" element={<NextSettingsPage onAuthExpired={onAuthExpired} />} />
            {role === 'ADMIN' && (
              <Route path="accounts" element={<AccountsPage onAuthExpired={onAuthExpired} />} />
            )}
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
