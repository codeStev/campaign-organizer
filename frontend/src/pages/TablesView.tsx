import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  rollTablesApi,
  cardDecksApi,
  RollTable,
  CardDeck,
  ApiError,
} from '../api/client';
import { diceRange, DiceRange } from '../lib/dice';
import { Button } from '../components/ui/button';
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
}

interface DeckCardDraft {
  title: string;
  body: string;
}

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
  entries: [{ minResult: '', maxResult: '', body: '' }],
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
    cards: d.cards.map((c) => ({ title: c.title ?? '', body: c.body })),
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
    rows.push({ minResult: String(cursor), maxResult: String(cursor + span - 1), body: '' });
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

  function setEntry(index: number, patch: Partial<TableEntryDraft>) {
    setDraft((d) => ({
      ...d,
      entries: d.entries.map((e, i) => (i === index ? { ...e, ...patch } : e)),
    }));
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

  /** Evenly cover the expression's whole range with n entries. */
  function splitEvenly() {
    if (!liveRange) return;
    const count = Math.min(Math.max(Number(splitCount) || 1, 1), liveRange.max - liveRange.min + 1);
    setDraft((d) => ({ ...d, entries: evenSplitRows(liveRange, count) }));
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
        })),
      };
      try {
        const saved =
          draft.id != null ? await tablesApi.update(draft.id, body) : await tablesApi.create(body);
        navigate(`/worlds/${worldId}/tables/table/${saved.id}`);
        await refresh();
      } catch (err) {
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
        cards: draft.cards.map((c) => ({ title: c.title || undefined, body: c.body })),
      };
      try {
        const saved =
          draft.id != null ? await decksApi.update(draft.id, body) : await decksApi.create(body);
        navigate(`/worlds/${worldId}/tables/deck/${saved.id}`);
        await refresh();
      } catch (err) {
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
            setDraft({ ...EMPTY_DRAFT, kind: 'table', entries: [{ minResult: '', maxResult: '', body: '' }] });
            navigate(`/worlds/${worldId}/tables`);
          }}
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
                <span>
                  🎲 {t.title}
                </span>
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
            setDraft({ ...EMPTY_DRAFT, kind: 'deck', entries: [], cards: [{ title: '', body: '' }] });
            navigate(`/worlds/${worldId}/tables`);
          }}
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
                <span>🃏 {d.title}</span>
                <small className="muted">{d.cards.length} cards</small>
              </button>
            </li>
          ))}
          {!loading && decks.length === 0 && <li className="muted">No card decks yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        <form className="card" onSubmit={save}>
          {draft.kind === 'table' ? (
            <>
              <strong>{editingExisting ? 'Edit roll table' : 'New roll table'}</strong>
              <Input
                placeholder="Table title"
                value={draft.title}
                onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                required
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
                />
              </label>
              <small className="muted">
                {liveRange
                  ? `Possible results: ${liveRange.min}–${liveRange.max}`
                  : 'Enter any dice combination, e.g. 2d6 or 4d6kh3'}
              </small>

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
                <div key={i} className="month-row" style={{ alignItems: 'flex-start' }}>
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
                  <Textarea
                    placeholder={`Outcome on ${entry.minResult || '?'}–${entry.maxResult || '?'} — [[wiki-links]] allowed`}
                    value={entry.body}
                    onChange={(e) => setEntry(i, { body: e.target.value })}
                  />
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
              ))}
              <Button
                type="button"
                variant="link"
                onClick={() =>
                  setDraft((d) => ({
                    ...d,
                    entries: [...d.entries, { minResult: '', maxResult: '', body: '' }],
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
              {draft.cards.map((card, i) => (
                <div key={i} className="month-row" style={{ alignItems: 'flex-start' }}>
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
                      placeholder="Card body — [[wiki-links]] allowed"
                      value={card.body}
                      onChange={(e) =>
                        setDraft((d) => ({
                          ...d,
                          cards: d.cards.map((c, j) => (j === i ? { ...c, body: e.target.value } : c)),
                        }))
                      }
                    />
                  </div>
                  <Button
                    type="button"
                    variant="link"
                    className="text-destructive hover:text-destructive"
                    onClick={() =>
                      setDraft((d) => ({ ...d, cards: d.cards.filter((_, j) => j !== i) }))
                    }
                  >
                    ✕
                  </Button>
                </div>
              ))}
              <Button
                type="button"
                variant="link"
                onClick={() =>
                  setDraft((d) => ({ ...d, cards: [...d.cards, { title: '', body: '' }] }))
                }
              >
                + Add card
              </Button>
            </>
          )}

          <div className="editor-actions">
            <Button type="submit">
              {editingExisting
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
                className="text-destructive hover:text-destructive"
                onClick={() => void remove()}
              >
                Delete
              </Button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
