import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  statblocksApi,
  statblockTagsApi,
  worldTagsApi,
  gameSystemsApi,
  globalStatblocksApi,
  Statblock,
  Campaign,
  FieldTemplate,
  GlobalFieldTemplate,
  GlobalStatblock,
  GameSystem,
  FieldType,
} from '../api/client';
import { StatblockCardsView } from './StatblockCardsView';
import { EncounterSheetView } from './EncounterSheetView';
import { TemplateForm } from '../components/TemplateForm';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { TagInput, TagList } from '../components/TagInput';
import { Button } from '../components/ui/button';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { Spinner } from '../components/ui/spinner';
import { toast } from 'sonner';
import { renderMarkdown } from '../lib/markdown';

// Radix Select can't use "" as an item value (it's reserved for "no selection"),
// so options that mean a real, persistently-selectable "none" state (as opposed
// to a not-yet-chosen placeholder) go through this sentinel at the Select boundary.
const NONE_VALUE = '__none__';

interface Props {
  worldId: string;
  templates: FieldTemplate[];
  globalTemplates: GlobalFieldTemplate[];
  campaigns: Campaign[];
  onChanged: () => void;
  onError: (err: unknown) => void;
}

interface StatRow {
  key: string;
  value: string;
}

interface Draft {
  id: string | null;
  categoryId: string | null;
  name: string;
  notes: string;
  campaignId: string;
  worldTemplateId: string;
  globalTemplateId: string;
  rows: StatRow[];
  tags: string[];
}

const EMPTY: Draft = {
  id: null,
  categoryId: null,
  name: '',
  notes: '',
  campaignId: '',
  worldTemplateId: '',
  globalTemplateId: '',
  rows: [{ key: '', value: '' }],
  tags: [],
};

// Encodes which catalog a picked template comes from into the Select's value,
// since the id alone doesn't say whether to set worldTemplateId or globalTemplateId (ADR-0093).
const WORLD_PREFIX = 'world:';
const GLOBAL_PREFIX = 'global:';

function toRows(stats: Record<string, unknown>): StatRow[] {
  const rows = Object.entries(stats).map(([key, value]) => ({ key, value: String(value) }));
  return rows.length ? rows : [{ key: '', value: '' }];
}

/** Field types keyed by field key, for a template (empty map when none chosen). */
function fieldTypesOf(template: { sections: FieldTemplate['sections'] } | null): Map<string, FieldType> {
  const types = new Map<string, FieldType>();
  if (!template) return types;
  for (const section of template.sections) {
    for (const field of section.fields) types.set(field.key, field.type);
  }
  return types;
}

/** Parses a row's string value into the type a template field expects. */
function coerceRowValue(type: FieldType | undefined, raw: string): unknown {
  if (type === 'NUMBER' || type === 'CIRCLES') {
    const num = Number(raw);
    return raw !== '' && !Number.isNaN(num) ? num : null;
  }
  if (type === 'BOOLEAN') return raw === 'true';
  return raw;
}

