import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  rollTablesApi,
  cardDecksApi,
  diceApi,
  articlesApi,
  RollTable,
  RollTableEntry,
  CardDeck,
  DeckCard,
  ApiError,
} from '../api/client';
import { diceRange, DiceRange } from '../lib/dice';
import { renderLinkedMarkdown } from '../lib/markdown';
import { ArticleLinkPicker } from '../components/ArticleLinkPicker';
import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { Button } from '../components/ui/button';
import { Toggle } from '../components/ui/toggle';
import { Spinner } from '../components/ui/spinner';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

interface TableEntryDraft {
  minResult: string;
  maxResult: string;
  body: string;
  nestedTableIds: string[];
  nestedDeckIds: string[];
}

interface DeckCardDraft {
  title: string;
  body: string;
  nestedTableIds: string[];
  nestedDeckIds: string[];
}

const emptyChains = { nestedTableIds: [], nestedDeckIds: [] };

/** Which half of the tab the editor shows; the URL keeps it deep-linkable (FR-35). */
type DraftKind = 'table' | 'deck';

interface Draft {
  kind: DraftKind;
  id: string | null;
  title: string;
  description: string;
  diceExpression: string;
  entries: TableEntryDraft[];
  cards: DeckCardDraft[];
}

const EMPTY_DRAFT: Draft = {
  kind: 'table',
  id: null,
  title: '',
  description: '',
  diceExpression: '',
  entries: [{ minResult: '', maxResult: '', body: '', ...emptyChains }],
  cards: [],
};

function draftFromTable(t: RollTable): Draft {
  return {
    kind: 'table',
    id: t.id,
    title: t.title,
    description: t.description ?? '',
    diceExpression: t.diceExpression,
    entries: t.entries.map((e) => ({
      minResult: e.minResult != null ? String(e.minResult) : '',
      maxResult: e.maxResult != null ? String(e.maxResult) : '',
      body: e.body,
      nestedTableIds: e.nestedTableIds ?? [],
      nestedDeckIds: e.nestedDeckIds ?? [],
    })),
    cards: [],
  };
}

function draftFromDeck(d: CardDeck): Draft {
  return {
    kind: 'deck',
    id: d.id,
    title: d.title,
    description: d.description ?? '',
    diceExpression: '',
    entries: [],
    cards: d.cards.map((c) => ({
      title: c.title ?? '',
      body: c.body,
      nestedTableIds: c.nestedTableIds ?? [],
      nestedDeckIds: c.nestedDeckIds ?? [],
    })),
  };
}

/** Rows evenly covering the whole range with n entries; remainder goes to the front. */
function evenSplitRows(range: DiceRange, count: number): TableEntryDraft[] {
  const size = range.max - range.min + 1;
  const per = Math.floor(size / count);
  const remainder = size % count;
  const rows: TableEntryDraft[] = [];
  let cursor = range.min;
  for (let i = 0; i < count; i++) {
    const span = per + (i < remainder ? 1 : 0);
    rows.push({ minResult: String(cursor), maxResult: String(cursor + span - 1), body: '', ...emptyChains });
    cursor += span;
  }
  return rows;
}

/** Client-side pre-checks mirroring the domain rules; server stays authoritative. */
function tableDraftProblem(draft: Draft, range: DiceRange | null): string | null {
  if (!draft.title.trim()) return 'Give the table a title';
  if (!range) return `Not a usable dice expression: "${draft.diceExpression}"`;
  const bounds: Array<[number, number]> = [];
  let fallbacks = 0;
  for (const entry of draft.entries) {
    if (!entry.body.trim()) return 'Every entry needs an outcome text';
    if (entry.minResult === '' && entry.maxResult === '') {
      fallbacks += 1;
      continue;
    }
    if (entry.minResult === '' || entry.maxResult === '') {
      return 'Entries need both result bounds or neither';
    }
    const min = Number(entry.minResult);
    const max = Number(entry.maxResult);
    if (min < range.min || max > range.max) {
      return `Entry bounds must sit inside ${range.min}–${range.max}`;
    }
    if (min > max) return 'An entry range is inverted';
    bounds.push([min, max]);
  }
  if (fallbacks > 1) return 'Only one entry may cover the remaining results';
  // Overlap check over explicit ranges, sorted numerically by lower bound.
  bounds.sort((a, b) => a[0] - b[0]);
  for (let i = 1; i < bounds.length; i++) {
    if (bounds[i][0] <= bounds[i - 1][1]) return 'Entry ranges must not overlap';
  }
  return null;
}

