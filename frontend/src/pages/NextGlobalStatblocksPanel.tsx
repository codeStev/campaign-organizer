import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  globalStatblocksApi,
  globalFieldTemplatesApi,
  gameSystemsApi,
  GlobalStatblock,
  GlobalFieldTemplate,
  GameSystem,
  FieldType,
  ApiError,
} from '../api/client';
import { TemplateForm } from '../components/TemplateForm';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Spinner } from '../components/ui/spinner';
import { MobileBackButton } from '../components/MobileBackButton';
import { toast } from 'sonner';

const NONE_VALUE = '__none__';

interface Props {
  onAuthExpired: () => void;
}

interface StatRow {
  key: string;
  value: string;
}

interface Draft {
  id: string | null;
  name: string;
  systemId: string;
  globalTemplateId: string;
  notes: string;
  rows: StatRow[];
}

const EMPTY: Draft = {
  id: null,
  name: '',
  systemId: '',
  globalTemplateId: '',
  notes: '',
  rows: [{ key: '', value: '' }],
};

function toRows(stats: Record<string, unknown>): StatRow[] {
  const rows = Object.entries(stats).map(([key, value]) => ({ key, value: String(value) }));
  return rows.length ? rows : [{ key: '', value: '' }];
}

function fieldTypesOf(template: { sections: GlobalFieldTemplate['sections'] } | null): Map<string, FieldType> {
  const types = new Map<string, FieldType>();
  if (!template) return types;
  for (const section of template.sections) {
    for (const field of section.fields) types.set(field.key, field.type);
  }
  return types;
}

function coerceRowValue(type: FieldType | undefined, raw: string): unknown {
  if (type === 'NUMBER' || type === 'CIRCLES') {
    const num = Number(raw);
    return raw !== '' && !Number.isNaN(num) ? num : null;
  }
  if (type === 'BOOLEAN') return raw === 'true';
  return raw;
}

/**
 * World-independent, system-scoped statblock catalog (ADR-0096) — /next's
 * own fork of GlobalStatblocksPanel (ADR-0106): same API/CRUD logic, but
 * the old `.sheets-panel`/`.sheets-list-col` wrapper is swapped for /next's
 * `.wiki-layout`/`.wiki-sidebar`/`.wiki-main` picker shape (no world/
 * category context here, so no <CategoryTree>, same as
 * NextGlobalTemplatesPanel). Real monster/NPC instances, reused across
 * worlds by explicit copy-on-import into a campaign (that action lives on
 * the world-scoped NextStatblocksPanel, which has world/campaign context
 * this page doesn't).
 */