export function StatblocksPanel({ worldId, templates, globalTemplates, campaigns, onChanged, onError }: Props) {
  const navigate = useNavigate();
  const { statblockId: urlStatblockId } = useParams<{ statblockId: string }>();
  const api = useMemo(() => statblocksApi(worldId), [worldId]);
  const [list, setList] = useState<Statblock[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [savedTags, setSavedTags] = useState<string[]>([]);
  const [worldTags, setWorldTags] = useState<string[]>([]);
  const [filterCampaign, setFilterCampaign] = useState('');
  const [filterTag, setFilterTag] = useState('');
  const [cardsOpen, setCardsOpen] = useState(false);
  // FR-44: encounter-sheet builder over the same selection as card printing.
  const [encounterOpen, setEncounterOpen] = useState(false);
  // Ids ticked for printing; empty = print the whole (filtered) list.
  const [selected, setSelected] = useState<Set<string>>(new Set());
  // Read (rendered values) vs edit (the form) — mirrors WorldView's article mode.
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  const [systems, setSystems] = useState<GameSystem[]>([]);
  const [catalog, setCatalog] = useState<GlobalStatblock[]>([]);
  const [importOpen, setImportOpen] = useState(false);
  const [importCatalogId, setImportCatalogId] = useState('');
  const [importCampaignId, setImportCampaignId] = useState('');
  // Statblocks-only bulk select for print/encounter (separate from picking one to open).
  const [bulkOpen, setBulkOpen] = useState(false);

  useEffect(() => {
    gameSystemsApi.list().then(setSystems).catch(() => {});
    globalStatblocksApi.list().then(setCatalog).catch(() => {});
  }, []);

  async function importFromCatalog() {
    if (!importCatalogId || !importCampaignId) return;
    try {
      const copy = await globalStatblocksApi.import(importCatalogId, {
        worldId,
        campaignId: importCampaignId,
      });
      setImportOpen(false);
      setImportCatalogId('');
      await refresh();
      onChanged();
      navigate(urlStatblockId ? `../${copy.id}` : copy.id, { relative: 'path' });
      toast.success(`Imported "${copy.name}" into the campaign`);
    } catch (err) {
      onError(err);
    }
  }

  function systemName(systemId: string): string {
    return systems.find((s) => s.id === systemId)?.name ?? '(unknown system)';
  }

  function systemColor(systemId: string): string | null {
    return systems.find((s) => s.id === systemId)?.color ?? null;
  }

  const statblockTemplates = templates.filter((t) => t.kind === 'STATBLOCK');
  const globalStatblockTemplates = globalTemplates.filter((t) => t.kind === 'STATBLOCK');
  const template =
    statblockTemplates.find((t) => t.id === draft.worldTemplateId) ??
    globalStatblockTemplates.find((t) => t.id === draft.globalTemplateId) ??
    null;
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

  function toggleSelected(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  // Only count/print selections that are actually in the current list.
  const toPrint = selected.size ? list.filter((s) => selected.has(s.id)) : list;

  const refresh = useCallback(async () => {
    try {
      setList(await api.list({ campaignId: filterCampaign || undefined, tag: filterTag || undefined }));
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, filterCampaign, filterTag, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const loadWorldTags = useCallback(() => {
    worldTagsApi(worldId).list().then(setWorldTags).catch(onError);
  }, [worldId, onError]);

  useEffect(() => {
    loadWorldTags();
  }, [loadWorldTags]);

  function edit(sb: Statblock, tags: string[] = []) {
    setDraft({
      id: sb.id,
      categoryId: sb.categoryId ?? null,
      name: sb.name,
      notes: sb.notes ?? '',
      campaignId: sb.campaignId ?? '',
      worldTemplateId: sb.worldTemplateId ?? '',
      globalTemplateId: sb.globalTemplateId ?? '',
      rows: toRows(sb.stats),
      tags,
    });
    setSavedTags(tags);
    setMode('read');
  }

  function newStatblock() {
    setDraft({ ...EMPTY, campaignId: filterCampaign });
    setSavedTags([]);
    setMode('edit');
    navigate(urlStatblockId ? '..' : '.', { relative: 'path' });
  }

  // The URL is the source of truth for which statblock is open (ADR-0053);
  // "new" is a sentinel the sidebar's "+ New statblock" button navigates to.
  useEffect(() => {
    if (!urlStatblockId || urlStatblockId === draft.id) return;
    if (urlStatblockId === 'new') {
      newStatblock();
      return;
    }
    Promise.all([api.get(urlStatblockId), statblockTagsApi(worldId, urlStatblockId).get()])
      .then(([sb, t]) => edit(sb, t.tags))
      .catch(onError);
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
      categoryId: draft.categoryId,
      name: draft.name,
      stats,
      notes: draft.notes || null,
      campaignId: draft.campaignId || null,
      worldTemplateId: draft.worldTemplateId || null,
      globalTemplateId: draft.globalTemplateId || null,
    };
    const wasNew = !draft.id;
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      const tagResult = await statblockTagsApi(worldId, saved.id).set(draft.tags);
      edit(saved, tagResult.tags);
      if (wasNew) navigate(saved.id);
      await refresh();
      onChanged();
      loadWorldTags();
      toast.success(`Statblock "${body.name}" saved`);
    } catch (err) {
      onError(err);
    }
  }

  async function duplicate(sb: Statblock) {
    try {
      const copy = await api.duplicate(sb.id);
      await refresh();
      onChanged();
      navigate(`../${copy.id}`, { relative: 'path' });
      toast.success(`Statblock "${copy.name}" created`);
    } catch (err) {
      onError(err);
    }
  }

  async function remove(sb: Statblock) {
    try {
      await api.remove(sb.id);
      if (draft.id === sb.id) {
        setDraft(EMPTY);
        setSavedTags([]);
        navigate('..', { relative: 'path' });
      }
      await refresh();
      onChanged();
    } catch (err) {
      onError(err);
    }
  }

  function setRow(i: number, patch: Partial<StatRow>) {
    setDraft((d) => ({ ...d, rows: d.rows.map((r, j) => (j === i ? { ...r, ...patch } : r)) }));
  }

  return (
    <div className="sheet-detail">
      <div className="editor-actions">
        <Button variant="outline" onClick={() => setBulkOpen((v) => !v)}>
          {bulkOpen ? 'Hide bulk select' : '☑ Bulk select (print / encounter)'}
        </Button>
        {campaigns.length > 0 && catalog.length > 0 && (
          <Button variant="link" onClick={() => setImportOpen((v) => !v)}>
            Import from catalog
          </Button>
        )}
      </div>
      {importOpen && (
          <div className="import-catalog-picker">
            <Select value={importCatalogId || NONE_VALUE} onValueChange={(v) => setImportCatalogId(v === NONE_VALUE ? '' : v)}>
              <SelectTrigger>
                <SelectValue placeholder="Choose a statblock…" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE_VALUE} disabled>
                  Choose a statblock…
                </SelectItem>
                {catalog.map((s) => (
                  <SelectItem key={s.id} value={s.id}>
                    {s.name} · {systemName(s.systemId)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={importCampaignId || NONE_VALUE}
              onValueChange={(v) => setImportCampaignId(v === NONE_VALUE ? '' : v)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Into which campaign…" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE_VALUE} disabled>
                  Into which campaign…
                </SelectItem>
                {campaigns.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <div className="editor-actions">
              <Button
                type="button"
                onClick={() => void importFromCatalog()}
                disabled={!importCatalogId || !importCampaignId}
              >
                Import
              </Button>
              <Button type="button" variant="link" onClick={() => setImportOpen(false)}>
                Cancel
              </Button>
            </div>
          </div>
        )}
      {bulkOpen && (
        <div className="card statblock-bulk-select">
          <div className="editor-actions">
            {campaigns.length > 0 && (
              <Select
                value={filterCampaign || NONE_VALUE}
                onValueChange={(v) => setFilterCampaign(v === NONE_VALUE ? '' : v)}
              >
                <SelectTrigger title="Filter by campaign">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NONE_VALUE}>All campaigns</SelectItem>
                  {campaigns.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
            {worldTags.length > 0 && (
              <Select value={filterTag || NONE_VALUE} onValueChange={(v) => setFilterTag(v === NONE_VALUE ? '' : v)}>
                <SelectTrigger title="Filter by tag">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NONE_VALUE}>All tags</SelectItem>
                  {worldTags.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
          {list.length > 0 && (
            <div className="statblock-print-bar">
              <Button
                variant="link"
                onClick={() => setCardsOpen(true)}
                title={
                  selected.size
                    ? 'Print the ticked statblocks as cut-out cards'
                    : 'Print all listed statblocks as cut-out cards'
                }
              >
                🖨 Print {selected.size ? `${selected.size} card${selected.size > 1 ? 's' : ''}` : 'cards'}
              </Button>
              <Button
                variant="link"
                onClick={() => setEncounterOpen(true)}
                title={
                  selected.size
                    ? 'Build a combat tracker from the ticked statblocks'
                    : 'Build a combat tracker from all listed statblocks'
                }
              >
                ⚔ Encounter
              </Button>
              {selected.size > 0 && (
                <Button variant="link" onClick={() => setSelected(new Set())}>
                  Clear
                </Button>
              )}
            </div>
          )}
          <ul className="article-list">
            {list.map((sb) => (
              <li key={sb.id} className="statblock-row">
                <Checkbox
                  className="statblock-check"
                  checked={selected.has(sb.id)}
                  onCheckedChange={() => toggleSelected(sb.id)}
                  title="Select for printing"
                />
                <button
                  className={sb.id === draft.id ? 'article-link active' : 'article-link'}
                  onClick={() => navigate(urlStatblockId ? `../${sb.id}` : sb.id, { relative: 'path' })}
                >
                  <TruncatedLabel label={sb.name}>{sb.name}</TruncatedLabel>
                </button>
              </li>
            ))}
            {loading && (
              <li className="muted loading-row">
                <Spinner /> Loading…
              </li>
            )}
            {!loading && list.length === 0 && <li className="muted">No statblocks yet.</li>}
          </ul>
        </div>
      )}

      <div className="card">
        {(mode === 'edit' || !draft.id) && (
          <>
            <Input
              className="title-input"
              placeholder="Statblock name (e.g. Goblin)"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
            {campaigns.length > 0 && (
              <label className="sheet-article">
                <span className="muted">Campaign</span>
                <Select
                  value={draft.campaignId || NONE_VALUE}
                  onValueChange={(v) => setDraft({ ...draft, campaignId: v === NONE_VALUE ? '' : v })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NONE_VALUE}>— shared (no campaign) —</SelectItem>
                    {campaigns.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </label>
            )}
            <label className="sheet-article">
              <span className="muted">Template</span>
              <Select
                value={
                  draft.worldTemplateId
                    ? `${WORLD_PREFIX}${draft.worldTemplateId}`
                    : draft.globalTemplateId
                      ? `${GLOBAL_PREFIX}${draft.globalTemplateId}`
                      : NONE_VALUE
                }
                onValueChange={(v) => {
                  if (v === NONE_VALUE) {
                    setDraft({ ...draft, worldTemplateId: '', globalTemplateId: '' });
                  } else if (v.startsWith(WORLD_PREFIX)) {
                    setDraft({ ...draft, worldTemplateId: v.slice(WORLD_PREFIX.length), globalTemplateId: '' });
                  } else {
                    setDraft({ ...draft, globalTemplateId: v.slice(GLOBAL_PREFIX.length), worldTemplateId: '' });
                  }
                }}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NONE_VALUE}>None — free-form</SelectItem>
                  {statblockTemplates.length > 0 && (
                    <SelectGroup>
                      <SelectLabel>This world</SelectLabel>
                      {statblockTemplates.map((t) => (
                        <SelectItem key={t.id} value={`${WORLD_PREFIX}${t.id}`}>
                          {t.name}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  )}
                  {globalStatblockTemplates.length > 0 && (
                    <SelectGroup>
                      <SelectLabel>Global (system) templates</SelectLabel>
                      {globalStatblockTemplates.map((t) => (
                        <SelectItem key={t.id} value={`${GLOBAL_PREFIX}${t.id}`}>
                          {systemColor(t.systemId) && (
                            <span className="system-color-dot" style={{ backgroundColor: systemColor(t.systemId)! }} />
                          )}
                          {t.name} · {systemName(t.systemId)}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  )}
                </SelectContent>
              </Select>
            </label>

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
            <TagInput worldId={worldId} value={draft.tags} onChange={(tags) => setDraft({ ...draft, tags })} />
            <MarkdownEditor value={draft.notes} onChange={(notes) => setDraft({ ...draft, notes })} />
            <div className="editor-actions">
              <Button onClick={save} disabled={!draft.name}>
                {draft.id ? 'Save statblock' : 'Create statblock'}
              </Button>
              {draft.id && (
                <Button
                  type="button"
                  variant="link"
                  onClick={() => {
                    const saved = list.find((s) => s.id === draft.id);
                    if (saved) edit(saved, savedTags);
                    else setMode('read');
                  }}
                >
                  Cancel
                </Button>
              )}
              {draft.id && (
                <Button
                  type="button"
                  variant="link"
                  onClick={() => duplicate(list.find((s) => s.id === draft.id)!)}
                >
                  Duplicate
                </Button>
              )}
              {draft.id && (
                <ConfirmDeleteDialog
                  trigger={
                    <Button variant="link" className="text-destructive hover:text-destructive">
                      Delete
                    </Button>
                  }
                  title="Delete statblock?"
                  description={`This permanently deletes "${draft.name}" and cannot be undone.`}
                  onConfirm={() => remove(list.find((s) => s.id === draft.id)!)}
                />
              )}
            </div>
          </>
        )}
        {mode === 'read' && draft.id && (
          <article className="article-read">
            <div className="article-read-head">
              <h2>{draft.name}</h2>
              <Button type="button" onClick={() => setMode('edit')}>
                Edit
              </Button>
            </div>
            <TagList worldId={worldId} tags={draft.tags} />
            {draft.campaignId && campaigns.length > 0 && (
              <p className="muted">
                Campaign: {campaigns.find((c) => c.id === draft.campaignId)?.name ?? '—'}
              </p>
            )}
            {template && <p className="muted">Template: {template.name}</p>}

            {template && (
              <TemplateForm sections={template.sections} values={templateValues} onChange={() => {}} readOnly />
            )}

            {(template ? otherRows : draft.rows.map((row, index) => ({ row, index }))).filter(({ row }) =>
              row.key.trim(),
            ).length > 0 && (
              <>
                <strong className="muted">{template ? 'Other stats' : 'Stats'}</strong>
                <dl className="print-stats">
                  {(template ? otherRows : draft.rows.map((row, index) => ({ row, index })))
                    .filter(({ row }) => row.key.trim())
                    .map(({ row, index }) => (
                      <div key={index} className="print-stat">
                        <dt>{row.key}</dt>
                        <dd>{row.value || '—'}</dd>
                      </div>
                    ))}
                </dl>
              </>
            )}

            {draft.notes ? (
              <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(draft.notes) }} />
            ) : (
              <p className="muted">(no notes)</p>
            )}
          </article>
        )}
      </div>

      {cardsOpen && (
        <StatblockCardsView
          statblocks={toPrint}
          templates={statblockTemplates}
          globalTemplates={globalStatblockTemplates}
          title={
            selected.size
              ? `${toPrint.length} selected`
              : filterCampaign
                ? campaigns.find((c) => c.id === filterCampaign)?.name ?? ''
                : 'All statblocks'
          }
          onClose={() => setCardsOpen(false)}
        />
      )}

      {encounterOpen && (
        <EncounterSheetView
          worldId={worldId}
          statblocks={toPrint}
          templates={statblockTemplates}
          globalTemplates={globalStatblockTemplates}
          onClose={() => setEncounterOpen(false)}
        />
      )}
    </div>
  );
}
