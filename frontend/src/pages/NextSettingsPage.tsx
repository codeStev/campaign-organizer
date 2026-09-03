import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { AiSettingsPanel } from './AiSettingsPanel';
import { Button } from '../components/ui/button';

interface Props {
  onAuthExpired: () => void;
}

/**
 * Settings (docs/ui-overhaul-plan.md Phase 5) — reskin of the old UI's
 * category-nav layout. Only "AI" is a real settings category today (FR-39);
 * the plan's mockup-driven "backup/access" categories don't correspond to
 * anything this app actually persists as a setting — backup/import is a
 * one-shot world action, not configurable state, and there's no user-facing
 * access/password setting to reskin (ADR-0006: one env-configured password).
 * A "Backup & Import" entry links to where that flow already lives instead
 * of fabricating a section with nothing to configure.
 */
export function NextSettingsPage({ onAuthExpired }: Props) {
  return (
    <section className="settings-layout">
      <nav className="settings-nav">
        <NavLink to="ai" className={({ isActive }) => `settings-nav-item${isActive ? ' active' : ''}`}>
          AI
        </NavLink>
      </nav>
      <div className="settings-content">
        <Routes>
          <Route index element={<Navigate to="ai" replace />} />
          <Route path="ai" element={<AiSettingsPanel onAuthExpired={onAuthExpired} />} />
          <Route path="*" element={<Navigate to="ai" replace />} />
        </Routes>
        <p className="muted">
          Backup and restore a world from the{' '}
          <Button variant="link" asChild>
            <NavLink to="/worlds">worlds list (current UI) →</NavLink>
          </Button>
        </p>
      </div>
    </section>
  );
}
