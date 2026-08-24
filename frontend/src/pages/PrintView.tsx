import { useCallback, useEffect, useMemo, useState } from 'react';
import { NewWindowPortal, useNewWindowContainer } from '../components/NewWindowPortal';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import {
  articlesApi,
  mapsApi,
  pinsApi,
  Article,
  Campaign,
  MapPin,
  WorldMap,
} from '../api/client';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so the meaningfully persistent "whole world" scope goes through this sentinel.
const NONE_VALUE = '__none__';

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
}: {
  scope: string;
  campaigns: Campaign[];
  onChange: (scope: string) => void;
}) {
  const container = useNewWindowContainer();
  return (
    <Select value={scope || NONE_VALUE} onValueChange={(v) => onChange(v === NONE_VALUE ? '' : v)}>
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
 * then hands off to the browser's native print / Save-as-PDF via window.print().
 */
export function PrintView({ worldId, worldName, campaigns, onClose, onError }: Props) {
  const api = useMemo(() => articlesApi(worldId), [worldId]);
  const maps = useMemo(() => mapsApi(worldId), [worldId]);
  // '' = whole world; otherwise restrict to a campaign's referenced articles.
  const [scope, setScope] = useState('');
  const [includeMaps, setIncludeMaps] = useState(true);
  const [includeContents, setIncludeContents] = useState(true);
  const [loading, setLoading] = useState(true);
  const [articles, setArticles] = useState<Article[]>([]);
  const [printableMaps, setPrintableMaps] = useState<PrintableMap[]>([]);

  const scopeName = scope ? campaigns.find((c) => c.id === scope)?.name ?? '' : '';
  // Maps only print at whole-world scope, so `articles` covers every linked pin.
  const articleTitleById = useMemo(() => new Map(articles.map((a) => [a.id, a.title])), [articles]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const summaries = await api.list(scope ? { campaignId: scope } : undefined);
      // Fetch each rendered body; sort into a stable A–Z booklet order.
      const full = await Promise.all(summaries.map((s) => api.get(s.id)));
      full.sort((a, b) => a.title.localeCompare(b.title));
      setArticles(full);

      if (includeMaps && !scope) {
        const list = await maps.list();
        const withPins = await Promise.all(
          list.map(async (m) => ({ map: m, pins: await pinsApi(worldId, m.id).list() })),
        );
        setPrintableMaps(withPins);
      } else {
        setPrintableMaps([]);
      }
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, maps, worldId, scope, includeMaps, onError]);

  useEffect(() => {
    void load();
  }, [load]);

  const today = new Date().toLocaleDateString();

  return (
    <NewWindowPortal title={`Print — ${worldName}`} onClose={onClose}>
      <div className="print-toolbar">
        <strong>Print / PDF</strong>
        <label>
          Scope <ScopeSelect scope={scope} campaigns={campaigns} onChange={setScope} />
        </label>
        <label className="print-check">
          <input
            type="checkbox"
            checked={includeContents}
            onChange={(e) => setIncludeContents(e.target.checked)}
          />
          Contents
        </label>
        <label className="print-check" title={scope ? 'Maps print with the whole world only' : ''}>
          <input
            type="checkbox"
            checked={includeMaps}
            disabled={!!scope}
            onChange={(e) => setIncludeMaps(e.target.checked)}
          />
          Maps
        </label>
        <span className="print-toolbar-spacer" />
        <Button onClick={() => window.print()} disabled={loading}>
          🖨 Print
        </Button>
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>

      <div className="print-doc">
        <section className="print-cover">
          <h1>{worldName}</h1>
          <p className="print-subtitle">{scopeName ? `Campaign: ${scopeName}` : 'World compendium'}</p>
          <p className="print-date">{today}</p>
        </section>

        {loading && <p className="print-status">Preparing document…</p>}

        {!loading && includeContents && articles.length > 0 && (
          <section className="print-contents">
            <h2>Contents</h2>
            <ol>
              {articles.map((a) => (
                <li key={a.id}>{a.title}</li>
              ))}
            </ol>
          </section>
        )}

        {!loading &&
          articles.map((a) => (
            <article key={a.id} className="print-article">
              <h1>{a.title}</h1>
              <p className="print-kicker">{a.template.toLowerCase()}</p>
              {/* eslint-disable-next-line react/no-danger */}
              <div
                className="print-body"
                dangerouslySetInnerHTML={{ __html: a.bodyHtml || '<p><em>(empty)</em></p>' }}
              />
            </article>
          ))}

        {!loading && articles.length === 0 && (
          <p className="print-status">
            {scope ? 'This campaign references no articles yet.' : 'No articles to print.'}
          </p>
        )}

        {!loading &&
          printableMaps.map(({ map, pins }) => (
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
      </div>
    </NewWindowPortal>
  );
}
