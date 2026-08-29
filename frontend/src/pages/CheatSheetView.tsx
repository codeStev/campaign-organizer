import { FormEvent, useEffect, useMemo, useState } from 'react';
import { NewWindowPortal } from '../components/NewWindowPortal';
import { Button } from '../components/ui/button';
import { Textarea } from '../components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  articlesApi,
  cardDecksApi,
  CardDeck,
  cheatSheetsApi,
  CheatSheetFragment,
  CheatSheetFragmentType,
  fieldTemplatesApi,
  FieldTemplate,
  rollTablesApi,
  RollTable,
  RollTableEntry,
  statblocksApi,
  Statblock,
} from '../api/client';
import { orderedStatEntries } from '../lib/statblockDisplay';
import { renderLinkedMarkdown, renderMarkdown } from '../lib/markdown';

interface Props {
  worldId: string;
  campaignId: string;
  sessionId: string;
  sessionTitle: string;
  onClose: () => void;
  onError: (err: unknown) => void;
}

const KINDS: { value: CheatSheetFragmentType; label: string }[] = [
  { value: 'FREEFORM', label: 'Freeform note' },
  { value: 'STATBLOCK', label: 'Statblock' },
  { value: 'TABLE_ROW', label: 'Roll-table row' },
  { value: 'DECK_CARD', label: 'Deck card' },
];

const EMPTY_ADD = {
  kind: 'FREEFORM' as CheatSheetFragmentType,
  text: '',
  statblockId: '',
  tableId: '',
  entryId: '',
  deckId: '',
  cardId: '',
};

/** "2–7" / "≤12" / "—" for a table row's printed range. */
function entryRange(e: RollTableEntry): string {
  if (e.minResult == null && e.maxResult == null) return '—';
  if (e.minResult != null && (e.maxResult == null || e.minResult === e.maxResult)) {
    return String(e.minResult);
  }
  if (e.minResult == null) return `≤${e.maxResult}`;
  return `${e.minResult}–${e.maxResult}`;
}

