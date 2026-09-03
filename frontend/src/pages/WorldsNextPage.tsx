import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { worldsApi, ApiError } from '../api/client';
import { Button } from '../components/ui/button';

interface Props {
  onAuthExpired: () => void;
}

/**
 * /next landing content (docs/ui-overhaul-plan.md Phase 1) — world picking
 * itself now lives in the header's NextWorldSwitcher popover, matching the
 * reviewed mockup; this just prompts toward it instead of duplicating the
 * list. Full world management (create/backup/import/delete) still lives on
 * the old /worlds page.
 */
export function WorldsNextPage({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  const [hasWorlds, setHasWorlds] = useState<boolean | null>(null);

  useEffect(() => {
    worldsApi
      .list()
      .then((worlds) => setHasWorlds(worlds.length > 0))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) onAuthExpired();
      });
  }, [onAuthExpired]);

  return (
    <div className="card">
      <h1>Worlds</h1>
      <p className="muted">
        {hasWorlds === false
          ? 'No worlds yet — create one on the old worlds page.'
          : 'Pick a world from the switcher above, top-left.'}
      </p>
      <Button variant="link" onClick={() => navigate('/worlds')}>
        Manage worlds (old UI) →
      </Button>
    </div>
  );
}
