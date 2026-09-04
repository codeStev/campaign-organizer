import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { worldsApi, World, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Checkbox } from '../components/ui/checkbox';
import { Badge } from '../components/ui/badge';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { toast } from 'sonner';

interface Props {
  onAuthExpired: () => void;
}

/**
 * /next's own world management (ADR-0106) — create/delete/browse every
 * world. Ported from the deleted WorldsPage.tsx now that old UI is retired;
 * backup/import already lived on NextSettingsPage (whole-instance, not
 * per-world), so this only needed the create-form + list + delete piece.
 * Picking a world to open still goes through the header's world switcher
 * (NextWorldSwitcher), matching the reviewed mockup — this list exists for
 * create/delete/browse, opening a world here is a convenience, not the only way in.
 */
export function WorldsNextPage({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  const [worlds, setWorlds] = useState<World[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [scratch, setScratch] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh() {
    setLoading(true);
    try {
      setWorlds(await worldsApi.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }

  function handleError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) {
      onAuthExpired();
      return;
    }
    setError(err instanceof Error ? err.message : 'Something went wrong');
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    try {
      const created = await worldsApi.create({ name, description: description || undefined, scratch });
      setName('');
      setDescription('');
      setScratch(false);
      await refresh();
      toast.success(`World "${created.name}" created`);
    } catch (err) {
      handleError(err);
    }
  }

  async function handleDelete(id: string) {
    try {
      await worldsApi.remove(id);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <section>
      <form className="card" onSubmit={handleCreate}>
        <h2>New world</h2>
        <label htmlFor="name">Name</label>
        <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required data-testid="new-world-name" />
        <label htmlFor="description">Description</label>
        <Textarea id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
        <label className="layer-toggle" htmlFor="scratch">
          <Checkbox
            id="scratch"
            checked={scratch}
            onCheckedChange={(checked) => setScratch(checked === true)}
            data-testid="new-world-scratch"
          />
          Scratch world (brainstorming sandbox, separate from your real worlds)
        </label>
        <Button type="submit" disabled={name.length === 0} data-testid="create-world-submit">
          Create world
        </Button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="worlds-head">
        <h2>Worlds</h2>
      </div>

      {loading ? (
        <p className="muted loading-row">
          <Spinner /> Loading…
        </p>
      ) : worlds.length === 0 ? (
        <p className="muted">No worlds yet. Create your first one above.</p>
      ) : (
        <ul className="world-list">
          {worlds.map((world) => (
            <li key={world.id} className="card world-item">
              <button
                className="world-open"
                onClick={() => navigate(`/next/worlds/${world.id}`)}
                data-testid="world-open"
                data-world-name={world.name}
              >
                <strong>{world.name}</strong>
                {world.scratch && <Badge variant="secondary">Scratch</Badge>}
                {world.description && <p className="muted">{world.description}</p>}
              </button>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    Delete
                  </Button>
                }
                title="Delete world?"
                description={`This permanently deletes "${world.name}" — every article, map, campaign, and everything else in it. This cannot be undone.`}
                onConfirm={() => handleDelete(world.id)}
              />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