function cardLabel(c: { title?: string | null; body: string }): string {
  if (c.title) return c.title;
  const plain = c.body.replace(/[#*_>`[\]]/g, '').trim();
  return plain.length > 48 ? `${plain.slice(0, 48)}…` : plain || '(untitled card)';
}

/** Condensed "AC 15 · HP 7 · Speed 30 ft" line for a referenced statblock. */
function statblockLine(sb: Statblock, templates: FieldTemplate[]): string {
  return orderedStatEntries(sb.stats, sb.templateId, templates)
    .filter((e) => e.type !== 'TEXTAREA' && String(e.value).trim() !== '')
    .slice(0, 8)
    .map((e) => `${e.label} ${String(e.value)}`)
    .join(' · ');
}

/**
 * Editor + printable one-page sheet for one session (FR-37): an ordered list
 * of condensed fragments — freeform notes or references to existing
 * statblocks, roll-table rows and deck cards. The server validates every
 * reference on save; the client owns ordering and pixels.
 */
export function CheatSheetView({
  worldId,
  campaignId,
  sessionId,
  sessionTitle,
  onClose,
  onError,
}: Props) {
  const api = useMemo(
    () => cheatSheetsApi(worldId, campaignId, sessionId),
    [worldId, campaignId, sessionId],
  );
  const [fragments, setFragments] = useState<CheatSheetFragment[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [printOpen, setPrintOpen] = useState(false);
  const [add, setAdd] = useState(EMPTY_ADD);

  const [statblocks, setStatblocks] = useState<Statblock[]>([]);
  const [tables, setTables] = useState<RollTable[]>([]);
  const [decks, setDecks] = useState<CardDeck[]>([]);
  const [templates, setTemplates] = useState<FieldTemplate[]>([]);
  // [[wiki-links]] in referenced rows/cards resolve against the world's articles.
  const [linkTitles, setLinkTitles] = useState<Map<string, string>>(new Map());

  useEffect(() => {
    let active = true;
    api
      .get()
      .then((sheet) => active && setFragments(sheet.fragments))
      .catch(onError)
      .finally(() => active && setLoaded(true));
    statblocksApi(worldId)
      .list()
      .then((l) => active && setStatblocks(l))
      .catch(onError);
    rollTablesApi(worldId)
      .list()
      .then((l) => active && setTables(l))
      .catch(onError);
    cardDecksApi(worldId)
      .list()
      .then((l) => active && setDecks(l))
      .catch(onError);
    fieldTemplatesApi(worldId)
      .list('STATBLOCK')
      .then((t) => active && setTemplates(t))
      .catch(onError);
    articlesApi(worldId)
      .list()
      .then((list) => {
        if (!active) return;
        const byName = new Map<string, string>();
        for (const a of list) {
          if (!byName.has(a.title.toLowerCase())) byName.set(a.title.toLowerCase(), a.title);
          if (!byName.has(a.slug.toLowerCase())) byName.set(a.slug.toLowerCase(), a.title);
        }
        setLinkTitles(byName);
      })
      .catch(() => {
        // Links render as broken-link spans; not worth failing the editor.
      });
    return () => {
      active = false;
    };
  }, [api, worldId, onError]);

  const statblockById = useMemo(
    () => new Map(statblocks.map((s) => [s.id, s])),
    [statblocks],
  );
  const tableById = useMemo(() => new Map(tables.map((t) => [t.id, t])), [tables]);
  const deckById = useMemo(() => new Map(decks.map((d) => [d.id, d])), [decks]);
  const linkLookup = useMemo(
    () => (name: string) => linkTitles.get(name) ?? null,
    [linkTitles],
  );

  function close() {
    if (dirty && !window.confirm('Discard unsaved cheat-sheet changes?')) return;
    onClose();
  }

  function addFragment(e: FormEvent) {
    e.preventDefault();
    const base = { id: null };
    if (add.kind === 'FREEFORM' && add.text.trim()) {
      setFragments((f) => [...f, { ...base, type: 'FREEFORM', text: add.text.trim() }]);
    } else if (add.kind === 'STATBLOCK' && add.statblockId) {
      setFragments((f) => [...f, { ...base, type: 'STATBLOCK', statblockId: add.statblockId }]);
    } else if (add.kind === 'TABLE_ROW' && add.tableId && add.entryId) {
      setFragments((f) => [
        ...f,
        { ...base, type: 'TABLE_ROW', tableId: add.tableId, entryId: add.entryId },
      ]);
    } else if (add.kind === 'DECK_CARD' && add.deckId && add.cardId) {
      setFragments((f) => [
        ...f,
        { ...base, type: 'DECK_CARD', deckId: add.deckId, cardId: add.cardId },
      ]);
    } else {
      return;
    }
    setAdd({ ...EMPTY_ADD, kind: add.kind });
    setDirty(true);
  }

  function move(index: number, delta: number) {
    setFragments((f) => {
      const next = [...f];
      const target = index + delta;
      if (target < 0 || target >= next.length) return f;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
    setDirty(true);
  }

  function removeAt(index: number) {
    setFragments((f) => f.filter((_, i) => i !== index));
    setDirty(true);
  }

  async function save() {
    setSaving(true);
    try {
      const sheet = await api.put(
        fragments.map((f) => ({
          type: f.type,
          text: f.text ?? null,
          statblockId: f.statblockId ?? null,
          tableId: f.tableId ?? null,
          entryId: f.entryId ?? null,
          deckId: f.deckId ?? null,
          cardId: f.cardId ?? null,
        })),
      );
      setFragments(sheet.fragments);
      setDirty(false);
    } catch (err) {
      onError(err);
    } finally {
      setSaving(false);
    }
  }

  /** One fragment's body as it appears in both the editor and the printout. */
  function fragmentBody(f: CheatSheetFragment) {
    switch (f.type) {
      case 'FREEFORM':
        return <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(f.text ?? '') }} />;
      case 'STATBLOCK': {
        const sb = f.statblockId ? statblockById.get(f.statblockId) : undefined;
        if (!sb) return <span className="cheatsheet-missing">Missing statblock</span>;
        const line = statblockLine(sb, templates);
        return (
          <>
            <strong>{sb.name}</strong>
            {line && <div className="cheatsheet-statline">{line}</div>}
          </>
        );
      }
      case 'TABLE_ROW': {
        const t = f.tableId ? tableById.get(f.tableId) : undefined;
        const entry = t?.entries.find((e) => e.id === f.entryId);
        if (!t || !entry) return <span className="cheatsheet-missing">Missing table row</span>;
        return (
          <>
            <span className="cheatsheet-ref">
              {t.title} · {entryRange(entry)}
            </span>
            <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderLinkedMarkdown(entry.body, linkLookup) }} />
          </>
        );
      }
      case 'DECK_CARD': {
        const d = f.deckId ? deckById.get(f.deckId) : undefined;
        const card = d?.cards.find((c) => c.id === f.cardId);
        if (!d || !card) return <span className="cheatsheet-missing">Missing deck card</span>;
        return (
          <>
            <span className="cheatsheet-ref">
              {d.title} · {cardLabel(card)}
            </span>
            <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderLinkedMarkdown(card.body, linkLookup) }} />
          </>
        );
      }
    }
  }

  function kindLabel(t: CheatSheetFragmentType): string {
    return KINDS.find((k) => k.value === t)?.label ?? t;
  }

  const selectedTable = tables.find((t) => t.id === add.tableId);
  const selectedDeck = decks.find((d) => d.id === add.deckId);

  return (
    <>
      <section className="card cheatsheet-editor">
        <h3 className="session-heading">
          📋 Cheat sheet — {sessionTitle}
          <Button variant="link" onClick={close}>
            Close
          </Button>
        </h3>

        <ol className="cheatsheet-list">
          {fragments.map((f, i) => (
            <li key={f.id ?? `new-${i}`} className={`cheatsheet-item cheatsheet-${f.type.toLowerCase()}`}>
              <div className="cheatsheet-order">
                <Button
                  variant="link"
                  onClick={() => move(i, -1)}
                  disabled={i === 0}
                  title="Move up"
                >
                  ↑
                </Button>
                <Button
                  variant="link"
                  onClick={() => move(i, 1)}
                  disabled={i === fragments.length - 1}
                  title="Move down"
                >
                  ↓
                </Button>
              </div>
              <div className="cheatsheet-content">
                <span className={`cheatsheet-badge cheatsheet-badge-${f.type.toLowerCase()}`}>
                  {kindLabel(f.type)}
                </span>
                {fragmentBody(f)}
              </div>
              <Button
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => removeAt(i)}
                title="Remove fragment"
              >
                ✕
              </Button>
            </li>
          ))}
          {loaded && fragments.length === 0 && (
            <li className="muted">
              Nothing on this sheet yet — add freeform notes or reference content below.
            </li>
          )}
        </ol>

        <form className="cheatsheet-add" onSubmit={addFragment}>
          <Select
            value={add.kind}
            onValueChange={(v) =>
              setAdd({ ...EMPTY_ADD, kind: v as CheatSheetFragmentType })
            }
          >
            <SelectTrigger className="cheatsheet-kind-select">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {KINDS.map((k) => (
                <SelectItem key={k.value} value={k.value}>
                  {k.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {add.kind === 'FREEFORM' && (
            <Textarea
              placeholder="Quick note for the table…"
              value={add.text}
              onChange={(e) => setAdd({ ...add, text: e.target.value })}
            />
          )}
          {add.kind === 'STATBLOCK' && (
            <Select value={add.statblockId} onValueChange={(v) => setAdd({ ...add, statblockId: v })}>
              <SelectTrigger>
                <SelectValue placeholder="Pick a statblock…" />
              </SelectTrigger>
              <SelectContent>
                {statblocks.map((s) => (
                  <SelectItem key={s.id} value={s.id}>
                    {s.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
          {add.kind === 'TABLE_ROW' && (
            <>
              <Select
                value={add.tableId}
                onValueChange={(v) => setAdd({ ...add, tableId: v, entryId: '' })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Pick a roll table…" />
                </SelectTrigger>
                <SelectContent>
                  {tables.map((t) => (
                    <SelectItem key={t.id} value={t.id}>
                      {t.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select
                value={add.entryId}
                onValueChange={(v) => setAdd({ ...add, entryId: v })}
                disabled={!selectedTable}
              >
                <SelectTrigger>
                  <SelectValue placeholder={selectedTable ? 'Pick a row…' : 'Table first…'} />
                </SelectTrigger>
                <SelectContent>
                  {(selectedTable?.entries ?? []).map((e) => (
                    <SelectItem key={e.id} value={e.id}>
                      {entryRange(e)} · {e.body.replace(/\[\[|\]\]/g, '').slice(0, 60)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </>
          )}
          {add.kind === 'DECK_CARD' && (
            <>
              <Select
                value={add.deckId}
                onValueChange={(v) => setAdd({ ...add, deckId: v, cardId: '' })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Pick a card deck…" />
                </SelectTrigger>
                <SelectContent>
                  {decks.map((d) => (
                    <SelectItem key={d.id} value={d.id}>
                      {d.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select
                value={add.cardId}
                onValueChange={(v) => setAdd({ ...add, cardId: v })}
                disabled={!selectedDeck}
              >
                <SelectTrigger>
                  <SelectValue placeholder={selectedDeck ? 'Pick a card…' : 'Deck first…'} />
                </SelectTrigger>
                <SelectContent>
                  {(selectedDeck?.cards ?? []).map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {cardLabel(c)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </>
          )}

          <Button type="submit" variant="link">
            + Add
          </Button>
        </form>

        <div className="editor-actions">
          <Button onClick={save} disabled={saving || (!dirty && loaded)}>
            {saving ? 'Saving…' : dirty ? 'Save sheet' : 'Saved'}
          </Button>
          <Button variant="link" onClick={() => setPrintOpen(true)} disabled={fragments.length === 0}>
            🖨 Print
          </Button>
          {dirty && <span className="muted">Unsaved changes</span>}
        </div>
      </section>

      {printOpen && (
        <NewWindowPortal title={`Cheat sheet — ${sessionTitle}`} onClose={() => setPrintOpen(false)}>
          <div className="print-toolbar">
            <strong>Cheat sheet</strong>
            <span className="print-toolbar-spacer" />
            <Button onClick={() => window.print()}>🖨 Print</Button>
            <Button variant="link" onClick={() => setPrintOpen(false)}>
              Close
            </Button>
          </div>
          <div className="print-doc cheat-sheet-print">
            <section className="print-cover">
              <h1>{sessionTitle}</h1>
              <p className="print-subtitle">GM cheat sheet</p>
            </section>
            <ol className="cheatsheet-list">
              {fragments.map((f, i) => (
                <li
                  key={f.id ?? `p-${i}`}
                  className={`cheatsheet-item cheatsheet-${f.type.toLowerCase()}`}
                >
                  <span className="cheatsheet-num">{i + 1}</span>
                  <div className="cheatsheet-content">{fragmentBody(f)}</div>
                </li>
              ))}
            </ol>
          </div>
        </NewWindowPortal>
      )}
    </>
  );
}