export function NextGlobalStatblocksPanel({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { globalStatblockId: urlStatblockId } = useParams<{ globalStatblockId: string }>();
  const [list, setList] = useState<GlobalStatblock[]>([]);
  const [loading, setLoading] = useState(true);
  const [systems, setSystems] = useState<GameSystem[]>([]);
  const [templates, setTemplates] = useState<GlobalFieldTemplate[]>([]);
  const [filterSystem, setFilterSystem] = useState('');
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(() => {
    globalStatblocksApi
      .list(filterSystem || undefined)
      .then(setList)
      .catch(onError)
      .finally(() => setLoading(false));
  }, [filterSystem, onError]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    gameSystemsApi.list().then(setSystems).catch(onError);
    globalFieldTemplatesApi.list('STATBLOCK').then(setTemplates).catch(onError);
  }, [onError]);

  function systemName(systemId: string): string {
    return systems.find((s) => s.id === systemId)?.name ?? '(unknown system)';
  }

  function systemColor(systemId: string): string | null {
    return systems.find((s) => s.id === systemId)?.color ?? null;
  }

  const templatesForSystem = templates.filter((t) => t.systemId === draft.systemId);
  const template = templatesForSystem.find((t) => t.id === draft.globalTemplateId) ?? null;
  const fieldTypes = fieldTypesOf(template);
  const templateValues: Record<string, unknown> = {};
  const otherRows: { row: StatRow; index: number }[] = [];
  draft.rows.forEach((row, index) => {
    if (fieldTypes.has(row.key)) templateValues[row.key] = coerceRowValue(fieldTypes.get(row.key), row.value);
    else otherRows.push({ row, index });
  });

  function setTemplateValues(values: Record<string, unknown>) {
    setDraft((d) => {
      const byKey = new Map(d.rows.map((r) => [r.key, r]));
      for (const [key, value] of Object.entries(values)) {
        byKey.set(key, { key, value: value === null || value === undefined ? '' : String(value) });
      }
      return { ...d, rows: Array.from(byKey.values()) };
    });
  }

  function setRow(i: number, patch: Partial<StatRow>) {
    setDraft((d) => ({ ...d, rows: d.rows.map((r, j) => (j === i ? { ...r, ...patch } : r)) }));
  }

  function edit(s: GlobalStatblock) {
    setDraft({
      id: s.id,
      name: s.name,
      systemId: s.systemId,
      globalTemplateId: s.globalTemplateId ?? '',
      notes: s.notes ?? '',
      rows: toRows(s.stats),
    });
    setEditing(true);
  }

  // The URL is the source of truth for which catalog entry is open (ADR-0053).
  useEffect(() => {
    if (!urlStatblockId || urlStatblockId === draft.id) return;
    globalStatblocksApi.get(urlStatblockId).then(edit).catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlStatblockId]);

  async function save() {
    const stats: Record<string, unknown> = {};
    for (const r of draft.rows) {
      const key = r.key.trim();
      if (!key) continue;
      if (fieldTypes.has(key)) {
        stats[key] = coerceRowValue(fieldTypes.get(key), r.value);
      } else {
        const num = Number(r.value);
        stats[key] = r.value !== '' && !Number.isNaN(num) ? num : r.value;
      }
    }
    const body = {
      name: draft.name,
      systemId: draft.systemId,
      globalTemplateId: draft.globalTemplateId || null,
      stats,
      notes: draft.notes || null,
    };
    try {
      const saved = draft.id
        ? await globalStatblocksApi.update(draft.id, body)
        : await globalStatblocksApi.create(body);
      edit(saved);
      navigate(`/next/templates/statblocks/${saved.id}`);
      refresh();
      toast.success(`"${saved.name}" saved`);
    } catch (err) {
      onError(err);
    }
  }

  async function remove(s: GlobalStatblock) {
    try {
      await globalStatblocksApi.remove(s.id);
      if (draft.id === s.id) {
        setDraft(EMPTY);
        setEditing(false);
        navigate('/next/templates/statblocks');
      }
      refresh();
    } catch (err) {
      onError(err);
    }
  }

  return (
    <div className="wiki-layout" data-has-selection={editing}>
      <aside className="wiki-sidebar">
        <Button
          className="sidebar-new-button"
          size="sm"
          onClick={() => {
            setDraft({ ...EMPTY, systemId: filterSystem });
            setEditing(true);
            navigate('/next/templates/statblocks');
          }}
        >
          + New statblock
        </Button>
        {systems.length > 0 && (
          <Select value={filterSystem || NONE_VALUE} onValueChange={(v) => setFilterSystem(v === NONE_VALUE ? '' : v)}>
            <SelectTrigger>
              <SelectValue placeholder="Filter by system" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE_VALUE}>All systems</SelectItem>
              {systems.map((s) => (
                <SelectItem key={s.id} value={s.id}>
                  {s.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
        {error && <p className="error">{error}</p>}
        <ul className="article-list">
          {list.map((s) => (
            <li key={s.id} className="rel-row">
              <Button
                variant="link"
                className="template-open"
                onClick={() => navigate(`/next/templates/statblocks/${s.id}`)}
              >
                {systemColor(s.systemId) && (
                  <span className="system-color-dot" style={{ backgroundColor: systemColor(s.systemId)! }} />
                )}
                <strong>{s.name}</strong> <small className="muted">{systemName(s.systemId)}</small>
              </Button>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    ✕
                  </Button>
                }
                title="Delete global statblock?"
                description={`This deletes "${s.name}" from the shared catalog. Already-imported copies in campaigns are unaffected.`}
                onConfirm={() => remove(s)}
              />
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && list.length === 0 && <li className="muted">No global statblocks yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        <MobileBackButton />
        <div className="sheet-detail card">
          {editing && (
            <>
              <Input
                className="title-input"
                placeholder="Statblock name (e.g. Adult Red Dragon)"
                value={draft.name}
                onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              />
              <label className="sheet-article">
                <span className="muted">Game system</span>
                <Select
                  value={draft.systemId || NONE_VALUE}
                  onValueChange={(v) =>
                    setDraft({ ...draft, systemId: v === NONE_VALUE ? '' : v, globalTemplateId: '' })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Choose a system…" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NONE_VALUE} disabled>
                      Choose a system…
                    </SelectItem>
                    {systems.map((s) => (
                      <SelectItem key={s.id} value={s.id}>
                        {s.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </label>
              {templatesForSystem.length > 0 && (
                <label className="sheet-article">
                  <span className="muted">Template</span>
                  <Select
                    value={draft.globalTemplateId || NONE_VALUE}
                    onValueChange={(v) => setDraft({ ...draft, globalTemplateId: v === NONE_VALUE ? '' : v })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE_VALUE}>None — free-form</SelectItem>
                      {templatesForSystem.map((t) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </label>
              )}

              {template && (
                <TemplateForm sections={template.sections} values={templateValues} onChange={setTemplateValues} />
              )}

              <strong className="muted">{template ? 'Other stats' : 'Stats'}</strong>
              {(template ? otherRows : draft.rows.map((row, index) => ({ row, index }))).map(({ row, index }) => (
                <div key={index} className="month-row">
                  <Input
                    placeholder="stat (AC)"
                    value={row.key}
                    onChange={(e) => setRow(index, { key: e.target.value })}
                  />
                  <Input
                    placeholder="value (15)"
                    value={row.value}
                    onChange={(e) => setRow(index, { value: e.target.value })}
                  />
                  <Button
                    type="button"
                    variant="link"
                    className="text-destructive hover:text-destructive"
                    onClick={() => setDraft((d) => ({ ...d, rows: d.rows.filter((_, j) => j !== index) }))}
                  >
                    ✕
                  </Button>
                </div>
              ))}
              <Button
                type="button"
                variant="link"
                onClick={() => setDraft((d) => ({ ...d, rows: [...d.rows, { key: '', value: '' }] }))}
              >
                + Add stat
              </Button>
              <MarkdownEditor value={draft.notes} onChange={(notes) => setDraft({ ...draft, notes })} />
              <div className="editor-actions">
                <Button onClick={save} disabled={!draft.name || !draft.systemId}>
                  {draft.id ? 'Save statblock' : 'Create statblock'}
                </Button>
                <Button
                  type="button"
                  variant="link"
                  onClick={() => {
                    const saved = list.find((s) => s.id === draft.id);
                    if (saved) edit(saved);
                    else {
                      setDraft(EMPTY);
                      setEditing(false);
                      navigate('/next/templates/statblocks');
                    }
                  }}
                >
                  Cancel
                </Button>
              </div>
            </>
          )}
          {!editing && <p className="muted">Select a global statblock or create a new one.</p>}
        </div>
      </div>
    </div>
  );
}