export function TablesView({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { kind: urlKind, entityId: urlEntityId } = useParams<{
    kind?: string;
    entityId?: string;
  }>();
  const tablesApi = useMemo(() => rollTablesApi(worldId), [worldId]);
  const decksApi = useMemo(() => cardDecksApi(worldId), [worldId]);
  const [tables, setTables] = useState<RollTable[]>([]);
  const [decks, setDecks] = useState<CardDeck[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [error, setError] = useState<string | null>(null);
  // Read (rendered, with roll/draw) vs edit (the form) — mirrors WorldView's article mode.
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  // Save button feedback: "saved" auto-reverts after a beat so it doesn't
  // linger indefinitely once the user starts editing again.
  const [saveState, setSaveState] = useState<'idle' | 'saving' | 'saved'>('idle');
  const savedTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => () => {
    if (savedTimeout.current) clearTimeout(savedTimeout.current);
  }, []);
  function markSaved(message: string) {
    setSaveState('saved');
    if (savedTimeout.current) clearTimeout(savedTimeout.current);
    savedTimeout.current = setTimeout(() => setSaveState('idle'), 1500);
    // The button's own "Saved" state reverts almost immediately (round-trips
    // are fast), which is easy to miss entirely — the toast is the feedback
    // that's actually meant to be noticed.
    toast.success(message);
  }
  // Roll result for the table editor; matchedIndex points at the hit entry row.
  const [roll, setRoll] = useState<{ total: number; breakdown: string; matchedIndex: number | null } | null>(null);
  // Stateless deck draw (ADR-0066): just a highlighted random card.
  const [drawnIndex, setDrawnIndex] = useState<number | null>(null);
  // Key of the textarea the link picker inserts into, e.g. "entry-2" / "card-0".
  const [linkTarget, setLinkTarget] = useState<string | null>(null);
  // Row whose chained-content picker is open, same key scheme.
  const [chainOpen, setChainOpen] = useState<string | null>(null);
  const textareas = useRef<Record<string, HTMLTextAreaElement | null>>({});
  // Standalone print of the open table/deck (ADR-0038 pattern).
  const [printing, setPrinting] = useState(false);
  const { opts: printOpts, setOpts: setPrintOpts, docProps: printDocProps } = usePrintOptions();
  // Lowercase title/slug → display title, for [[wiki-links]] in the printout.
  const [linkTitles, setLinkTitles] = useState<Map<string, string>>(new Map());

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(async () => {
    try {
      const [t, d] = await Promise.all([tablesApi.list(), decksApi.list()]);
      setTables(t);
      setDecks(d);
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [tablesApi, decksApi, handleError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  function edit(kind: DraftKind, entity: RollTable | CardDeck) {
    setDraft(kind === 'table' ? draftFromTable(entity as RollTable) : draftFromDeck(entity as CardDeck));
    setRoll(null);
    setDrawnIndex(null);
    setSaveState('idle');
    setMode('read');
  }

  // The URL is the source of truth for what's open (ADR-0053): /tables/table/:id
  // or /tables/deck/:id. Resolve against the already-loaded lists.
  useEffect(() => {
    if (!urlKind || !urlEntityId) return;
    const wanted = urlKind === 'table' ? 'table' : 'deck';
    if (urlKind !== 'table' && urlKind !== 'deck') return;
    if (draft.kind === wanted && draft.id === urlEntityId) return;
    if (wanted === 'table') {
      const found = tables.find((t) => t.id === urlEntityId);
      if (found) edit('table', found);
    } else {
      const found = decks.find((d) => d.id === urlEntityId);
      if (found) edit('deck', found);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlKind, urlEntityId, tables, decks]);

  const liveRange = useMemo(
    () => (draft.kind === 'table' ? diceRange(draft.diceExpression) : null),
    [draft.kind, draft.diceExpression],
  );
  // Deferred so editing the count doesn't clobber existing entry rows.
  const [splitCount, setSplitCount] = useState('3');

  // The saved entity behind the draft — only saved tables/decks can print.
  const printTable =
    draft.kind === 'table' && draft.id != null
      ? tables.find((t) => t.id === draft.id) ?? null
      : null;
  const printDeck =
    draft.kind === 'deck' && draft.id != null ? decks.find((d) => d.id === draft.id) ?? null : null;

  // Where the last roll landed / which card was drawn — chains on that row
  // become live sub-rollers below the result line (FR-41).
  const landedEntry = roll?.matchedIndex != null ? draft.entries[roll.matchedIndex] ?? null : null;
  const drawnCard = drawnIndex != null ? draft.cards[drawnIndex] ?? null : null;

  // Everything the printed table/deck chains in, breadth-first, each printed
  // once — cycles cut by the seen-sets (FR-41).
  const chainedForPrint = useMemo(() => {
    if (!printTable && !printDeck) return { tables: [] as RollTable[], decks: [] as CardDeck[] };
    const outTables: RollTable[] = [];
    const outDecks: CardDeck[] = [];
    const seenTables = new Set<string>(printTable ? [printTable.id] : []);
    const seenDecks = new Set<string>(printDeck ? [printDeck.id] : []);
    type Ref = { kind: 'table' | 'deck'; id: string };
    let frontier: Ref[] = [
      ...(printTable
        ? printTable.entries.flatMap((e) => [
            ...e.nestedTableIds.map((id): Ref => ({ kind: 'table', id })),
            ...e.nestedDeckIds.map((id): Ref => ({ kind: 'deck', id })),
          ])
        : []),
      ...(printDeck
        ? printDeck.cards.flatMap((c) => [
            ...c.nestedTableIds.map((id): Ref => ({ kind: 'table', id })),
            ...c.nestedDeckIds.map((id): Ref => ({ kind: 'deck', id })),
          ])
        : []),
    ];
    while (frontier.length > 0) {
      const next: Ref[] = [];
      for (const ref of frontier) {
        if (ref.kind === 'table') {
          if (seenTables.has(ref.id)) continue;
          seenTables.add(ref.id);
          const t = tables.find((x) => x.id === ref.id);
          if (!t) continue;
          outTables.push(t);
          t.entries.forEach((e) => {
            e.nestedTableIds.forEach((id) => next.push({ kind: 'table', id }));
            e.nestedDeckIds.forEach((id) => next.push({ kind: 'deck', id }));
          });
        } else {
          if (seenDecks.has(ref.id)) continue;
          seenDecks.add(ref.id);
          const d = decks.find((x) => x.id === ref.id);
          if (!d) continue;
          outDecks.push(d);
          d.cards.forEach((c) => {
            c.nestedTableIds.forEach((id) => next.push({ kind: 'table', id }));
            c.nestedDeckIds.forEach((id) => next.push({ kind: 'deck', id }));
          });
        }
      }
      frontier = next;
    }
    return { tables: outTables, decks: outDecks };
  }, [printTable, printDeck, tables, decks]);

  // Chained sections print appended at the end, so a row that chains
  // somewhere needs its own note naming the target(s) — otherwise there's
  // no way to tell, on paper, which appended table/deck a result leads to.
  function chainNote(nestedTableIds: string[], nestedDeckIds: string[]): string | null {
    const names = [
      ...nestedTableIds.map((id) => tables.find((t) => t.id === id)?.title),
      ...nestedDeckIds.map((id) => decks.find((d) => d.id === id)?.title),
    ].filter((n): n is string => !!n);
    return names.length > 0 ? names.join(', ') : null;
  }

  /** Read-only entries grid — shared by the read view and the print portal. */
  function tableEntriesGrid(entries: RollTableEntry[]) {
    return (
      <table className="print-table-grid">
        <tbody>
          {entries.map((e) => (
            <tr key={e.id}>
              <td className="print-table-range">
                {e.minResult != null && e.maxResult != null ? `${e.minResult}–${e.maxResult}` : 'else'}
              </td>
              <td>
                {/* eslint-disable-next-line react/no-danger */}
                <div
                  className="preview-body"
                  dangerouslySetInnerHTML={{ __html: renderLinkedMarkdown(e.body, titleLookup) }}
                />
                {chainNote(e.nestedTableIds, e.nestedDeckIds) && (
                  <p className="print-chain-note">↳ see: {chainNote(e.nestedTableIds, e.nestedDeckIds)}</p>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  /** Read-only cards sheet — shared by the read view and the print portal. */
  function deckCardSheet(cards: DeckCard[]) {
    return (
      <div className="card-sheet">
        {cards.map((c) => (
          <div key={c.id} className="deck-card">
            {c.title && <div className="deck-card-name">{c.title}</div>}
            {/* eslint-disable-next-line react/no-danger */}
            <div
              className="preview-body"
              dangerouslySetInnerHTML={{ __html: renderLinkedMarkdown(c.body, titleLookup) }}
            />
            {chainNote(c.nestedTableIds, c.nestedDeckIds) && (
              <p className="print-chain-note">↳ see: {chainNote(c.nestedTableIds, c.nestedDeckIds)}</p>
            )}
          </div>
        ))}
      </div>
    );
  }

  useEffect(() => {
    if ((!printing && !(mode === 'read' && draft.id != null)) || linkTitles.size > 0) return;
    articlesApi(worldId)
      .list()
      .then((list) => {
        const byName = new Map<string, string>();
        for (const a of list) {
          if (!byName.has(a.title.toLowerCase())) byName.set(a.title.toLowerCase(), a.title);
          if (!byName.has(a.slug.toLowerCase())) byName.set(a.slug.toLowerCase(), a.title);
        }
        setLinkTitles(byName);
      })
      .catch(() => {
        // Print anyway; [[links]] render as broken-link spans.
      });
  }, [printing, mode, draft.id, worldId, linkTitles.size]);
  const titleLookup = (name: string) => linkTitles.get(name) ?? null;

  function setEntry(index: number, patch: Partial<TableEntryDraft>) {
    setDraft((d) => ({
      ...d,
      entries: d.entries.map((e, i) => (i === index ? { ...e, ...patch } : e)),
    }));
  }

  /** Add/remove a chained table/deck on one entry or card row (FR-41). */
  function toggleNested(
    key: string,
    field: 'nestedTableIds' | 'nestedDeckIds',
    id: string,
  ) {
    const pick = (ids: string[]) =>
      ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
    setDraft((d) => {
      if (key.startsWith('entry-')) {
        const i = Number(key.slice('entry-'.length));
        return {
          ...d,
          entries: d.entries.map((e, j) =>
            j === i
              ? field === 'nestedTableIds'
                ? { ...e, nestedTableIds: pick(e.nestedTableIds) }
                : { ...e, nestedDeckIds: pick(e.nestedDeckIds) }
              : e,
          ),
        };
      }
      const i = Number(key.slice('card-'.length));
      return {
        ...d,
        cards: d.cards.map((c, j) =>
          j === i
            ? field === 'nestedTableIds'
              ? { ...c, nestedTableIds: pick(c.nestedTableIds) }
              : { ...c, nestedDeckIds: pick(c.nestedDeckIds) }
            : c,
        ),
      };
    });
  }

  function moveCard(index: number, delta: number) {
    setDraft((d) => {
      const cards = [...d.cards];
      const target = index + delta;
      if (target < 0 || target >= cards.length) return d;
      [cards[index], cards[target]] = [cards[target], cards[index]];
      return { ...d, cards };
    });
  }

  /** Row a rolled total lands on: explicit range first, else the fallback row. */
  function matchingEntryIndex(total: number): number | null {
    const explicit = draft.entries.findIndex(
      (e) =>
        e.minResult !== '' &&
        e.maxResult !== '' &&
        Number(e.minResult) <= total &&
        total <= Number(e.maxResult),
    );
    if (explicit >= 0) return explicit;
    return draft.entries.findIndex((e) => e.minResult === '' && e.maxResult === '');
  }

  async function doRoll() {
    setError(null);
    try {
      const result = await diceApi.roll(draft.diceExpression);
      setRoll({
        total: result.total,
        breakdown: result.breakdown,
        matchedIndex: matchingEntryIndex(result.total),
      });
    } catch (err) {
      handleError(err);
    }
  }

  function drawCard() {
    setDrawnIndex(Math.floor(Math.random() * draft.cards.length));
  }

  /** Splice the picked [[Title]] into the targeted textarea at its caret. */
  function insertLink(title: string) {
    const key = linkTarget;
    if (!key) return;
    const el = textareas.current[key];
    if (!el) return;
    const link = `[[${title}]]`;
    const start = el.selectionStart ?? el.value.length;
    const end = el.selectionEnd ?? start;
    const next = el.value.slice(0, start) + link + el.value.slice(end);
    const caret = start + link.length;
    if (key.startsWith('entry-')) {
      setEntry(Number(key.slice('entry-'.length)), { body: next });
    } else {
      const i = Number(key.slice('card-'.length));
      setDraft((d) => ({
        ...d,
        cards: d.cards.map((c, j) => (j === i ? { ...c, body: next } : c)),
      }));
    }
    setLinkTarget(null);
    requestAnimationFrame(() => {
      const updated = textareas.current[key];
      if (updated) {
        updated.focus();
        updated.setSelectionRange(caret, caret);
      }
    });
  }

  /** Evenly cover the expression's whole range with n entries. */
  function splitEvenly() {
    if (!liveRange) return;
    const count = Math.min(Math.max(Number(splitCount) || 1, 1), liveRange.max - liveRange.min + 1);
    setDraft((d) => ({ ...d, entries: evenSplitRows(liveRange, count) }));
  }

  /** Chip picker for one row's chained tables/decks; the open entity excludes itself. */
  function chainPicker(key: string) {
    const row = key.startsWith('entry-')
      ? draft.entries[Number(key.slice('entry-'.length))]
      : draft.cards[Number(key.slice('card-'.length))];
    if (!row) return null;
    const otherTables = tables.filter((t) => t.id !== draft.id);
    const otherDecks = decks.filter((d) => d.id !== draft.id);
    return (
      <div className="chain-picker">
        <strong className="muted">Chain into this result</strong>
        {otherTables.length > 0 && (
          <div className="chain-group">
            <span className="muted">🎲</span>
            {otherTables.map((t) => (
              <Toggle
                type="button"
                key={t.id}
                className={row.nestedTableIds.includes(t.id) ? 'chain-chip on' : 'chain-chip'}
                pressed={row.nestedTableIds.includes(t.id)}
                onPressedChange={() => toggleNested(key, 'nestedTableIds', t.id)}
              >
                {t.title}
              </Toggle>
            ))}
          </div>
        )}
        {otherDecks.length > 0 && (
          <div className="chain-group">
            <span className="muted">🃏</span>
            {otherDecks.map((d) => (
              <Toggle
                type="button"
                key={d.id}
                className={row.nestedDeckIds.includes(d.id) ? 'chain-chip on' : 'chain-chip'}
                pressed={row.nestedDeckIds.includes(d.id)}
                onPressedChange={() => toggleNested(key, 'nestedDeckIds', d.id)}
              >
                {d.title}
              </Toggle>
            ))}
          </div>
        )}
        {otherTables.length === 0 && otherDecks.length === 0 && (
          <small className="muted">Nothing to chain yet — create another table or deck first.</small>
        )}
      </div>
    );
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (draft.kind === 'table') {
      const problem = tableDraftProblem(draft, liveRange);
      if (problem) {
        setError(problem);
        return;
      }
      const body = {
        title: draft.title,
        description: draft.description || undefined,
        diceExpression: draft.diceExpression,
        entries: draft.entries.map((entry) => ({
          minResult: entry.minResult === '' ? null : Number(entry.minResult),
          maxResult: entry.maxResult === '' ? null : Number(entry.maxResult),
          body: entry.body,
          nestedTableIds: entry.nestedTableIds,
          nestedDeckIds: entry.nestedDeckIds,
        })),
      };
      setSaveState('saving');
      try {
        const saved =
          draft.id != null ? await tablesApi.update(draft.id, body) : await tablesApi.create(body);
        edit('table', saved);
        navigate(`/worlds/${worldId}/tables/table/${saved.id}`);
        await refresh();
        markSaved(`Table "${saved.title}" saved`);
      } catch (err) {
        setSaveState('idle');
        handleError(err);
      }
    } else {
      if (!draft.title.trim()) {
        setError('Give the deck a title');
        return;
      }
      if (draft.cards.some((c) => !c.body.trim())) {
        setError('Every card needs a body');
        return;
      }
      const body = {
        title: draft.title,
        description: draft.description || undefined,
        cards: draft.cards.map((c) => ({
          title: c.title || undefined,
          body: c.body,
          nestedTableIds: c.nestedTableIds,
          nestedDeckIds: c.nestedDeckIds,
        })),
      };
      setSaveState('saving');
      try {
        const saved =
          draft.id != null ? await decksApi.update(draft.id, body) : await decksApi.create(body);
        edit('deck', saved);
        navigate(`/worlds/${worldId}/tables/deck/${saved.id}`);
        await refresh();
        markSaved(`Deck "${saved.title}" saved`);
      } catch (err) {
        setSaveState('idle');
        handleError(err);
      }
    }
  }

  async function remove() {
    if (draft.id == null) return;
    try {
      if (draft.kind === 'table') await tablesApi.remove(draft.id);
      else await decksApi.remove(draft.id);
      setDraft(EMPTY_DRAFT);
      navigate(`/worlds/${worldId}/tables`);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  const editingExisting = draft.id != null;

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <Button
          onClick={() => {
            setDraft({
              ...EMPTY_DRAFT,
              kind: 'table',
              entries: [{ minResult: '', maxResult: '', body: '', ...emptyChains }],
            });
            setRoll(null);
            setMode('edit');
            navigate(`/worlds/${worldId}/tables`);
          }}
          data-testid="new-table-button"
        >
          + New roll table
        </Button>
        <ul className="article-list">
          {tables.map((t) => (
            <li key={t.id}>
              <button
                className={
                  draft.kind === 'table' && draft.id === t.id ? 'article-link active' : 'article-link'
                }
                onClick={() => navigate(`/worlds/${worldId}/tables/table/${t.id}`)}
              >
                <TruncatedLabel label={t.title}>🎲 {t.title}</TruncatedLabel>
                <small className="muted">
                  {t.diceExpression} · {t.entries.length} entries
                </small>
              </button>
            </li>
          ))}
          {!loading && tables.length === 0 && <li className="muted">No roll tables yet.</li>}
        </ul>
        <Button
          onClick={() => {
            setDraft({
              ...EMPTY_DRAFT,
              kind: 'deck',
              entries: [],
              cards: [{ title: '', body: '', ...emptyChains }],
            });
            setDrawnIndex(null);
            setMode('edit');
            navigate(`/worlds/${worldId}/tables`);
          }}
          data-testid="new-deck-button"
        >
          + New card deck
        </Button>
        <ul className="article-list">
          {decks.map((d) => (
            <li key={d.id}>
              <button
                className={
                  draft.kind === 'deck' && draft.id === d.id ? 'article-link active' : 'article-link'
                }
                onClick={() => navigate(`/worlds/${worldId}/tables/deck/${d.id}`)}
              >
                <TruncatedLabel label={d.title}>🃏 {d.title}</TruncatedLabel>
                <small className="muted">{d.cards.length} cards</small>
              </button>
            </li>
          ))}
          {!loading && decks.length === 0 && <li className="muted">No card decks yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        {(mode === 'edit' || !editingExisting) && (
        <form className="card" onSubmit={save}>
          {draft.kind === 'table' ? (
            <>
              <strong>{editingExisting ? 'Edit roll table' : 'New roll table'}</strong>
              <Input
                placeholder="Table title"
                value={draft.title}
                onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                required
                data-testid="table-title-input"
              />
              <Input
                placeholder="Description (optional)"
                value={draft.description}
                onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              />
              <label className="muted">
                Dice expression
                <Input
                  placeholder="2d6+1d4"
                  value={draft.diceExpression}
                  onChange={(e) => setDraft({ ...draft, diceExpression: e.target.value })}
                  required
                  data-testid="table-dice-input"
                />
              </label>
              <small className="muted">
                {liveRange
                  ? `Possible results: ${liveRange.min}–${liveRange.max}`
                  : 'Enter any dice combination, e.g. 2d6 or 4d6kh3'}
              </small>
              <div className="editor-actions">
                <Button type="button" variant="outline" disabled={!liveRange} onClick={() => void doRoll()}>
                  🎲 Roll
                </Button>
                {roll && (
                  <small className="muted">
                    Rolled <strong>{roll.total}</strong> ({roll.breakdown})
                    {roll.matchedIndex == null && liveRange
                      ? ` — no entry covers ${roll.total}`
                      : ''}
                  </small>
                )}
              </div>
              {landedEntry &&
                (landedEntry.nestedTableIds.length > 0 ||
                  landedEntry.nestedDeckIds.length > 0) && (
                <div className="chain-children">
                  {landedEntry.nestedTableIds.map((id) => (
                    <ChainNode
                      key={`t-${id}`}
                      kind="table"
                      entityId={id}
                      tables={tables}
                      decks={decks}
                      depth={1}
                      onError={handleError}
                    />
                  ))}
                  {landedEntry.nestedDeckIds.map((id) => (
                    <ChainNode
                      key={`d-${id}`}
                      kind="deck"
                      entityId={id}
                      tables={tables}
                      decks={decks}
                      depth={1}
                      onError={handleError}
                    />
                  ))}
                </div>
              )}

              <div className="editor-actions">
                <strong className="muted">Entries</strong>
                {liveRange && (
                  <span className="muted">
                    Split {liveRange.min}–{liveRange.max} evenly into&nbsp;
                    <Input
                      type="number"
                      min="1"
                      style={{ width: '4rem', display: 'inline-block' }}
                      value={splitCount}
                      onChange={(e) => setSplitCount(e.target.value)}
                    />
                    &nbsp;<Button type="button" variant="link" onClick={splitEvenly}>
                      Apply
                    </Button>
                  </span>
                )}
              </div>
              {draft.entries.map((entry, i) => (
                <div
                  key={i}
                  className={roll?.matchedIndex === i ? 'month-row hit-row' : 'month-row'}
                  style={{ alignItems: 'flex-start' }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <Input
                      type="number"
                      placeholder="from"
                      value={entry.minResult}
                      onChange={(e) => setEntry(i, { minResult: e.target.value })}
                      style={{ width: '5rem' }}
                    />
                    <Input
                      type="number"
                      placeholder="to"
                      value={entry.maxResult}
                      onChange={(e) => setEntry(i, { maxResult: e.target.value })}
                      style={{ width: '5rem' }}
                    />
                  </div>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <Textarea
                      ref={(el) => {
                        textareas.current[`entry-${i}`] = el;
                      }}
                      placeholder={`Outcome on ${entry.minResult || '?'}–${entry.maxResult || '?'} — [[wiki-links]] allowed`}
                      value={entry.body}
                      onChange={(e) => setEntry(i, { body: e.target.value })}
                      data-testid={`table-entry-body-${i}`}
                    />
                    {chainOpen === `entry-${i}` && chainPicker(`entry-${i}`)}
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <Button
                      type="button"
                      variant="link"
                      title="Chain other tables/decks into this outcome"
                      onClick={() =>
                        setChainOpen(chainOpen === `entry-${i}` ? null : `entry-${i}`)
                      }
                    >
                      ⛓{entry.nestedTableIds.length + entry.nestedDeckIds.length > 0
                        ? ` ${entry.nestedTableIds.length + entry.nestedDeckIds.length}`
                        : ''}
                    </Button>
                    <Button type="button" variant="link" onClick={() => setLinkTarget(`entry-${i}`)}>
                      [[link]]
                    </Button>
                    <Button
                      type="button"
                      variant="link"
                      className="text-destructive hover:text-destructive"
                      onClick={() =>
                        setDraft((d) => ({ ...d, entries: d.entries.filter((_, j) => j !== i) }))
                      }
                    >
                      ✕
                    </Button>
                  </div>
                </div>
              ))}
              <Button
                type="button"
                variant="link"
                onClick={() =>
                  setDraft((d) => ({
                    ...d,
                    entries: [
                      ...d.entries,
                      { minResult: '', maxResult: '', body: '', ...emptyChains },
                    ],
                  }))
                }
              >
                + Add entry
              </Button>
            </>
          ) : (
            <>
              <strong>{editingExisting ? 'Edit card deck' : 'New card deck'}</strong>
              <Input
                placeholder="Deck title"
                value={draft.title}
                onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                required
              />
              <Input
                placeholder="Description (optional)"
                value={draft.description}
                onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              />

              <strong className="muted">Cards</strong>
              <div className="editor-actions">
                <Button
                  type="button"
                  variant="outline"
                  disabled={draft.cards.length === 0}
                  onClick={drawCard}
                >
                  🃏 Draw a card
                </Button>
                {drawnIndex != null && draft.cards[drawnIndex] && (
                  <small className="muted">
                    Drew card {drawnIndex + 1}
                    {draft.cards[drawnIndex].title ? `: ${draft.cards[drawnIndex].title}` : ''}
                  </small>
                )}
              </div>
              {drawnCard &&
                (drawnCard.nestedTableIds.length > 0 ||
                  drawnCard.nestedDeckIds.length > 0) && (
                <div className="chain-children">
                  {drawnCard.nestedTableIds.map((id) => (
                    <ChainNode
                      key={`t-${id}`}
                      kind="table"
                      entityId={id}
                      tables={tables}
                      decks={decks}
                      depth={1}
                      onError={handleError}
                    />
                  ))}
                  {drawnCard.nestedDeckIds.map((id) => (
                    <ChainNode
                      key={`d-${id}`}
                      kind="deck"
                      entityId={id}
                      tables={tables}
                      decks={decks}
                      depth={1}
                      onError={handleError}
                    />
                  ))}
                </div>
              )}
              {draft.cards.map((card, i) => (
                <div
                  key={i}
                  className={drawnIndex === i ? 'month-row hit-row' : 'month-row'}
                  style={{ alignItems: 'flex-start' }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <Button
                      type="button"
                      variant="link"
                      title="Move up"
                      onClick={() => moveCard(i, -1)}
                      disabled={i === 0}
                    >
                      ↑
                    </Button>
                    <Button
                      type="button"
                      variant="link"
                      title="Move down"
                      onClick={() => moveCard(i, 1)}
                      disabled={i === draft.cards.length - 1}
                    >
                      ↓
                    </Button>
                  </div>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <Input
                      placeholder="Card title (optional)"
                      value={card.title}
                      onChange={(e) =>
                        setDraft((d) => ({
                          ...d,
                          cards: d.cards.map((c, j) => (j === i ? { ...c, title: e.target.value } : c)),
                        }))
                      }
                    />
                    <Textarea
                      ref={(el) => {
                        textareas.current[`card-${i}`] = el;
                      }}
                      placeholder="Card body — [[wiki-links]] allowed"
                      value={card.body}
                      onChange={(e) =>
                        setDraft((d) => ({
                          ...d,
                          cards: d.cards.map((c, j) => (j === i ? { ...c, body: e.target.value } : c)),
                        }))
                      }
                    />
                    {chainOpen === `card-${i}` && chainPicker(`card-${i}`)}
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <Button
                      type="button"
                      variant="link"
                      title="Chain other tables/decks into this card"
                      onClick={() =>
                        setChainOpen(chainOpen === `card-${i}` ? null : `card-${i}`)
                      }
                    >
                      ⛓{card.nestedTableIds.length + card.nestedDeckIds.length > 0
                        ? ` ${card.nestedTableIds.length + card.nestedDeckIds.length}`
                        : ''}
                    </Button>
                    <Button type="button" variant="link" onClick={() => setLinkTarget(`card-${i}`)}>
                      [[link]]
                    </Button>
                    <Button
                      type="button"
                      variant="link"
                      className="text-destructive hover:text-destructive"
                      onClick={() => {
                        setDraft((d) => ({ ...d, cards: d.cards.filter((_, j) => j !== i) }));
                        setDrawnIndex(null);
                      }}
                    >
                      ✕
                    </Button>
                  </div>
                </div>
              ))}
              <Button
                type="button"
                variant="link"
                onClick={() =>
                  setDraft((d) => ({
                    ...d,
                    cards: [...d.cards, { title: '', body: '', ...emptyChains }],
                  }))
                }
              >
                + Add card
              </Button>
            </>
          )}

          <div className="editor-actions">
            <Button type="submit" disabled={saveState === 'saving'} data-testid="table-save-button">
              {saveState === 'saving' && <Spinner data-icon="inline-start" />}
              {saveState === 'saving'
                ? 'Saving…'
                : saveState === 'saved'
                  ? 'Saved'
                  : editingExisting
                    ? draft.kind === 'table'
                      ? 'Save table'
                      : 'Save deck'
                    : draft.kind === 'table'
                      ? 'Create table'
                      : 'Create deck'}
            </Button>
            {editingExisting && (
              <Button
                type="button"
                variant="link"
                onClick={() => {
                  const saved = draft.kind === 'table' ? printTable : printDeck;
                  if (saved) edit(draft.kind, saved);
                  else setMode('read');
                }}
              >
                Cancel
              </Button>
            )}
            {(printTable || printDeck) && (
              <Button
                type="button"
                variant="outline"
                onClick={() => setPrinting(true)}
                data-testid="table-print-button"
              >
                🖨 Print
              </Button>
            )}
            {editingExisting && (
              <ConfirmDeleteDialog
                trigger={
                  <Button type="button" variant="link" className="text-destructive hover:text-destructive">
                    Delete
                  </Button>
                }
                title={draft.kind === 'table' ? 'Delete table?' : 'Delete deck?'}
                description={`This permanently deletes "${draft.title}" and cannot be undone.`}
                onConfirm={() => void remove()}
              />
            )}
          </div>
        </form>
        )}

        {mode === 'read' && editingExisting && (printTable || printDeck) && (
          <article className="article-read">
            <div className="article-read-head">
              <h2>{printTable ? `🎲 ${printTable.title}` : `🃏 ${printDeck!.title}`}</h2>
              <div className="editor-actions">
                <Button type="button" onClick={() => setMode('edit')}>
                  Edit
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setPrinting(true)}
                  data-testid="table-print-button"
                >
                  🖨 Print
                </Button>
              </div>
            </div>
            {(printTable?.description || printDeck?.description) && (
              <p className="muted">{printTable?.description ?? printDeck?.description}</p>
            )}

            {printTable && (
              <>
                <div className="editor-actions">
                  <Button type="button" variant="outline" onClick={() => void doRoll()}>
                    🎲 Roll ({printTable.diceExpression})
                  </Button>
                  {roll && (
                    <small className="muted">
                      Rolled <strong>{roll.total}</strong> ({roll.breakdown})
                      {roll.matchedIndex == null ? ` — no entry covers ${roll.total}` : ''}
                    </small>
                  )}
                </div>
                {landedEntry &&
                  (landedEntry.nestedTableIds.length > 0 || landedEntry.nestedDeckIds.length > 0) && (
                  <div className="chain-children">
                    {landedEntry.nestedTableIds.map((id) => (
                      <ChainNode
                        key={`t-${id}`}
                        kind="table"
                        entityId={id}
                        tables={tables}
                        decks={decks}
                        depth={1}
                        onError={handleError}
                      />
                    ))}
                    {landedEntry.nestedDeckIds.map((id) => (
                      <ChainNode
                        key={`d-${id}`}
                        kind="deck"
                        entityId={id}
                        tables={tables}
                        decks={decks}
                        depth={1}
                        onError={handleError}
                      />
                    ))}
                  </div>
                )}
                {tableEntriesGrid(printTable.entries)}
              </>
            )}

            {printDeck && (
              <>
                <div className="editor-actions">
                  <Button type="button" variant="outline" disabled={printDeck.cards.length === 0} onClick={drawCard}>
                    🃏 Draw a card
                  </Button>
                  {drawnIndex != null && printDeck.cards[drawnIndex] && (
                    <small className="muted">
                      Drew card {drawnIndex + 1}
                      {printDeck.cards[drawnIndex].title ? `: ${printDeck.cards[drawnIndex].title}` : ''}
                    </small>
                  )}
                </div>
                {drawnCard &&
                  (drawnCard.nestedTableIds.length > 0 || drawnCard.nestedDeckIds.length > 0) && (
                  <div className="chain-children">
                    {drawnCard.nestedTableIds.map((id) => (
                      <ChainNode
                        key={`t-${id}`}
                        kind="table"
                        entityId={id}
                        tables={tables}
                        decks={decks}
                        depth={1}
                        onError={handleError}
                      />
                    ))}
                    {drawnCard.nestedDeckIds.map((id) => (
                      <ChainNode
                        key={`d-${id}`}
                        kind="deck"
                        entityId={id}
                        tables={tables}
                        decks={decks}
                        depth={1}
                        onError={handleError}
                      />
                    ))}
                  </div>
                )}
                {deckCardSheet(printDeck.cards)}
              </>
            )}
          </article>
        )}
      </div>

      <ArticleLinkPicker
        worldId={worldId}
        open={linkTarget != null}
        onPick={insertLink}
        onClose={() => setLinkTarget(null)}
      />

      {(printTable || printDeck) && printing && (
        <NewWindowPortal
          title={`Print — ${printTable?.title ?? printDeck?.title ?? ''}`}
          onClose={() => setPrinting(false)}
        >
          <div className="print-toolbar">
            <strong>{printTable ? 'Roll table' : 'Card deck'}</strong>
            <PrintOptionsMenu opts={printOpts} onChange={setPrintOpts} />
            <span className="print-toolbar-spacer" />
            <PrintButton />
            <Button variant="link" onClick={() => setPrinting(false)}>
              Close
            </Button>
          </div>
          <div className="print-doc" {...printDocProps}>
            <section className="print-cover">
              <h1>{printTable?.title ?? printDeck?.title}</h1>
              {(printTable?.description || printDeck?.description) && (
                <p className="print-subtitle">{printTable?.description ?? printDeck?.description}</p>
              )}
            </section>

            {printTable && (
              <section className="print-roll-table">
                <p className="print-kicker">
                  {printTable.diceExpression} ({printTable.minResult}–{printTable.maxResult})
                </p>
                {tableEntriesGrid(printTable.entries)}
              </section>
            )}

            {printDeck && (
              <section className="print-map-section">
                {deckCardSheet(printDeck.cards)}
              </section>
            )}

            {chainedForPrint.tables.map((t) => (
              <section key={t.id} className="print-roll-table">
                <h2>{t.title}</h2>
                <p className="print-kicker">
                  {t.diceExpression} ({t.minResult}–{t.maxResult})
                </p>
                {tableEntriesGrid(t.entries)}
              </section>
            ))}
            {chainedForPrint.decks.length > 0 && (
              <section className="print-map-section">
                {chainedForPrint.decks.map((d) => (
                  <div key={d.id} style={{ marginBottom: '1rem' }}>
                    <h2>{d.title}</h2>
                    {deckCardSheet(d.cards)}
                  </div>
                ))}
              </section>
            )}
          </div>
        </NewWindowPortal>
      )}
    </div>
  );
}

/** Stored chains may form cycles, so live rolling stops at this depth. */
const MAX_CHAIN_DEPTH = 8;

interface ChainNodeProps {
  kind: 'table' | 'deck';
  entityId: string;
  tables: RollTable[];
  decks: CardDeck[];
  depth: number;
  onError: (err: unknown) => void;
}

/**
 * One chained table/deck as a live sub-roller (FR-41): a roll lands on a row,
 * a draw picks a card, and whatever that row/card chains in recurses below.
 * Cycles are cut by the depth cap; deleted targets render as a note.
 */
function ChainNode({ kind, entityId, tables, decks, depth, onError }: ChainNodeProps) {
  const [roll, setRoll] = useState<{ total: number; breakdown: string } | null>(null);
  const [drawnIndex, setDrawnIndex] = useState<number | null>(null);

  const table = kind === 'table' ? tables.find((t) => t.id === entityId) : undefined;
  const deck = kind === 'deck' ? decks.find((d) => d.id === entityId) : undefined;

  if (!table && !deck) {
    return <div className="chain-node chain-missing">Chained content was deleted.</div>;
  }
  if (depth > MAX_CHAIN_DEPTH) {
    return <div className="chain-node chain-muted">…</div>;
  }

  const children = (
    nestedTableIds: string[],
    nestedDeckIds: string[],
    keyPrefix: string,
  ) => (
    <div className="chain-children">
      {nestedTableIds.map((id) => (
        <ChainNode
          key={`${keyPrefix}-t-${id}`}
          kind="table"
          entityId={id}
          tables={tables}
          decks={decks}
          depth={depth + 1}
          onError={onError}
        />
      ))}
      {nestedDeckIds.map((id) => (
        <ChainNode
          key={`${keyPrefix}-d-${id}`}
          kind="deck"
          entityId={id}
          tables={tables}
          decks={decks}
          depth={depth + 1}
          onError={onError}
        />
      ))}
    </div>
  );

  if (table) {
    const landed =
      roll != null
        ? table.entries.find(
            (e) =>
              e.minResult != null &&
              e.maxResult != null &&
              e.minResult <= roll.total &&
              roll.total <= e.maxResult,
          ) ?? table.entries.find((e) => e.minResult == null && e.maxResult == null)
        : undefined;
    const hasChains =
      landed != null && (landed.nestedTableIds.length > 0 || landed.nestedDeckIds.length > 0);
    return (
      <div className="chain-node">
        <div className="editor-actions">
          <span>
            🎲 <strong>{table.title}</strong>{' '}
            <small className="muted">{table.diceExpression}</small>
          </span>
          <Button
            type="button"
            variant="link"
            onClick={() => {
              diceApi
                .roll(table.diceExpression)
                .then((result) => setRoll({ total: result.total, breakdown: result.breakdown }))
                .catch(onError);
            }}
          >
            Roll
          </Button>
          {roll && (
            <small className="muted">
              → <strong>{roll.total}</strong> ({roll.breakdown})
            </small>
          )}
        </div>
        {landed && <p className="chain-outcome">{landed.body}</p>}
        {hasChains &&
          children(landed!.nestedTableIds, landed!.nestedDeckIds, `r${table.id}`)}
      </div>
    );
  }

  // Deck branch.
  const card = drawnIndex != null ? deck!.cards[drawnIndex] : undefined;
  return (
    <div className="chain-node">
      <div className="editor-actions">
        <span>
          🃏 <strong>{deck!.title}</strong>
        </span>
        <Button
          type="button"
          variant="link"
          disabled={deck!.cards.length === 0}
          onClick={() => setDrawnIndex(Math.floor(Math.random() * deck!.cards.length))}
        >
          Draw
        </Button>
        {card && (
          <small className="muted">
            → {card.title ? <strong>{card.title}</strong> : 'Card'}
          </small>
        )}
      </div>
      {card && <p className="chain-outcome">{card.body}</p>}
      {card && (card.nestedTableIds.length > 0 || card.nestedDeckIds.length > 0) &&
        children(card.nestedTableIds, card.nestedDeckIds, `c${card.id}`)}
    </div>
  );
}
