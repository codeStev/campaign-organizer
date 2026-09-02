import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { worldsApi, World, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Spinner } from '../components/ui/spinner';

interface Props {
  onAuthExpired: () => void;
}

/**
 * Minimal world picker for /next (docs/ui-overhaul-plan.md Phase 1) — just
 * enough to reach WorldViewNext. Full world management (create/backup/
 * import/delete) still lives on the old /worlds page; link back there for
 * now rather than duplicating that CRUD before it has a real /next design.
 */
export function WorldsNextPage({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  const [worlds, setWorlds] = useState<World[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    worldsApi
      .list()
      .then(setWorlds)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) return onAuthExpired();
        setError(err instanceof Error ? err.message : 'Something went wrong');
      })
      .finally(() => setLoading(false));
  }, [onAuthExpired]);

  return (
    <div className="card">
      <div className="editor-actions">
        <h1 style={{ flex: 1 }}>Worlds</h1>
        <Button variant="link" onClick={() => navigate('/worlds')}>
          Manage worlds (old UI) →
        </Button>
      </div>
      {error && <p className="error">{error}</p>}
      <ul className="article-list">
        {worlds.map((w) => (
          <li key={w.id} className="rel-row">
            <Button
              variant="link"
              className="world-open"
              onClick={() => navigate(`/next/worlds/${w.id}`)}
            >
              <strong>{w.name}</strong>
            </Button>
          </li>
        ))}
        {loading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!loading && worlds.length === 0 && <li className="muted">No worlds yet.</li>}
      </ul>
    </div>
  );
}
