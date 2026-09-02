import { useCallback, useEffect, useState } from 'react';
import { gameSystemsApi, GameSystem, ApiError } from '../api/client';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Spinner } from '../components/ui/spinner';

interface Props {
  onAuthExpired: () => void;
}

/**
 * Top-level, world-independent game system catalog (ADR-0094, ADR-0095).
 * Promoted to its own top-level page (ADR-0098) — GameSystem outgrew being
 * "the thing templates are keyed by" once campaigns could link to one too.
 */
export function GameSystemsPage({ onAuthExpired }: Props) {
  const [systems, setSystems] = useState<GameSystem[]>([]);
  const [systemsLoading, setSystemsLoading] = useState(true);
  const [newSystemName, setNewSystemName] = useState('');
  const [editingSystemId, setEditingSystemId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editTagline, setEditTagline] = useState('');
  const [editColor, setEditColor] = useState('');
  const [editNotes, setEditNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const onError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refreshSystems = useCallback(() => {
    gameSystemsApi
      .list()
      .then(setSystems)
      .catch(onError)
      .finally(() => setSystemsLoading(false));
  }, [onError]);

  useEffect(() => {
    refreshSystems();
  }, [refreshSystems]);

  async function addSystem() {
    const name = newSystemName.trim();
    if (!name) return;
    try {
      const created = await gameSystemsApi.create({ name });
      setSystems((s) => [...s, created]);
      setNewSystemName('');
      toast.success(`Game system "${created.name}" added`);
    } catch (err) {
      onError(err);
    }
  }

  function startEditSystem(system: GameSystem) {
    setEditingSystemId(system.id);
    setEditName(system.name);
    setEditTagline(system.tagline ?? '');
    setEditColor(system.color ?? '');
    setEditNotes(system.notes ?? '');
  }

  async function saveEditSystem(system: GameSystem) {
    const trimmed = editName.trim();
    if (!trimmed) return;
    try {
      const updated = await gameSystemsApi.update(system.id, {
        name: trimmed,
        tagline: editTagline.trim() || null,
        color: editColor.trim() || null,
        notes: editNotes.trim() || null,
      });
      setSystems((s) => s.map((x) => (x.id === updated.id ? updated : x)));
      setEditingSystemId(null);
      toast.success(`Game system "${updated.name}" saved`);
    } catch (err) {
      onError(err);
    }
  }

  async function removeSystem(system: GameSystem) {
    try {
      await gameSystemsApi.remove(system.id);
      refreshSystems();
    } catch (err) {
      onError(err);
    }
  }

  return (
    <section className="card">
      <h3>Game systems</h3>
      <p className="muted hint">
        A top-level, world-independent list of the systems your templates, statblocks, and campaigns
        are keyed by (ADR-0094, ADR-0095).
      </p>
      {error && <p className="error">{error}</p>}
      <div className="editor-actions">
        <Input
          placeholder="New game system name…"
          value={newSystemName}
          onChange={(e) => setNewSystemName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              void addSystem();
            }
          }}
        />
        <Button type="button" disabled={!newSystemName.trim()} onClick={() => void addSystem()}>
          Add
        </Button>
      </div>
      <ul className="article-list">
        {systems.map((s) =>
          editingSystemId === s.id ? (
            <li key={s.id} className="game-system-edit">
              <div className="editor-actions">
                <Input placeholder="Name" value={editName} onChange={(e) => setEditName(e.target.value)} />
                <Input
                  placeholder="Tagline (optional)"
                  value={editTagline}
                  onChange={(e) => setEditTagline(e.target.value)}
                />
                <input
                  type="color"
                  value={editColor || '#888888'}
                  onChange={(e) => setEditColor(e.target.value)}
                  title="Badge color"
                />
              </div>
              <MarkdownEditor value={editNotes} onChange={setEditNotes} />
              <div className="editor-actions">
                <Button type="button" disabled={!editName.trim()} onClick={() => void saveEditSystem(s)}>
                  Save
                </Button>
                <Button type="button" variant="link" onClick={() => setEditingSystemId(null)}>
                  Cancel
                </Button>
              </div>
            </li>
          ) : (
            <li key={s.id} className="rel-row">
              {s.color && (
                <span className="system-color-dot" style={{ backgroundColor: s.color }} title={s.name} />
              )}
              <span>
                <strong>{s.name}</strong> {s.tagline && <small className="muted">— {s.tagline}</small>}
              </span>
              <Button type="button" variant="link" onClick={() => startEditSystem(s)}>
                ✎ Edit
              </Button>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    ✕
                  </Button>
                }
                title="Delete game system?"
                description={`This deletes "${s.name}". Blocked if any global template still uses it; world-scoped templates just lose the reference.`}
                onConfirm={() => removeSystem(s)}
              />
            </li>
          ),
        )}
        {systemsLoading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!systemsLoading && systems.length === 0 && <li className="muted">No game systems yet.</li>}
      </ul>
    </section>
  );
}
