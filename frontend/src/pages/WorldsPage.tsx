import { ChangeEvent, FormEvent, useEffect, useRef, useState } from 'react';
import { worldsApi, downloadBackup, importBackup, World, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { toast } from 'sonner';

interface Props {
  onOpenWorld: (world: World) => void;
  onAuthExpired: () => void;
}

export function WorldsPage({ onOpenWorld, onAuthExpired }: Props) {
  const [worlds, setWorlds] = useState<World[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [backingUp, setBackingUp] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importing, setImporting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

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
      toast.success(`World "${name}" created`);
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

  async function handleBackup() {
    setBackingUp(true);
    try {
      await downloadBackup();
    } catch (err) {
      handleError(err);
    } finally {
      setBackingUp(false);
    }
  }

  function handleImportFileChosen(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setImportFile(file);
    event.target.value = '';
  }

  async function runImport(mode: 'ADDITIVE' | 'OVERWRITE') {
    if (!importFile) return;
    setImporting(true);
    setError(null);
    try {
      await importBackup(importFile, mode);
      setImportFile(null);
      await refresh();
    } catch (err) {
      handleError(err);
    } finally {
      setImporting(false);
    }
  }

  function handleImportAdditive() {
    void runImport('ADDITIVE');
  }

  function handleImportOverwrite() {
    const consequence =
      worlds.length === 0
        ? 'There are no existing worlds to lose, but this cannot be undone.'
        : `This deletes all ${worlds.length} existing world(s) first. This cannot be undone.`;
    if (!window.confirm(`Replace everything with this backup? ${consequence}`)) return;
    void runImport('OVERWRITE');
  }

  return (
    <section>
      <form className="card" onSubmit={handleCreate}>
        <h2>New world</h2>
        <label htmlFor="name">Name</label>
        <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
        <label htmlFor="description">Description</label>
        <Textarea
          id="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <Button type="submit" disabled={name.length === 0}>
          Create world
        </Button>
      </form>

      {error && <p className="error">{error}</p>}

      <div className="worlds-head">
        <h2>Worlds</h2>
        <Button variant="link" onClick={handleBackup} disabled={backingUp} title="Download a full backup (every world + media)">
          {backingUp ? 'Backing up…' : '⬇ Backup'}
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".zip"
          style={{ display: 'none' }}
          onChange={handleImportFileChosen}
        />
        <Button
          variant="link"
          onClick={() => fileInputRef.current?.click()}
          disabled={importing}
          title="Import a backup ZIP"
        >
          Import
        </Button>
      </div>

      {importFile && (
        <div className="card">
          <p>
            Import <strong>{importFile.name}</strong> as:
          </p>
          <Button onClick={handleImportAdditive} disabled={importing}>
            {importing ? 'Importing…' : 'Add as new'}
          </Button>
          <Button
            variant="link"
            className="text-destructive hover:text-destructive"
            onClick={handleImportOverwrite}
            disabled={importing}
          >
            Replace everything
          </Button>
          <Button variant="link" onClick={() => setImportFile(null)} disabled={importing}>
            Cancel
          </Button>
        </div>
      )}

      {loading ? (
        <p>Loading…</p>
      ) : worlds.length === 0 ? (
        <p className="muted">No worlds yet. Create your first one above.</p>
      ) : (
        <ul className="world-list">
          {worlds.map((world) => (
            <li key={world.id} className="card world-item">
              <button className="world-open" onClick={() => onOpenWorld(world)}>
                <strong>{world.name}</strong>
                {world.description && <p className="muted">{world.description}</p>}
              </button>
              <Button variant="link" className="text-destructive hover:text-destructive" onClick={() => handleDelete(world.id)}>
                Delete
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
