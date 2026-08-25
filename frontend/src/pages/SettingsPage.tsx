import { NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { AiSettingsPanel } from './AiSettingsPanel';
import { Button } from '../components/ui/button';

/** Instance-level settings (FR-39): not nested under any world. Only "AI"
 * exists today; add entries here as more categories arrive. */
const CATEGORIES = [{ key: 'ai', label: 'AI' }];

interface Props {
  onAuthExpired: () => void;
}

export function SettingsPage({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  return (
    <section className="settings-layout">
      <nav className="settings-nav">
        <Button variant="link" onClick={() => navigate('/worlds')}>
          ← Worlds
        </Button>
        {CATEGORIES.map((c) => (
          <NavLink
            key={c.key}
            to={c.key}
            className={({ isActive }) => `settings-nav-item${isActive ? ' active' : ''}`}
          >
            {c.label}
          </NavLink>
        ))}
      </nav>
      <div className="settings-content">
        <Routes>
          <Route index element={<Navigate to="ai" replace />} />
          <Route path="ai" element={<AiSettingsPanel onAuthExpired={onAuthExpired} />} />
          <Route path="*" element={<Navigate to="ai" replace />} />
        </Routes>
      </div>
    </section>
  );
}
