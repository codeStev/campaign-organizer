import { FormEvent, useEffect, useState } from 'react';
import { worldsApi, World, ApiError } from '../api/client';

interface Props {
  onAuthExpired: () => void;
}

export function WorldsPage({ onAuthExpired }: Props) {
  const [worlds, setWorlds] = useState<World[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
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

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    try {
      await worldsApi.create({ name, description: description || undefined });
      setName('');
      setDescription('');
      await refresh();
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

  function handleError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) {
      onAuthExpired();
      return;
    }
    setError(err instanceof Error ? err.message : 'Something went wrong');
  }

  return (
    <section>
      <form className="card" onSubmit={handleCreate}>
        <h2>New world</h2>
        <label htmlFor="name">Name</label>
        <input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
        <label htmlFor="description">Description</label>
        <textarea
          id="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <button type="submit" disabled={name.length === 0}>
          Create world
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <h2>Worlds</h2>
      {loading ? (
        <p>Loading…</p>
      ) : worlds.length === 0 ? (
        <p className="muted">No worlds yet. Create your first one above.</p>
      ) : (
        <ul className="world-list">
          {worlds.map((world) => (
            <li key={world.id} className="card world-item">
              <div>
                <strong>{world.name}</strong>
                {world.description && <p className="muted">{world.description}</p>}
              </div>
              <button className="link-button danger" onClick={() => handleDelete(world.id)}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
