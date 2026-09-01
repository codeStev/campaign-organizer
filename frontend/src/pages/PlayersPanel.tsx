import { KeyboardEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { playersApi, Player, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { toast } from 'sonner';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

/** FR-53: a world-scoped, reusable pool of players (name only), shared across campaigns. */
export function PlayersPanel({ worldId, onAuthExpired }: Props) {
  const api = useMemo(() => playersApi(worldId), [worldId]);
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  // Player id currently being renamed inline (null = none).
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState('');

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(async () => {
    try {
      setPlayers(await api.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function addPlayer() {
    const trimmed = name.trim();
    if (!trimmed) return;
    try {
      await api.create({ name: trimmed });
      setName('');
      await refresh();
      toast.success(`Player "${trimmed}" added`);
    } catch (err) {
      handleError(err);
    }
  }

  function handleAddKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      e.preventDefault();
      void addPlayer();
    }
  }

  function startRename(p: Player) {
    setRenamingId(p.id);
    setRenameValue(p.name);
  }

  async function saveRename(p: Player) {
    const trimmed = renameValue.trim();
    if (!trimmed || trimmed === p.name) {
      setRenamingId(null);
      return;
    }
    try {
      await api.update(p.id, { name: trimmed });
      setRenamingId(null);
      await refresh();
      toast.success('Player renamed');
    } catch (err) {
      handleError(err);
    }
  }

  function handleRenameKeyDown(e: KeyboardEvent<HTMLInputElement>, p: Player) {
    if (e.key === 'Enter') {
      e.preventDefault();
      void saveRename(p);
    } else if (e.key === 'Escape') {
      setRenamingId(null);
    }
  }

  async function remove(p: Player) {
    try {
      await api.remove(p.id);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="card">
      <h3>Players</h3>
      <p className="muted hint">
        A reusable pool of the people you play with. Add someone here once, then add them to a
        campaign&apos;s roster to track attendance.
      </p>
      {error && <p className="error">{error}</p>}

      <div className="editor-actions">
        <Input
          placeholder="Player name (Enter to add)"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={handleAddKeyDown}
        />
        <Button type="button" disabled={!name.trim()} onClick={() => void addPlayer()}>
          + Add player
        </Button>
      </div>

      <ul className="article-list">
        {players.map((p) => (
          <li key={p.id} className="rel-row">
            {renamingId === p.id ? (
              <Input
                autoFocus
                value={renameValue}
                onChange={(e) => setRenameValue(e.target.value)}
                onKeyDown={(e) => handleRenameKeyDown(e, p)}
                onBlur={() => void saveRename(p)}
              />
            ) : (
              <Button variant="link" className="template-open" onClick={() => startRename(p)}>
                <strong>{p.name}</strong>
              </Button>
            )}
            <ConfirmDeleteDialog
              trigger={
                <Button variant="link" className="text-destructive hover:text-destructive">
                  ✕
                </Button>
              }
              title="Delete player?"
              description={`This removes "${p.name}" from every campaign roster and their entire attendance history. This cannot be undone.`}
              onConfirm={() => remove(p)}
            />
          </li>
        ))}
        {loading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!loading && players.length === 0 && <li className="muted">No players yet.</li>}
      </ul>
    </div>
  );
}
