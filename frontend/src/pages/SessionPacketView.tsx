import { useEffect, useMemo, useState } from 'react';
import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';
import { Button } from '../components/ui/button';
import { Checkbox } from '../components/ui/checkbox';
import { CheckTreeNode, CheckTreeRow } from '../components/CheckTree';
import {
  sessionsApi,
  fieldTemplatesApi,
  SessionPacket,
  FieldTemplate,
  PacketArticle,
  Statblock,
} from '../api/client';
import { orderedStatEntries } from '../lib/statblockDisplay';
import { renderMarkdown } from '../lib/markdown';

/** Every article id within an article's subtree (itself + descendants). */
function articleSubtreeIds(articleId: string, childrenByParent: Map<string, PacketArticle[]>): string[] {
  const kids = childrenByParent.get(articleId) ?? [];
  return [articleId, ...kids.flatMap((k) => articleSubtreeIds(k.id, childrenByParent))];
}

/** An article node, nesting its child articles then any statblocks tagged to it. */
function buildArticleNode(
  article: PacketArticle,
  childrenByParent: Map<string, PacketArticle[]>,
  statblocksByArticleId: Map<string, Statblock[]>,
): CheckTreeNode {
  const childArticleNodes = (childrenByParent.get(article.id) ?? []).map((a) =>
    buildArticleNode(a, childrenByParent, statblocksByArticleId),
  );
  const childStatblockNodes = (statblocksByArticleId.get(article.id) ?? []).map((sb) => ({
    id: `statblock:${sb.id}`,
    label: sb.name,
    children: [] as CheckTreeNode[],
  }));
  return {
    id: `article:${article.id}`,
    label: article.title,
    children: [...childArticleNodes, ...childStatblockNodes],
  };
}

/**
 * Builds the packet's include-tree (ADR-0080 nesting + the "fit non-article
 * entries into the hierarchy" request): article subtrees nest by
 * parentArticleId (an article whose parent isn't in this packet is promoted
 * to a subtree root); each beat "claims" the article-root subtrees its
 * articleIds reach into (root or any descendant) and gets them as children,
 * so unchecking a beat cascades to everything it pulled in; a subtree no
 * beat claims surfaces as its own top-level node instead of vanishing.
 * Statblocks nest under the exact article they're tagged to (wherever that
 * article appears — duplicated across multiple claiming beats, same shared
 * id so their checkbox state stays in sync); an untagged statblock is
 * top-level. Maps, roll tables, card decks, and handouts carry no article
 * link in these DTOs, so they're always top-level.
 */
function buildPacketTree(packet: SessionPacket): CheckTreeNode[] {
  const articlesById = new Map(packet.articles.map((a) => [a.id, a]));
  const childrenByParent = new Map<string, PacketArticle[]>();
  const articleRoots: PacketArticle[] = [];
  for (const a of packet.articles) {
    if (a.parentArticleId && articlesById.has(a.parentArticleId)) {
      const list = childrenByParent.get(a.parentArticleId) ?? [];
      list.push(a);
      childrenByParent.set(a.parentArticleId, list);
    } else {
      articleRoots.push(a);
    }
  }

  const statblocksByArticleId = new Map<string, Statblock[]>();
  const linkedStatblockIds = new Set<string>();
  for (const sb of packet.statblocks) {
    if (sb.articleId && articlesById.has(sb.articleId)) {
      const list = statblocksByArticleId.get(sb.articleId) ?? [];
      list.push(sb);
      statblocksByArticleId.set(sb.articleId, list);
      linkedStatblockIds.add(sb.id);
    }
  }

  const rootSubtreeArticleIds = new Map(
    articleRoots.map((root) => [root.id, new Set(articleSubtreeIds(root.id, childrenByParent))]),
  );

  const claimedRootIds = new Set<string>();
  const beatNodes: CheckTreeNode[] = packet.beats.map((b) => {
    const claimedRoots = articleRoots.filter((root) =>
      b.articleIds.some((id) => rootSubtreeArticleIds.get(root.id)!.has(id)),
    );
    for (const r of claimedRoots) claimedRootIds.add(r.id);
    return {
      id: `beat:${b.id}`,
      label: b.title,
      children: claimedRoots.map((r) => buildArticleNode(r, childrenByParent, statblocksByArticleId)),
    };
  });

  const unclaimedArticleRootNodes = articleRoots
    .filter((r) => !claimedRootIds.has(r.id))
    .map((r) => buildArticleNode(r, childrenByParent, statblocksByArticleId));

  const unlinkedStatblockNodes: CheckTreeNode[] = packet.statblocks
    .filter((sb) => !linkedStatblockIds.has(sb.id))
    .map((sb) => ({ id: `statblock:${sb.id}`, label: sb.name, children: [] }));

  const mapNodes: CheckTreeNode[] = packet.maps.map((m) => ({ id: `map:${m.id}`, label: m.name, children: [] }));
  const rollTableNodes: CheckTreeNode[] = packet.rollTables.map((t) => ({
    id: `rollTable:${t.id}`,
    label: t.title,
    children: [],
  }));
  const cardDeckNodes: CheckTreeNode[] = packet.cardDecks.map((d) => ({
    id: `cardDeck:${d.id}`,
    label: d.title,
    children: [],
  }));
  const handoutNodes: CheckTreeNode[] = packet.handouts.map((h) => ({
    id: `handout:${h.id}`,
    label: h.title,
    children: [],
  }));
  const clockNodes: CheckTreeNode[] = packet.clocks.map((c) => ({
    id: `clock:${c.id}`,
    label: c.title,
    children: [],
  }));

  return [
    ...beatNodes,
    ...unclaimedArticleRootNodes,
    ...unlinkedStatblockNodes,
    ...mapNodes,
    ...rollTableNodes,
    ...cardDeckNodes,
    ...handoutNodes,
    ...clockNodes,
  ];
}

