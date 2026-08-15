import { useCallback, useEffect, useMemo, useState } from 'react';
import { NewWindowPortal } from '../components/NewWindowPortal';
import {
  articlesApi,
  mapsApi,
  pinsApi,
  Article,
  Campaign,
  MapPin,
  WorldMap,
} from '../api/client';

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
          Scope{' '}
          <select value={scope} onChange={(e) => setScope(e.target.value)}>
            <option value="">Whole world</option>
            {campaigns.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
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
        <button onClick={() => window.print()} disabled={loading}>
          🖨 Print
        </button>
        <button className="link-button" onClick={onClose}>
          Close
        </button>
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
                    <li key={p.id}>{p.label || 'Unlabeled pin'}</li>
                  ))}
                </ol>
              )}
            </section>
          ))}
      </div>
    </NewWindowPortal>
  );
}
