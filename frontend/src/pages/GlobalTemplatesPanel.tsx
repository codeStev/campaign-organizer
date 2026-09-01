import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  globalFieldTemplatesApi,
  builtinFieldTemplatesApi,
  gameSystemsApi,
  GlobalFieldTemplate,
  GlobalFieldTemplateRequest,
  FieldTemplateRequest,
  BuiltinFieldTemplate,
  GameSystem,
  ApiError,
} from '../api/client';
import { TemplateBuilder } from '../components/TemplateBuilder';
import { TemplateForm } from '../components/TemplateForm';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Spinner } from '../components/ui/spinner';

interface Props {
  onAuthExpired: () => void;
}

const KIND_LABEL: Record<'CHARACTER' | 'STATBLOCK', string> = {
  CHARACTER: 'Character sheet',
  STATBLOCK: 'Statblock',
};

/**
 * World-independent, system-scoped template catalog (ADR-0093, FR-55).
 * CHARACTER/STATBLOCK kinds only; reused across every world/campaign that
 * shares a game system instead of being rebuilt per world.
 */
export function GlobalTemplatesPanel({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { globalTemplateId: urlTemplateId } = useParams<{ globalTemplateId: string }>();
  const [templates, setTemplates] = useState<GlobalFieldTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [builtins, setBuiltins] = useState<BuiltinFieldTemplate[]>([]);
  const [systems, setSystems] = useState<GameSystem[]>([]);
  const [systemsLoading, setSystemsLoading] = useState(true);
  const [newSystemName, setNewSystemName] = useState('');
  const [editingSystemId, setEditingSystemId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editTagline, setEditTagline] = useState('');
  const [editColor, setEditColor] = useState('');
  const [editNotes, setEditNotes] = useState('');
  const [newKind, setNewKind] = useState<'CHARACTER' | 'STATBLOCK'>('CHARACTER');
  const [choice, setChoice] = useState('');
  const [editing, setEditing] = useState<GlobalFieldTemplate | null>(null);
  const [building, setBuilding] = useState(false);
  const [previewing, setPreviewing] = useState<GlobalFieldTemplate | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(() => {
    globalFieldTemplatesApi
      .list()
      .then(setTemplates)
      .catch(onError)
      .finally(() => setLoading(false));
  }, [onError]);

  const refreshSystems = useCallback(() => {
    gameSystemsApi
      .list()
      .then(setSystems)
      .catch(onError)
      .finally(() => setSystemsLoading(false));
  }, [onError]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    refreshSystems();
  }, [refreshSystems]);

  useEffect(() => {
    builtinFieldTemplatesApi.list().then(setBuiltins).catch(onError);
  }, [onError]);

  function systemName(systemId: string): string {
    return systems.find((s) => s.id === systemId)?.name ?? '(unknown system)';
  }

  function systemColor(systemId: string): string | null {
    return systems.find((s) => s.id === systemId)?.color ?? null;
  }

  /** Finds a game system by exact case-insensitive name, creating one if none matches. */
  async function resolveSystemId(name: string): Promise<string> {
    const existing = systems.find((s) => s.name.toLowerCase() === name.toLowerCase());
    if (existing) return existing.id;
    const created = await gameSystemsApi.create({ name });
    setSystems((s) => [...s, created]);
    return created.id;
  }

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

  useEffect(() => {
    if (!urlTemplateId || urlTemplateId === editing?.id || urlTemplateId === previewing?.id) return;
    const found = templates.find((t) => t.id === urlTemplateId);
    if (found) setPreviewing(found);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlTemplateId, templates]);

  const buildersForKind = builtins.filter((b) => b.kind === newKind);

  async function addFromBuiltin() {
    const b = buildersForKind.find((x) => x.name === choice);
    if (!b) return;
    if (!b.system) {
      onError(new Error(`"${b.name}" has no system label and can't be added to the global catalog.`));
      return;
    }
    try {
      const systemId = await resolveSystemId(b.system);
      const already = templates.find(
        (t) => t.kind === b.kind && t.systemId === systemId && t.name === b.name,
      );
      if (already) {
        toast.info(`"${b.name}" is already in the global catalog`);
        setChoice('');
        return;
      }
      await globalFieldTemplatesApi.create({ name: b.name, kind: b.kind, systemId, sections: b.sections });
      setChoice('');
      refresh();
      toast.success(`"${b.name}" added to the global catalog`);
    } catch (err) {
      onError(err);
    }
  }

  // TemplateBuilder/TemplateForm are template-content-agnostic (ADR-0093) — reused
  // as-is via a FieldTemplateRequest adapter, since a system is required here.
  async function saveTemplate(body: FieldTemplateRequest) {
    if (!body.systemId) {
      onError(new Error('A game system is required for a global template.'));
      return;
    }
    const request: GlobalFieldTemplateRequest = {
      name: body.name,
      kind: body.kind,
      systemId: body.systemId,
      sections: body.sections,
    };
    try {
      if (editing) await globalFieldTemplatesApi.update(editing.id, request);
      else await globalFieldTemplatesApi.create(request);
      setBuilding(false);
      setEditing(null);
      setPreviewing(null);
      navigate('/templates/global');
      refresh();
      toast.success('Global template saved');
    } catch (err) {
      onError(err);
    }
  }

  async function remove(t: GlobalFieldTemplate) {
    try {
      await globalFieldTemplatesApi.remove(t.id);
      refresh();
    } catch (err) {
      onError(err);
    }
  }

  if (building) {
    return (
      <TemplateBuilder
        initial={editing ? { ...editing, worldId: '' } : null}
        kind={editing?.kind ?? newKind}
        onSave={saveTemplate}
        onCancel={() => {
          setBuilding(false);
          setEditing(null);
          if (previewing) navigate(`/templates/global/${previewing.id}`);
          else navigate('/templates/global');
        }}
      />
    );
  }

  if (previewing) {
    return (
      <div className="card template-preview">
        <div className="article-read-head">
          <div>
            <h3>{previewing.name}</h3>
            <small className="muted">
              {KIND_LABEL[previewing.kind as 'CHARACTER' | 'STATBLOCK']} ·{' '}
              {systemColor(previewing.systemId) && (
                <span
                  className="system-color-dot"
                  style={{ backgroundColor: systemColor(previewing.systemId)! }}
                />
              )}
              {systemName(previewing.systemId)} · {previewing.sections.length} sections
            </small>
          </div>
          <div className="editor-actions">
            <Button
              type="button"
              onClick={() => {
                setEditing(previewing);
                setBuilding(true);
              }}
            >
              Edit
            </Button>
            <Button
              type="button"
              variant="link"
              onClick={() => {
                setPreviewing(null);
                navigate('/templates/global');
              }}
            >
              Back to list
            </Button>
          </div>
        </div>
        <TemplateForm sections={previewing.sections} values={{}} onChange={() => {}} readOnly />
      </div>
    );
  }

  return (
    <>
      <section className="card">
        <h3>Game systems</h3>
        <p className="muted hint">
          A top-level, world-independent list of the systems your templates are keyed by (ADR-0094).
        </p>
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
                  <Input
                    placeholder="Name"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                  />
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
                  <strong>{s.name}</strong>{' '}
                  {s.tagline && <small className="muted">— {s.tagline}</small>}
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

      <div className="card">
        <h3>Global templates</h3>
        <p className="muted hint">
          A system-scoped catalog shared across every world and campaign — build a game system&apos;s
          character sheet or statblock once here instead of per world.
        </p>
        {error && <p className="error">{error}</p>}

        <div className="editor-actions">
          <label className="muted">
            What are you building?{' '}
            <Select
              value={newKind}
              onValueChange={(v) => {
                setNewKind(v as 'CHARACTER' | 'STATBLOCK');
                setChoice('');
              }}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CHARACTER">Character sheet</SelectItem>
                <SelectItem value="STATBLOCK">Statblock</SelectItem>
              </SelectContent>
            </Select>
          </label>
          <Select value={choice} onValueChange={setChoice}>
            <SelectTrigger>
              <SelectValue placeholder="Starter system…" />
            </SelectTrigger>
            <SelectContent>
              {buildersForKind.map((b) => (
                <SelectItem key={b.name} value={b.name}>
                  {b.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button onClick={addFromBuiltin} disabled={!choice}>
            Add starter
          </Button>
          <Button
            onClick={() => {
              setEditing(null);
              setPreviewing(null);
              setBuilding(true);
            }}
          >
            Build new
          </Button>
        </div>

        <ul className="article-list">
          {templates.map((t) => (
            <li key={t.id} className="rel-row">
              <Button
                variant="link"
                className="template-open"
                onClick={() => navigate(`/templates/global/${t.id}`)}
              >
                <strong>{t.name}</strong>{' '}
                <small className="muted">
                  {KIND_LABEL[t.kind as 'CHARACTER' | 'STATBLOCK']} ·{' '}
                  {systemColor(t.systemId) && (
                    <span className="system-color-dot" style={{ backgroundColor: systemColor(t.systemId)! }} />
                  )}
                  {systemName(t.systemId)} · {t.sections.length} sections
                </small>
              </Button>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    ✕
                  </Button>
                }
                title="Delete global template?"
                description={`This deletes "${t.name}" from the shared catalog. Blocked if any character sheet or statblock in any world still uses it.`}
                onConfirm={() => remove(t)}
              />
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && templates.length === 0 && (
            <li className="muted">No global templates yet. Add a starter or build one.</li>
          )}
        </ul>
      </div>
    </>
  );
}
