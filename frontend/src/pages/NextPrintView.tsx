import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { NewWindowPortal, PrintButton, useNewWindowContainer } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { Input } from '../components/ui/input';
import { CheckTreeNode, CheckTreeRow } from '../components/CheckTree';
import {
  articlesApi,
  mapsApi,
  pinsApi,
  rollTablesApi,
  cardDecksApi,
  RollTable,
  CardDeck,
  Article,
  Campaign,
  MapPin,
  WorldMap,
} from '../api/client';
import { renderLinkedMarkdown } from '../lib/markdown';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so the meaningfully persistent "whole world" scope goes through this sentinel.
const NONE_VALUE = '__none__';

/**
 * Transitive closure of wiki-linked article ids starting from seedId, by
 * scraping data-article-id off each article's already-rendered bodyHtml
 * (WikiLinker only emits that attribute for a resolved link — a broken
 * [[link]] renders as a <span>, so it's automatically excluded here with no
 * extra logic). The visited set makes this terminate on its own even
 * through a link cycle, so no depth cap is needed.
 */
function linkedClosure(seedId: string, articles: Article[]): Set<string> {
  const byId = new Map(articles.map((a) => [a.id, a]));
  const visited = new Set<string>([seedId]);
  const queue = [seedId];
  while (queue.length) {
    const current = byId.get(queue.shift()!);
    const html = current?.bodyHtml ?? '';
    for (const m of html.matchAll(/data-article-id="([0-9a-f-]{36})"/g)) {
      const id = m[1];
      if (!visited.has(id) && byId.has(id)) {
        visited.add(id);
        queue.push(id);
      }
    }
  }
  return visited;
}

/**
 * Article nodes for the include-tree, nested by parentArticleId (ADR-0080).
 * An article whose parent isn't in this document's article list (out of
 * scope, or simply not loaded) is promoted to a root of its own — same
 * fallback rule as the sidebar tree.
 */
function buildArticleCheckNodes(articles: Article[]): CheckTreeNode[] {
  const byId = new Map(articles.map((a) => [a.id, a]));
  const childrenByParent = new Map<string, Article[]>();
  const roots: Article[] = [];
  for (const a of articles) {
    if (a.parentArticleId && byId.has(a.parentArticleId)) {
      const list = childrenByParent.get(a.parentArticleId) ?? [];
      list.push(a);
      childrenByParent.set(a.parentArticleId, list);
    } else {
      roots.push(a);
    }
  }
  const build = (a: Article): CheckTreeNode => ({
    id: `article:${a.id}`,
    label: a.title,
    children: (childrenByParent.get(a.id) ?? [])
      .sort((x, y) => x.title.localeCompare(y.title))
      .map(build),
  });
  return roots.sort((a, b) => a.title.localeCompare(b.title)).map(build);
}

/** True if a node's own label, or any descendant's, matches the filter. */
function articleTreeMatches(node: CheckTreeNode, filter: string): boolean {
  if (node.label.toLowerCase().includes(filter)) return true;
  return node.children.some((c) => articleTreeMatches(c, filter));
}

function filterArticleTree(nodes: CheckTreeNode[], filter: string): CheckTreeNode[] {
  if (!filter) return nodes;
  const f = filter.toLowerCase();
  return nodes.filter((n) => articleTreeMatches(n, f));
}

/**
 * A separate component (not inlined in PrintView) so useNewWindowContainer()
 * runs as a descendant of NewWindowPortal's provider — PrintView itself
 * renders *above* NewWindowPortal in the tree, so calling the hook there
 * would miss the popped-out window's container and portal into the wrong
 * (hidden, main-app) window instead.
 */
