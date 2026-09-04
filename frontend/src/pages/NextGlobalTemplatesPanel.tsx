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
import { Button } from '../components/ui/button';
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
 * World-independent, system-scoped template catalog (ADR-0093, FR-55) —
 * /next's own fork of GlobalTemplatesPanel (ADR-0106): same
 * TemplateBuilder/TemplateForm/API logic, but a persistent picker sidebar +
 * main pane instead of the old single-card view swapping its entire content
 * for the builder/preview, matching /next's .wiki-layout convention used
 * everywhere else a screen has no world/category context to hang a
 * <CategoryTree> off of.
 */
export function NextGlobalTemplatesPanel({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { globalTemplateId: urlTemplateId } = useParams<{ globalTemplateId: string }>();
  const [templates, setTemplates] = useState<GlobalFieldTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [builtins, setBuiltins] = useState<BuiltinFieldTemplate[]>([]);
  const [systems, setSystems] = useState<GameSystem[]>([]);
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
    gameSystemsApi.list().then(setSystems).catch(onError);
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
      navigate('/next/templates/global');
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

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        {error && <p className="error">{error}</p>}
        <p className="muted hint">
          A system-scoped catalog shared across every world and campaign — build a game system&apos;s
          character sheet or statblock once here instead of per world.
        </p>

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
            className="sidebar-new-button"
            size="sm"
            onClick={() => {
              setEditing(null);
              setPreviewing(null);
              setBuilding(true);
              navigate('/next/templates/global');
            }}
          >
            + Build new
          </Button>
        </div>

        <ul className="article-list">
          {templates.map((t) => (
            <li key={t.id} className="rel-row">
              <Button
                variant="link"
                className="template-open"
                onClick={() => navigate(`/next/templates/global/${t.id}`)}
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
      </aside>

      <div className="wiki-main">
        {building && (
          <TemplateBuilder
            initial={editing ? { ...editing, worldId: '' } : null}
            kind={editing?.kind ?? newKind}
            onSave={saveTemplate}
            onCancel={() => {
              setBuilding(false);
              setEditing(null);
              if (previewing) navigate(`/next/templates/global/${previewing.id}`);
              else navigate('/next/templates/global');
            }}
          />
        )}

        {!building && previewing && (
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
                    navigate('/next/templates/global');
                  }}
                >
                  Back to list
                </Button>
              </div>
            </div>
            <TemplateForm sections={previewing.sections} values={{}} onChange={() => {}} readOnly />
          </div>
        )}

        {!building && !previewing && <p className="muted">Select a template from the sidebar, or build one.</p>}
      </div>
    </div>
  );
}
