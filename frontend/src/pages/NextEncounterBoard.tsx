import { FormEvent, useMemo, useState } from 'react';
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
import { toast } from 'sonner';

const NONE_VALUE = '__none__';

interface Props {
  worldId: string;
  campaignId: string;
  encounters: Encounter[];
  onChanged: () => void;
  statblocks: Statblock[];
  templates: FieldTemplate[];
  globalTemplates: GlobalFieldTemplate[];
  onError: (err: unknown) => void;
}

/**
 * /next's own fork of EncounterBoard (ADR-0106) — persisted, printable
 * groupings of statblocks (ADR-0097), the reusable counterpart to the
 * ad-hoc "⚔ Encounter" flow (ADR-0069, still available unchanged from
 * StatblocksPanel). Unlike the old version, each encounter card is always
 * expanded instead of an expand/collapse accordion (`.arc-card`/
 * `.arc-toggle`) — that pattern was already rejected for Story Arcs this
 * session (NextArcsPage gave arcs their own dedicated page instead of
 * inline expand/collapse cards). An encounter's entries are a flat
 * statblock+quantity list plus a notes field, not a rich enough surface to
 * warrant its own route the way an arc's beats did, so it stays a same-page
 * card list — just without the collapse behavior, and already scoped to one
 * campaign at a time via NextEncountersPage/CampaignNavTree.
 */
export function NextEncounterBoard({
  worldId,
  campaignId,
  encounters,
  onChanged,
  statblocks,
  templates,
  globalTemplates,
  onError,
}: Props) {
  const api = useMemo(() => encountersApi(worldId, campaignId), [worldId, campaignId]);
  const [newName, setNewName] = useState('');
  const [printing, setPrinting] = useState<Encounter | null>(null);

  async function addEncounter(e: FormEvent) {
    e.preventDefault();
    if (!newName) return;
    try {
      const created = await api.create({ name: newName, entries: [] });
      setNewName('');
      onChanged();
      toast.success(`Encounter "${created.name}" created`);
    } catch (err) {
      onError(err);
    }
  }

  async function saveEncounter(encounter: Encounter, request: EncounterRequest) {
    try {
      await api.update(encounter.id, request);
      onChanged();
    } catch (err) {
      onError(err);
    }
  }

  async function removeEncounter(encounter: Encounter) {
    try {
      await api.remove(encounter.id);
      onChanged();
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
    ? Object.fromEntries(printing.entries.map((entry) => [entry.statblockId, { qty: entry.quantity }]))
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
        {encounters.length === 0 && <p className="muted">No encounters yet.</p>}
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
      entries: [...encounter.entries, { statblockId, quantity: 1 }],
    });
  }

  const availableStatblocks = statblocks.filter(
    (s) => !encounter.entries.some((e) => e.statblockId === s.id),
  );

  return (
    <div className="arc-card">
      <div className="arc-head">
        <strong>{encounter.name}</strong>
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

      <div className="arc-beats">
        <MarkdownEditor value={encounter.notes ?? ''} onChange={setNotes} />

        {encounter.entries.length > 0 ? (
          <table className="encounter-table">
            <thead>
              <tr>
                <th>Combatant</th>
                <th>Qty</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {encounter.entries.map((entry, i) => (
                <tr key={i}>
                  <td>{statblockName(entry.statblockId)}</td>
                  <td>
                    <Input
                      type="number"
                      min={1}
                      max={20}
                      value={entry.quantity}
                      title="Quantity"
                      onChange={(e) => setEntry(i, { quantity: Math.max(1, Number(e.target.value) || 1) })}
                    />
                  </td>
                  <td>
                    <Button
                      type="button"
                      variant="link"
                      className="text-destructive hover:text-destructive"
                      onClick={() => removeEntry(i)}
                    >
                      ✕
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="muted">No statblocks yet.</p>
        )}

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
    </div>
  );
}