function ScopeSelect({
  scope,
  campaigns,
  onChange,
  disabled,
}: {
  scope: string;
  campaigns: Campaign[];
  onChange: (scope: string) => void;
  disabled?: boolean;
}) {
  const container = useNewWindowContainer();
  return (
    <Select
      value={scope || NONE_VALUE}
      onValueChange={(v) => onChange(v === NONE_VALUE ? '' : v)}
      disabled={disabled}
    >
      <SelectTrigger>
        <SelectValue />
      </SelectTrigger>
      <SelectContent container={container}>
        <SelectItem value={NONE_VALUE}>Whole world</SelectItem>
        {campaigns.map((c) => (
          <SelectItem key={c.id} value={c.id}>
            {c.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

interface Props {
  worldId: string;
  worldName: string;
  campaigns: Campaign[];
  onClose: () => void;
  onError: (err: unknown) => void;
}

interface PrintableMap {
  map: WorldMap;
  pins: MapPin[];
}

/**
 * Full-screen print/PDF view (ADR-0035). Renders a clean black-on-white document
 * — cover, optional contents, articles with embedded images, and annotated maps —
 * then hands off to the browser's native print / Save-as-PDF (PrintButton).
 * /next's own fork of PrintView (ADR-0106) — ownership-only, no markup
 * changes: its classes are print-media styling, not screen chrome, and it
 * already uses the same NewWindowPortal/PrintOptionsMenu/PrintButton shared
 * building blocks every other /next print flow uses.
 */
export function NextPrintView({ worldId, worldName, campaigns, onClose, onError }: Props) {
  const api = useMemo(() => articlesApi(worldId), [worldId]);
  const maps = useMemo(() => mapsApi(worldId), [worldId]);
  const tablesApi = useMemo(() => rollTablesApi(worldId), [worldId]);
  const decksApi = useMemo(() => cardDecksApi(worldId), [worldId]);
  // '' = whole world; otherwise restrict to a campaign's referenced articles.
  const [scope, setScope] = useState('');
  const [includeContents, setIncludeContents] = useState(true);
  const [loading, setLoading] = useState(true);
  const [articles, setArticles] = useState<Article[]>([]);
  const [printableMaps, setPrintableMaps] = useState<PrintableMap[]>([]);
  const [rollTables, setRollTables] = useState<RollTable[]>([]);
  const [cardDecks, setCardDecks] = useState<CardDeck[]>([]);
  const { opts: printOpts, setOpts: setPrintOpts, docProps: printDocProps } = usePrintOptions();
  // Per-item inclusion, on top of the scope above (all included by default).
  // Composite `${kind}:${id}` keys — see components/CheckTree.tsx.
  const [excludedIds, setExcludedIds] = useState<Set<string>>(new Set());
  const [articleFilter, setArticleFilter] = useState('');
  // "Linked from one article" scope: overrides `scope` when set - the
  // document becomes the seed article plus the transitive closure of
  // everything it [[links]] to, pre-checked into the exclude checklist above.
  const [seedArticleId, setSeedArticleId] = useState<string | null>(null);
  const [seedPickerOpen, setSeedPickerOpen] = useState(false);
  const [seedFilter, setSeedFilter] = useState('');
  // Every article in the world, for the seed picker - independent of the
  // current scope/campaign filter, since a link target can live anywhere.
  const [allArticles, setAllArticles] = useState<{ id: string; title: string }[]>([]);
  // Which seed the exclude checklist's defaults were last computed for, so
  // an unrelated re-load (e.g. toggling print options) doesn't wipe out
  // exclusions the GM already customized after picking a seed.
  const lastSeedAppliedRef = useRef<string | null>(null);

  function toggleIds(ids: string[], checked: boolean) {
    setExcludedIds((prev) => {
      const next = new Set(prev);
      for (const id of ids) {
        if (checked) next.delete(id);
        else next.add(id);
      }
      return next;
    });
  }

  const shownArticles = useMemo(
    () => articles.filter((a) => !excludedIds.has(`article:${a.id}`)),
    [articles, excludedIds],
  );
  const shownMaps = useMemo(
    () => printableMaps.filter(({ map }) => !excludedIds.has(`map:${map.id}`)),
    [printableMaps, excludedIds],
  );
  const shownRollTables = useMemo(
    () => rollTables.filter((t) => !excludedIds.has(`rollTable:${t.id}`)),
    [rollTables, excludedIds],
  );
  const shownCardDecks = useMemo(
    () => cardDecks.filter((d) => !excludedIds.has(`cardDeck:${d.id}`)),
    [cardDecks, excludedIds],
  );

  const articleTree = useMemo(() => buildArticleCheckNodes(articles), [articles]);
  const filteredArticleTree = useMemo(
    () => filterArticleTree(articleTree, articleFilter),
    [articleTree, articleFilter],
  );
  const mapNodes: CheckTreeNode[] = useMemo(
    () => printableMaps.map(({ map }) => ({ id: `map:${map.id}`, label: map.name, children: [] })),
    [printableMaps],
  );
  const rollTableNodes: CheckTreeNode[] = useMemo(
    () => rollTables.map((t) => ({ id: `rollTable:${t.id}`, label: t.title, children: [] })),
    [rollTables],
  );
  const cardDeckNodes: CheckTreeNode[] = useMemo(
    () => cardDecks.map((d) => ({ id: `cardDeck:${d.id}`, label: d.title, children: [] })),
    [cardDecks],
  );
  const treeRoots = useMemo(
    () => [...filteredArticleTree, ...mapNodes, ...rollTableNodes, ...cardDeckNodes],
    [filteredArticleTree, mapNodes, rollTableNodes, cardDeckNodes],
  );

  const seedTitle = seedArticleId ? allArticles.find((a) => a.id === seedArticleId)?.title ?? '' : '';
  const scopeName = scope ? campaigns.find((c) => c.id === scope)?.name ?? '' : '';
  // Maps only print at whole-world scope, so `articles` covers every linked pin.
  const articleTitleById = useMemo(() => new Map(articles.map((a) => [a.id, a.title])), [articles]);
  // Table/deck bodies resolve [[wiki-links]] against this booklet's articles.
  const linkLookup = useMemo(() => {
    const byName = new Map<string, string>();
    for (const a of articles) {
      if (!byName.has(a.title.toLowerCase())) byName.set(a.title.toLowerCase(), a.title);
      if (!byName.has(a.slug.toLowerCase())) byName.set(a.slug.toLowerCase(), a.title);
    }
    return (name: string) => byName.get(name) ?? null;
  }, [articles]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      // A seed's linked closure can reach outside any single campaign, so
      // seed mode always pulls the whole world regardless of `scope`.
      const summaries = await api.list(
        !seedArticleId && scope ? { campaignId: scope } : undefined,
      );
      // Fetch each rendered body; sort into a stable A–Z booklet order.
      const full = await Promise.all(summaries.map((s) => api.get(s.id)));
      full.sort((a, b) => a.title.localeCompare(b.title));
      setArticles(full);

      if (seedArticleId && lastSeedAppliedRef.current !== seedArticleId) {
        const closure = linkedClosure(seedArticleId, full);
        setExcludedIds(
          new Set(full.filter((a) => !closure.has(a.id)).map((a) => `article:${a.id}`)),
        );
        lastSeedAppliedRef.current = seedArticleId;
      } else if (!seedArticleId && lastSeedAppliedRef.current !== null) {
        setExcludedIds(new Set());
        lastSeedAppliedRef.current = null;
      }

      const wholeWorldScope = !scope && !seedArticleId;
      if (wholeWorldScope) {
        const [list, t, d] = await Promise.all([maps.list(), tablesApi.list(), decksApi.list()]);
        const withPins = await Promise.all(
          list.map(async (m) => ({ map: m, pins: await pinsApi(worldId, m.id).list() })),
        );
        setPrintableMaps(withPins);
        setRollTables(t);
        setCardDecks(d);
      } else {
        setPrintableMaps([]);
        setRollTables([]);
        setCardDecks([]);
      }
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, maps, tablesApi, decksApi, worldId, scope, seedArticleId, onError]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    api
      .list()
      .then((list) => setAllArticles(list.map((a) => ({ id: a.id, title: a.title }))))
      .catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId]);

  const today = new Date().toLocaleDateString();

  return (
    <NewWindowPortal title={`Print — ${worldName}`} onClose={onClose}>
      <div className="print-toolbar">
        <strong>Print / PDF</strong>
        <label>
          Scope{' '}
          <ScopeSelect
            scope={scope}
            campaigns={campaigns}
            onChange={setScope}
            disabled={!!seedArticleId}
          />
        </label>
        <label
          className="print-check"
          title="Start the document from one article plus everything it links to, transitively"
        >
          <Checkbox
            checked={!!seedArticleId || seedPickerOpen}
            onCheckedChange={(checked) => {
              if (checked) {
                setSeedPickerOpen(true);
              } else {
                setSeedArticleId(null);
                setSeedPickerOpen(false);
                setSeedFilter('');
              }
            }}
          />
          Linked from one article
        </label>
        <label className="print-check">
          <Checkbox
            checked={includeContents}
            onCheckedChange={(checked) => setIncludeContents(checked === true)}
          />
          Contents
        </label>
        <PrintOptionsMenu opts={printOpts} onChange={setPrintOpts} />
        <span className="print-toolbar-spacer" />
        <PrintButton disabled={loading} />
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>

      {seedPickerOpen && (
        <div className="print-toolbar map-print-layers">
          {seedArticleId ? (
            <>
              <span className="muted">Seed article:</span>
              <strong>{seedTitle}</strong>
              <Button variant="link" onClick={() => setSeedArticleId(null)}>
                Change
              </Button>
            </>
          ) : (
            <>
              <span className="muted">Pick a seed article:</span>
              <Input
                className="article-picker-filter"
                placeholder="Filter…"
                value={seedFilter}
                onChange={(e) => setSeedFilter(e.target.value)}
                autoFocus
              />
              {allArticles
                .filter((a) => a.title.toLowerCase().includes(seedFilter.toLowerCase()))
                .slice(0, 30)
                .map((a) => (
                  <Button
                    key={a.id}
                    type="button"
                    variant="outline"
                    className="print-check"
                    onClick={() => {
                      setSeedArticleId(a.id);
                      setSeedFilter('');
                    }}
                  >
                    {a.title}
                  </Button>
                ))}
            </>
          )}
        </div>
      )}

      {!loading && treeRoots.length > 0 && (
        <div className="print-toolbar map-print-layers">
          <span className="muted">Include:</span>
          <Input
            className="article-picker-filter"
            placeholder="Filter articles…"
            value={articleFilter}
            onChange={(e) => setArticleFilter(e.target.value)}
          />
          <ul className="check-tree">
            {treeRoots.map((node) => (
              <CheckTreeRow key={node.id} node={node} excludedIds={excludedIds} onToggle={toggleIds} />
            ))}
          </ul>
        </div>
      )}

      <div className="print-doc" {...printDocProps}>
        <section className="print-cover">
          <h1>{worldName}</h1>
          <p className="print-subtitle">
            {seedArticleId
              ? `Article: ${seedTitle} + linked`
              : scopeName
                ? `Campaign: ${scopeName}`
                : 'World compendium'}
          </p>
          <p className="print-date">{today}</p>
        </section>

        {loading && <p className="print-status">Preparing document…</p>}

        {!loading && includeContents && shownArticles.length > 0 && (
          <section className="print-contents">
            <h2>Contents</h2>
            <ol>
              {shownArticles.map((a) => (
                <li key={a.id}>{a.title}</li>
              ))}
            </ol>
          </section>
        )}

        {!loading &&
          shownArticles.map((a) => (
            <article key={a.id} className="print-article">
              <h1>{a.title}</h1>
              <p className="print-kicker">{a.template.toLowerCase()}</p>
              {/* eslint-disable-next-line react/no-danger */}
              <div
                className="print-body preview-body"
                dangerouslySetInnerHTML={{ __html: a.bodyHtml || '<p><em>(empty)</em></p>' }}
              />
            </article>
          ))}

        {!loading && articles.length === 0 && (
          <p className="print-status">
            {scope ? 'This campaign references no articles yet.' : 'No articles to print.'}
          </p>
        )}

        {!loading && articles.length > 0 && shownArticles.length === 0 && (
          <p className="print-status">Every article is excluded — check one above to print it.</p>
        )}

        {!loading &&
          shownMaps.map(({ map, pins }) => (
            <section key={map.id} className="print-map-section">
              <h1>{map.name}</h1>
              {map.imageUrl && (
                <div className="print-map-figure">
                  <img src={map.imageUrl} alt={map.name} />
                  {pins.map((p, i) => (
                    <span
                      key={p.id}
                      className="print-map-marker"
                      style={{ left: `${p.x * 100}%`, top: `${p.y * 100}%` }}
                    >
                      {i + 1}
                    </span>
                  ))}
                </div>
              )}
              {pins.length > 0 && (
                <ol className="print-map-legend">
                  {pins.map((p) => (
                    <li key={p.id}>
                      {p.label || (p.articleId ? articleTitleById.get(p.articleId) : '') || 'Unlabeled pin'}
                    </li>
                  ))}
                </ol>
              )}
            </section>
          ))}

        {!loading && shownRollTables.length > 0 && (
          <section className="print-map-section">
            <h1>Roll tables</h1>
            {shownRollTables.map((t) => (
              <div key={t.id} className="print-roll-table">
                <h2>{t.title}</h2>
                <p className="print-kicker">
                  {t.diceExpression} ({t.minResult}–{t.maxResult})
                </p>
                <div className="table-scroll">
                <table className="print-table-grid">
                  <tbody>
                    {t.entries.map((e) => (
                      <tr key={e.id}>
                        <td className="print-table-range">
                          {e.minResult != null && e.maxResult != null
                            ? `${e.minResult}–${e.maxResult}`
                            : 'else'}
                        </td>
                        {/* eslint-disable-next-line react/no-danger */}
                        <td
                          className="preview-body"
                          dangerouslySetInnerHTML={{
                            __html: renderLinkedMarkdown(e.body, linkLookup),
                          }}
                        />
                      </tr>
                    ))}
                  </tbody>
                </table>
                </div>
              </div>
            ))}
          </section>
        )}

        {!loading && shownCardDecks.length > 0 && (
          <section className="print-map-section">
            <h1>Card decks</h1>
            {shownCardDecks.map((d) => (
              <div key={d.id} style={{ marginBottom: '1rem' }}>
                <h2>{d.title}</h2>
                <div className="card-sheet">
                  {d.cards.map((c) => (
                    <div key={c.id} className="deck-card">
                      {c.title && <div className="deck-card-name">{c.title}</div>}
                      {/* eslint-disable-next-line react/no-danger */}
                      <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderLinkedMarkdown(c.body, linkLookup) }} />
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </section>
        )}
      </div>
    </NewWindowPortal>
  );
}
