import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  encountersApi,
  Encounter,
  EncounterEntry,
  EncounterRequest,
  Statblock,
  FieldTemplate,
  GlobalFieldTemplate,
} from '../api/client';
import { EncounterSheetView } from './EncounterSheetView';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Spinner } from '../components/ui/spinner';
import { toast } from 'sonner';

const NONE_VALUE = '__none__';

interface Props {
  worldId: string;
  campaignId: string;
  statblocks: Statblock[];
  templates: FieldTemplate[];
  globalTemplates: GlobalFieldTemplate[];
  onError: (err: unknown) => void;
}

/**
 * Persisted, printable groupings of statblocks (ADR-0097) - the reusable
 * counterpart to the ad-hoc "⚔ Encounter" flow (ADR-0069, still available
 * unchanged from StatblocksPanel). Linked to arc beats via ArcBoard.
 */
export function EncounterBoard({ worldId, campaignId, statblocks, templates, globalTemplates, onError }: Props) {
  const api = useMemo(() => encountersApi(worldId, campaignId), [worldId, campaignId]);
  const [encounters, setEncounters] = useState<Encounter[]>([]);
  const [loading, setLoading] = useState(true);
  const [newName, setNewName] = useState('');
  const [printing, setPrinting] = useState<Encounter | null>(null);

  const refresh = useCallback(async () => {
    try {
      setEncounters(await api.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function addEncounter(e: FormEvent) {
    e.preventDefault();
    if (!newName) return;
    try {
      const created = await api.create({ name: newName, entries: [] });
      setNewName('');
      await refresh();
      toast.success(`Encounter "${created.name}" created`);
    } catch (err) {
      onError(err);
    }
  }

  async function saveEncounter(encounter: Encounter, request: EncounterRequest) {
    try {
      const updated = await api.update(encounter.id, request);
      setEncounters((es) => es.map((x) => (x.id === encounter.id ? updated : x)));
    } catch (err) {
      onError(err);
    }
  }

  async function removeEncounter(encounter: Encounter) {
    try {
      await api.remove(encounter.id);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  const printStatblocks = printing
    ? printing.entries
        .map((entry) => statblocks.find((s) => s.id === entry.statblockId))
        .filter((s): s is Statblock => Boolean(s))
    : [];
  const printInitialEntries = printing
    ? Object.fromEntries(
        printing.entries.map((entry) => [
          entry.statblockId,
          { qty: entry.quantity, maxHp: entry.maxHpOverride == null ? '' : String(entry.maxHpOverride) },
        ]),
      )
    : undefined;

  return (
    <section className="card">
      <h3>Encounters</h3>
      <p className="muted hint">
        Build a reusable, printable grouping of statblocks — link it to an arc beat below, or print it any time.
      </p>
      <form className="editor-actions" onSubmit={addEncounter}>
        <Input placeholder="New encounter name" value={newName} onChange={(e) => setNewName(e.target.value)} />
        <Button type="submit" disabled={!newName}>
          Add encounter
        </Button>
      </form>

      <div className="arc-list">
        {encounters.map((encounter) => (
          <EncounterCard
            key={encounter.id}
            encounter={encounter}
            statblocks={statblocks}
            onSave={(request) => saveEncounter(encounter, request)}
            onRemove={() => removeEncounter(encounter)}
            onPrint={() => setPrinting(encounter)}
          />
        ))}
        {loading && (
          <p className="muted loading-row">
            <Spinner /> Loading…
          </p>
        )}
        {!loading && encounters.length === 0 && <p className="muted">No encounters yet.</p>}
      </div>

      {printing && (
        <EncounterSheetView
          worldId={worldId}
          statblocks={printStatblocks}
          templates={templates}
          globalTemplates={globalTemplates}
          initialEntries={printInitialEntries}
          onClose={() => setPrinting(null)}
        />
      )}
    </section>
  );
}

interface EncounterCardProps {
  encounter: Encounter;
  statblocks: Statblock[];
  onSave: (request: EncounterRequest) => void;
  onRemove: () => void;
  onPrint: () => void;
}

function EncounterCard({ encounter, statblocks, onSave, onRemove, onPrint }: EncounterCardProps) {
  const [open, setOpen] = useState(false);

  function statblockName(id: string): string {
    return statblocks.find((s) => s.id === id)?.name ?? '(unknown statblock)';
  }

  function setNotes(notes: string) {
    onSave({ name: encounter.name, notes: notes || null, entries: encounter.entries });
  }

  function setEntry(index: number, patch: Partial<EncounterEntry>) {
    const entries = encounter.entries.map((e, i) => (i === index ? { ...e, ...patch } : e));
    onSave({ name: encounter.name, notes: encounter.notes, entries });
  }

  function removeEntry(index: number) {
    onSave({ name: encounter.name, notes: encounter.notes, entries: encounter.entries.filter((_, i) => i !== index) });
  }

  function addEntry(statblockId: string) {
    onSave({
      name: encounter.name,
      notes: encounter.notes,
      entries: [...encounter.entries, { statblockId, quantity: 1, maxHpOverride: null }],
    });
  }

  const availableStatblocks = statblocks.filter(
    (s) => !encounter.entries.some((e) => e.statblockId === s.id),
  );

  return (
    <div className="arc-card">
      <div className="arc-head">
        <button className="arc-toggle" onClick={() => setOpen((v) => !v)}>
          <span className="caret">{open ? '▼' : '▶'}</span>
          <strong>{encounter.name}</strong>
        </button>
        <span className="muted">
          {encounter.entries.length} statblock{encounter.entries.length === 1 ? '' : 's'}
        </span>
        <Button variant="link" onClick={onPrint} disabled={encounter.entries.length === 0}>
          🖨 Print
        </Button>
        <ConfirmDeleteDialog
          trigger={
            <Button variant="link" className="text-destructive hover:text-destructive">
              ✕
            </Button>
          }
          title="Delete encounter?"
          description={`This permanently deletes "${encounter.name}" and cannot be undone. Beats linking to it just lose the reference.`}
          onConfirm={onRemove}
        />
      </div>

      {open && (
        <div className="arc-beats">
          <MarkdownEditor value={encounter.notes ?? ''} onChange={setNotes} />

          <ul className="article-list">
            {encounter.entries.map((entry, i) => (
              <li key={i} className="rel-row">
                <span>{statblockName(entry.statblockId)}</span>
                <Input
                  type="number"
                  min={1}
                  max={20}
                  value={entry.quantity}
                  title="Quantity"
                  onChange={(e) => setEntry(i, { quantity: Math.max(1, Number(e.target.value) || 1) })}
                />
                <Input
                  type="number"
                  min={0}
                  placeholder="max HP (auto)"
                  value={entry.maxHpOverride ?? ''}
                  onChange={(e) =>
                    setEntry(i, { maxHpOverride: e.target.value === '' ? null : Number(e.target.value) })
                  }
                />
                <Button
                  type="button"
                  variant="link"
                  className="text-destructive hover:text-destructive"
                  onClick={() => removeEntry(i)}
                >
                  ✕
                </Button>
              </li>
            ))}
            {encounter.entries.length === 0 && <li className="muted">No statblocks yet.</li>}
          </ul>

          {availableStatblocks.length > 0 && (
            <Select value={NONE_VALUE} onValueChange={addEntry}>
              <SelectTrigger>
                <SelectValue placeholder="+ add statblock" />
              </SelectTrigger>
              <SelectContent>
                {availableStatblocks.map((s) => (
                  <SelectItem key={s.id} value={s.id}>
                    {s.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
      )}
    </div>
  );
}
