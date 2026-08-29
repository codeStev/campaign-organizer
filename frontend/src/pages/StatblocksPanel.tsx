import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { statblocksApi, Statblock, Campaign, FieldTemplate, FieldType } from '../api/client';
import { StatblockCardsView } from './StatblockCardsView';
import { EncounterSheetView } from './EncounterSheetView';
import { TemplateForm } from '../components/TemplateForm';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { Button } from '../components/ui/button';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { Spinner } from '../components/ui/spinner';
import { toast } from 'sonner';

// Radix Select can't use "" as an item value (it's reserved for "no selection"),
// so options that mean a real, persistently-selectable "none" state (as opposed
// to a not-yet-chosen placeholder) go through this sentinel at the Select boundary.
const NONE_VALUE = '__none__';

interface Props {
  worldId: string;
  templates: FieldTemplate[];
  campaigns: Campaign[];
  onError: (err: unknown) => void;
}

interface StatRow {
  key: string;
  value: string;
}

interface Draft {
  id: string | null;
  name: string;
  notes: string;
  campaignId: string;
  templateId: string;
  rows: StatRow[];
}

const EMPTY: Draft = {
  id: null,
  name: '',
  notes: '',
  campaignId: '',
  templateId: '',
  rows: [{ key: '', value: '' }],
};

function toRows(stats: Record<string, unknown>): StatRow[] {
  const rows = Object.entries(stats).map(([key, value]) => ({ key, value: String(value) }));
  return rows.length ? rows : [{ key: '', value: '' }];
}

/** Field types keyed by field key, for a template (empty map when none chosen). */
function fieldTypesOf(template: FieldTemplate | null): Map<string, FieldType> {
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

export function StatblocksPanel({ worldId, templates, campaigns, onError }: Props) {
  const navigate = useNavigate();
  const { statblockId: urlStatblockId } = useParams<{ statblockId: string }>();
  const api = useMemo(() => statblocksApi(worldId), [worldId]);
  const [list, setList] = useState<Statblock[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [filterCampaign, setFilterCampaign] = useState('');
  const [cardsOpen, setCardsOpen] = useState(false);
  // FR-44: encounter-sheet builder over the same selection as card printing.
  const [encounterOpen, setEncounterOpen] = useState(false);
  // Ids ticked for printing; empty = print the whole (filtered) list.
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const statblockTemplates = templates.filter((t) => t.kind === 'STATBLOCK');
  const template = statblockTemplates.find((t) => t.id === draft.templateId) ?? null;
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
      setList(await api.list(filterCampaign || undefined));
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, filterCampaign, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  function edit(sb: Statblock) {
    setDraft({
      id: sb.id,
      name: sb.name,
      notes: sb.notes ?? '',
      campaignId: sb.campaignId ?? '',
      templateId: sb.templateId ?? '',
      rows: toRows(sb.stats),
    });
  }

  // The URL is the source of truth for which statblock is open (ADR-0053).
  useEffect(() => {
    if (!urlStatblockId || urlStatblockId === draft.id) return;
    api.get(urlStatblockId).then(edit).catch(onError);
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
      stats,
      notes: draft.notes || null,
      campaignId: draft.campaignId || null,
      templateId: draft.templateId || null,
    };
    try {
      if (draft.id) await api.update(draft.id, body);
      else await api.create(body);
      setDraft({ ...EMPTY, campaignId: filterCampaign });
      navigate(`/worlds/${worldId}/sheets/statblocks`);
      await refresh();
      toast.success(`Statblock "${body.name}" saved`);
    } catch (err) {
      onError(err);
    }
  }

  async function remove(sb: Statblock) {
    try {
      await api.remove(sb.id);
      if (draft.id === sb.id) {
        setDraft(EMPTY);
        navigate(`/worlds/${worldId}/sheets/statblocks`);
      }
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  function setRow(i: number, patch: Partial<StatRow>) {
    setDraft((d) => ({ ...d, rows: d.rows.map((r, j) => (j === i ? { ...r, ...patch } : r)) }));
  }

  return (
    <div className="sheets-panel">
      <div className="sheets-list-col">
        <Button
          onClick={() => {
            setDraft({ ...EMPTY, campaignId: filterCampaign });
            navigate(`/worlds/${worldId}/sheets/statblocks`);
          }}
        >
          + New statblock
        </Button>
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
                onClick={() => navigate(`/worlds/${worldId}/sheets/statblocks/${sb.id}`)}
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

      <div className="sheet-detail card">
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
            value={draft.templateId || NONE_VALUE}
            onValueChange={(v) => setDraft({ ...draft, templateId: v === NONE_VALUE ? '' : v })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE_VALUE}>None — free-form</SelectItem>
              {statblockTemplates.map((t) => (
                <SelectItem key={t.id} value={t.id}>
                  {t.name}
                </SelectItem>
              ))}
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
        <MarkdownEditor value={draft.notes} onChange={(notes) => setDraft({ ...draft, notes })} />
        <div className="editor-actions">
          <Button onClick={save} disabled={!draft.name}>
            {draft.id ? 'Save statblock' : 'Create statblock'}
          </Button>
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
      </div>

      {cardsOpen && (
        <StatblockCardsView
          statblocks={toPrint}
          templates={statblockTemplates}
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
          onClose={() => setEncounterOpen(false)}
        />
      )}
    </div>
  );
}