interface Props {
  worldId: string;
  campaignId: string;
  sessionId: string;
  onClose: () => void;
  onError: (err: unknown) => void;
}

/**
 * Full-screen print packet for one session (ADR-0036): its scheduled beats, the
 * articles those beats reference, and the campaign's statblocks — everything for
 * the night on one printable document. Portalled to <body> so print CSS can hide
 * the app (reuses the print styles from ADR-0035).
 */
export function SessionPacketView({ worldId, campaignId, sessionId, onClose, onError }: Props) {
  const [packet, setPacket] = useState<SessionPacket | null>(null);
  const [templates, setTemplates] = useState<FieldTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const { opts: printOpts, setOpts: setPrintOpts, docProps: printDocProps } = usePrintOptions();
  // Print-time filtering (client-side, over the already-fetched packet).
  const [includeGmNotes, setIncludeGmNotes] = useState(true);
  // Per-item inclusion tree (beats, articles, statblocks, maps, tables,
  // decks, handouts) — composite `${kind}:${id}` keys, see CheckTree.tsx.
  const [excludedIds, setExcludedIds] = useState<Set<string>>(new Set());

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

  const tree = useMemo(() => (packet ? buildPacketTree(packet) : []), [packet]);

  const shownBeats = useMemo(
    () => packet?.beats.filter((b) => !excludedIds.has(`beat:${b.id}`)) ?? [],
    [packet, excludedIds],
  );
  const shownArticles = useMemo(
    () => packet?.articles.filter((a) => !excludedIds.has(`article:${a.id}`)) ?? [],
    [packet, excludedIds],
  );
  const shownStatblocks = useMemo(
    () => packet?.statblocks.filter((sb) => !excludedIds.has(`statblock:${sb.id}`)) ?? [],
    [packet, excludedIds],
  );
  const shownMaps = useMemo(
    () => packet?.maps.filter((m) => !excludedIds.has(`map:${m.id}`)) ?? [],
    [packet, excludedIds],
  );
  const shownRollTables = useMemo(
    () => packet?.rollTables.filter((t) => !excludedIds.has(`rollTable:${t.id}`)) ?? [],
    [packet, excludedIds],
  );
  const shownCardDecks = useMemo(
    () => packet?.cardDecks.filter((d) => !excludedIds.has(`cardDeck:${d.id}`)) ?? [],
    [packet, excludedIds],
  );
  const shownHandouts = useMemo(
    () => packet?.handouts.filter((h) => !excludedIds.has(`handout:${h.id}`)) ?? [],
    [packet, excludedIds],
  );
  const shownClocks = useMemo(
    () => packet?.clocks.filter((c) => !excludedIds.has(`clock:${c.id}`)) ?? [],
    [packet, excludedIds],
  );

  useEffect(() => {
    let active = true;
    sessionsApi(worldId, campaignId)
      .packet(sessionId)
      .then((p) => active && setPacket(p))
      .catch(onError)
      .finally(() => active && setLoading(false));
    fieldTemplatesApi(worldId)
      .list('STATBLOCK')
      .then((t) => active && setTemplates(t))
      .catch(onError);
    return () => {
      active = false;
    };
  }, [worldId, campaignId, sessionId, onError]);

  const s = packet?.session;
  const heading = s
    ? `${s.sessionNumber != null ? `Session ${s.sessionNumber}: ` : ''}${s.title}`
    : 'Session';

  return (
    <NewWindowPortal title={`Packet — ${heading}`} onClose={onClose}>
      <div className="print-toolbar">
        <strong>Session packet</strong>
        <label className="print-check">
          <Checkbox
            checked={includeGmNotes}
            onCheckedChange={(checked) => setIncludeGmNotes(checked === true)}
          />
          GM notes
        </label>
        <PrintOptionsMenu opts={printOpts} onChange={setPrintOpts} />
        <span className="print-toolbar-spacer" />
        <PrintButton disabled={loading || !packet} />
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>

      {!loading && packet && tree.length > 0 && (
        <div className="print-toolbar map-print-layers">
          <span className="muted">Include:</span>
          <ul className="check-tree">
            {tree.map((node) => (
              <CheckTreeRow key={node.id} node={node} excludedIds={excludedIds} onToggle={toggleIds} />
            ))}
          </ul>
        </div>
      )}

      <div className="print-doc" {...printDocProps}>
        <section className="print-cover">
          <h1>{heading}</h1>
          <p className="print-subtitle">
            {packet?.campaignName}
            {s?.date ? ` · ${s.date}` : ''}
          </p>
        </section>

        {loading && <p className="print-status">Preparing packet…</p>}

        {!loading && packet && (
          <>
            {(s?.summary || (s?.notes && includeGmNotes)) && (
              <section className="print-article">
                <h1>Session overview</h1>
                {s?.summary && (
                  <>
                    <p className="print-kicker">summary</p>
                    <div
                      className="preview-body"
                      dangerouslySetInnerHTML={{ __html: renderMarkdown(s.summary) }}
                    />
                  </>
                )}
                {s?.notes && includeGmNotes && (
                  <>
                    <p className="print-kicker">gm notes</p>
                    <div
                      className="preview-body"
                      dangerouslySetInnerHTML={{ __html: renderMarkdown(s.notes) }}
                    />
                  </>
                )}
              </section>
            )}

            <section className="print-article">
              <h1>Beats</h1>
              {shownBeats.length === 0 && (
                <p className="print-status">No beats scheduled into this session.</p>
              )}
              <ol className="print-beats">
                {shownBeats.map((b) => (
                  <li key={b.id} className="print-beat">
                    <span className="print-beat-title">
                      {b.done ? '☑' : '☐'} {b.title}
                    </span>
                    {b.arcTitle && <span className="print-beat-arc"> — {b.arcTitle}</span>}
                    {b.body && (
                      <div
                        className="preview-body"
                        dangerouslySetInnerHTML={{ __html: renderMarkdown(b.body) }}
                      />
                    )}
                  </li>
                ))}
              </ol>
            </section>

            {shownArticles.length > 0 && (
              <div className="print-divider">
                <h1>Referenced material</h1>
                <p className="print-kicker">articles linked from this session's beats</p>
              </div>
            )}

            {shownArticles.map((a) => (
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

            {shownMaps.map((m) => (
              <section key={m.id} className="print-map-section">
                <h1>{m.name}</h1>
                {m.imageUrl && (
                  <div className="print-map-figure">
                    <img src={m.imageUrl} alt={m.name} />
                    {m.pins.map((p, i) => (
                      <span
                        key={i}
                        className="print-map-marker"
                        style={{ left: `${p.x * 100}%`, top: `${p.y * 100}%` }}
                      >
                        {i + 1}
                      </span>
                    ))}
                  </div>
                )}
                {m.pins.length > 0 && (
                  <ol className="print-map-legend">
                    {m.pins.map((p, i) => (
                      <li key={i}>{p.label || 'Unlabeled pin'}</li>
                    ))}
                  </ol>
                )}
              </section>
            ))}

            {shownStatblocks.length > 0 && (
              <section className="print-map-section">
                <h1>Statblocks</h1>
                {shownStatblocks.map((sb) => (
                  <div key={sb.id} className="print-statblock">
                    <h2>{sb.name}</h2>
                    <dl className="print-stats">
                      {orderedStatEntries(sb.stats, sb.templateId, templates).map((entry) => (
                        <div key={entry.key} className="print-stat">
                          <dt>{entry.label}</dt>
                          {entry.type === 'TEXTAREA' ? (
                            <dd
                              className="preview-body"
                              dangerouslySetInnerHTML={{ __html: renderMarkdown(String(entry.value)) }}
                            />
                          ) : (
                            <dd>{String(entry.value)}</dd>
                          )}
                        </div>
                      ))}
                    </dl>
                    {sb.notes && (
                      <div
                        className="preview-body"
                        dangerouslySetInnerHTML={{ __html: renderMarkdown(sb.notes) }}
                      />
                    )}
                  </div>
                ))}
              </section>
            )}

            {shownRollTables.length > 0 && (
              <section className="print-map-section">
                <h1>Roll tables</h1>
                {shownRollTables.map((t) => (
                  <div key={t.id} className="print-roll-table">
                    <h2>{t.title}</h2>
                    <p className="print-kicker">
                      {t.diceExpression} ({t.minResult}–{t.maxResult})
                    </p>
                    <table className="print-table-grid">
                      <tbody>
                        {t.entries.map((e, i) => (
                          <tr key={i}>
                            <td className="print-table-range">
                              {e.minResult != null && e.maxResult != null
                                ? `${e.minResult}–${e.maxResult}`
                                : 'else'}
                            </td>
                            {/* eslint-disable-next-line react/no-danger */}
                            <td className="preview-body" dangerouslySetInnerHTML={{ __html: e.bodyHtml }} />
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ))}
              </section>
            )}

            {shownCardDecks.length > 0 && (
              <section className="print-map-section">
                <h1>Card decks</h1>
                {shownCardDecks.map((d) => (
                  <div key={d.id} style={{ marginBottom: '1rem' }}>
                    <h2>{d.title}</h2>
                    <div className="card-sheet">
                      {d.cards.map((c, i) => (
                        <div key={i} className="deck-card">
                          {c.title && <div className="deck-card-name">{c.title}</div>}
                          {/* eslint-disable-next-line react/no-danger */}
                          <div className="preview-body" dangerouslySetInnerHTML={{ __html: c.bodyHtml }} />
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </section>
            )}

            {shownHandouts.map((h) => (
              <section key={h.id} className="print-map-section">
                <div className="handout-print">
                  <article className={`handout-doc ${h.preset.toLowerCase()}`}>
                    <h2 className="handout-title">{h.title}</h2>
                    {/* eslint-disable-next-line react/no-danger */}
                    <div
                      className="preview-body"
                      dangerouslySetInnerHTML={{ __html: renderMarkdown(h.body) }}
                    />
                  </article>
                </div>
              </section>
            ))}

            {shownClocks.length > 0 && (
              <section className="print-map-section">
                <h1>Clocks</h1>
                <p className="print-kicker">blank for hand-marking at the table</p>
                {shownClocks.map((c) => (
                  <div key={c.id} className="print-clock">
                    <h2>{c.title}</h2>
                    {c.description && <p className="print-kicker">{c.description}</p>}
                    <div className="circle-tracker">
                      {c.segments.map((_, i) => (
                        <span key={i} className="pip read-only" title={`${i + 1}`} />
                      ))}
                    </div>
                    {c.segments.some((s) => s.title) && (
                      <ol className="print-map-legend">
                        {c.segments.map((s, i) =>
                          s.title ? (
                            <li key={i}>
                              {i + 1}: {s.title}
                              {s.description ? ` — ${s.description}` : ''}
                            </li>
                          ) : null,
                        )}
                      </ol>
                    )}
                  </div>
                ))}
              </section>
            )}
          </>
        )}
      </div>
    </NewWindowPortal>
  );
}
