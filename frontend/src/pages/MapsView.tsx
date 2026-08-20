import { ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  mapsApi,
  pinsApi,
  mediaApi,
  articlesApi,
  layerStylesApi,
  WorldMap,
  MapPin,
  ArticleSummary,
  LayerStyle,
  ApiError,
} from '../api/client';
import { MapCanvas } from '../components/MapCanvas';
import { MapPrintView } from './MapPrintView';
import { LAYER_ICONS, iconComponent, iconSvg } from '../components/mapIcons';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

const DEFAULT_PIN_COLOR = '#6d54c9';

// Distinct, stable palette; a layer maps to one entry by name hash.
const PALETTE = [
  '#e6194b', '#3cb44b', '#4363d8', '#f58231', '#911eb4',
  '#42a5f5', '#f032e6', '#8bc34a', '#ff8f00', '#009688',
  '#9a6324', '#e91e63', '#808000', '#00838f', '#5c6bc0',
];

function autoColor(layer: string): string {
  let hash = 0;
  for (let i = 0; i < layer.length; i++) hash = (hash * 31 + layer.charCodeAt(i)) >>> 0;
  return PALETTE[hash % PALETTE.length];
}

export function MapsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { mapId: urlMapId } = useParams<{ mapId: string }>();
  const maps = useMemo(() => mapsApi(worldId), [worldId]);
  const media = useMemo(() => mediaApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);
  const stylesApi = useMemo(() => layerStylesApi(worldId), [worldId]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [list, setList] = useState<WorldMap[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<WorldMap | null>(null);
  const [pins, setPins] = useState<MapPin[]>([]);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [hiddenLayers, setHiddenLayers] = useState<Set<string>>(new Set());
  const [selectedPinId, setSelectedPinId] = useState<string | null>(null);
  const [showLabels, setShowLabels] = useState(false);
  const [printOpen, setPrintOpen] = useState(false);
  // Per-layer styling (colour + icon), persisted on the world so it travels
  // with the export and across devices (ADR-0049).
  const [styles, setStyles] = useState<Record<string, LayerStyle>>({});
  const [error, setError] = useState<string | null>(null);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const loadPins = useCallback(
    async (mapId: string) => {
      try {
        setPins(await pinsApi(worldId, mapId).list());
      } catch (err) {
        handleError(err);
      }
    },
    [worldId, handleError],
  );

  useEffect(() => {
    maps.list().then(setList).catch(handleError).finally(() => setLoading(false));
    articleApi.list().then(setArticles).catch(handleError);
    stylesApi.get().then(setStyles).catch(handleError);
  }, [maps, articleApi, stylesApi, handleError]);

  // Persist a layer-styling change to the world (merged with existing styles).
  const saveStyles = useCallback(
    (next: Record<string, LayerStyle>) => {
      setStyles(next);
      stylesApi.put(next).catch(handleError);
    },
    [stylesApi, handleError],
  );

  async function selectMap(map: WorldMap) {
    setSelected(map);
    setSelectedPinId(null);
    setHiddenLayers(new Set());
    await loadPins(map.id);
  }

  // The URL is the source of truth for which map is open (ADR-0053).
  useEffect(() => {
    if (!urlMapId || urlMapId === selected?.id) return;
    maps.get(urlMapId).then(selectMap).catch(handleError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlMapId]);

  async function handleNewMapFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    const name = window.prompt('Map name?', file.name.replace(/\.[^.]+$/, ''));
    if (!name) return;
    try {
      const asset = await media.upload(file);
      const map = await maps.create({ name, mediaId: asset.id });
      setList(await maps.list());
      await selectMap(map);
      navigate(`/worlds/${worldId}/maps/${map.id}`);
    } catch (err) {
      handleError(err);
    }
  }

  // Clicking the map drops a pin immediately; label/layer/article are edited after.
  async function addPin(x: number, y: number) {
    if (!selected) return;
    try {
      const created = await pinsApi(worldId, selected.id).create({ x, y });
      await loadPins(selected.id);
      setSelectedPinId(created.id);
    } catch (err) {
      handleError(err);
    }
  }

  async function savePin(pin: MapPin, fields: { label: string; layer: string; articleId: string }) {
    if (!selected) return;
    try {
      await pinsApi(worldId, selected.id).update(pin.id, {
        x: pin.x,
        y: pin.y,
        label: fields.label || null,
        layer: fields.layer || null,
        articleId: fields.articleId || null,
      });
      await loadPins(selected.id);
    } catch (err) {
      handleError(err);
    }
  }

  async function deletePin(pin: MapPin) {
    if (!selected) return;
    try {
      await pinsApi(worldId, selected.id).remove(pin.id);
      setSelectedPinId(null);
      await loadPins(selected.id);
    } catch (err) {
      handleError(err);
    }
  }

  async function deleteMap(map: WorldMap) {
    try {
      await maps.remove(map.id);
      if (selected?.id === map.id) {
        setSelected(null);
        setPins([]);
        navigate(`/worlds/${worldId}/maps`);
      }
      setList(await maps.list());
    } catch (err) {
      handleError(err);
    }
  }

  const layers = useMemo(
    () => Array.from(new Set(pins.map((p) => p.layer).filter((l): l is string => !!l))),
    [pins],
  );
  const visiblePins = pins.filter((p) => !p.layer || !hiddenLayers.has(p.layer));
  const selectedPin = pins.find((p) => p.id === selectedPinId) ?? null;

  const articleTitleById = useMemo(
    () => new Map(articles.map((a) => [a.id, a.title])),
    [articles],
  );
  // A pin's shown label: its own label, else the linked article's title.
  const labelFor = useCallback(
    (pin: MapPin) => pin.label || (pin.articleId ? articleTitleById.get(pin.articleId) ?? '' : ''),
    [articleTitleById],
  );
  const pinLabels = useMemo(() => {
    const map: Record<string, string> = {};
    pins.forEach((p) => {
      const l = labelFor(p);
      if (l) map[p.id] = l;
    });
    return map;
  }, [pins, labelFor]);

  const colorForLayer = useCallback(
    (layer: string) => styles[layer]?.color || autoColor(layer),
    [styles],
  );
  const colorByLayer = useMemo(() => {
    const map: Record<string, string> = {};
    layers.forEach((l) => (map[l] = colorForLayer(l)));
    return map;
  }, [layers, colorForLayer]);

  const iconByLayer = useMemo(() => {
    const map: Record<string, string> = {};
    layers.forEach((l) => {
      const icon = styles[l]?.icon;
      if (icon) map[l] = icon;
    });
    return map;
  }, [layers, styles]);

  // SVG markup for a pin's layer icon (white), or null to fall back to the number.
  const pinIcon = useCallback(
    (pin: MapPin) => {
      const key = pin.layer ? styles[pin.layer]?.icon : undefined;
      return key ? iconSvg(key, 14, '#fff') : null;
    },
    [styles],
  );

  function setLayerColor(layer: string, color: string) {
    saveStyles({ ...styles, [layer]: { ...styles[layer], color } });
  }

  function setLayerIcon(layer: string, key: string) {
    saveStyles({ ...styles, [layer]: { ...styles[layer], icon: key || null } });
  }

  function toggleLayer(layer: string) {
    setHiddenLayers((prev) => {
      const next = new Set(prev);
      if (next.has(layer)) next.delete(layer);
      else next.add(layer);
      return next;
    });
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <button onClick={() => fileInputRef.current?.click()}>+ New map</button>
        <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleNewMapFile} />
        <ul className="article-list">
          {list.map((m) => (
            <li key={m.id}>
              <button
                className={m.id === selected?.id ? 'article-link active' : 'article-link'}
                onClick={() => navigate(`/worlds/${worldId}/maps/${m.id}`)}
              >
                <span>{m.name}</span>
              </button>
            </li>
          ))}
          {loading && <li className="muted">Loading…</li>}
          {!loading && list.length === 0 && <li className="muted">No maps yet.</li>}
        </ul>

        {layers.length > 0 && (
          <div className="layer-toggles">
            <strong className="muted">Layers</strong>
            <p className="muted hint">Colour codes the pins; untick to hide a layer.</p>
            {layers.map((layer) => (
              <div key={layer} className="layer-row">
                <input
                  type="color"
                  className="layer-color"
                  value={colorForLayer(layer)}
                  onChange={(e) => setLayerColor(layer, e.target.value)}
                  title={`Colour for ${layer}`}
                />
                <select
                  className="layer-icon-select"
                  value={styles[layer]?.icon ?? ''}
                  onChange={(e) => setLayerIcon(layer, e.target.value)}
                  title={`Icon for ${layer}`}
                >
                  <option value="">#</option>
                  {LAYER_ICONS.map((ic) => (
                    <option key={ic.key} value={ic.key}>
                      {ic.label}
                    </option>
                  ))}
                </select>
                <label className="layer-toggle">
                  <input
                    type="checkbox"
                    checked={!hiddenLayers.has(layer)}
                    onChange={() => toggleLayer(layer)}
                  />
                  {layer}
                </label>
              </div>
            ))}
          </div>
        )}
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        {!selected && <p className="muted">Select or create a map. Click the image to drop a pin.</p>}
        {selected && (
          <>
            <div className="map-bar">
              <strong>{selected.name}</strong>
              <label className="layer-toggle" title="Show each pin's label on the map">
                <input
                  type="checkbox"
                  checked={showLabels}
                  onChange={(e) => setShowLabels(e.target.checked)}
                />
                Labels
              </label>
              <button className="link-button" onClick={() => setPrintOpen(true)} title="Print or save as PDF">
                🖨 Print map
              </button>
              <button className="link-button danger" onClick={() => deleteMap(selected)}>
                Delete map
              </button>
            </div>
            {selected.imageUrl ? (
              <MapCanvas
                imageUrl={selected.imageUrl}
                pins={visiblePins}
                selectedPinId={selectedPinId}
                colorByLayer={colorByLayer}
                defaultColor={DEFAULT_PIN_COLOR}
                pinLabels={pinLabels}
                showLabels={showLabels}
                pinIcon={pinIcon}
                onMapClick={addPin}
                onPinClick={setSelectedPinId}
              />
            ) : (
              <p className="error">This map's image is missing.</p>
            )}

            {visiblePins.length > 0 && (
              <ol className="pin-legend card">
                {visiblePins.map((p, i) => (
                  <li
                    key={p.id}
                    className={p.id === selectedPinId ? 'pin-legend-item active' : 'pin-legend-item'}
                  >
                    <button className="pin-legend-btn" onClick={() => setSelectedPinId(p.id)}>
                      <span
                        className="pin-legend-badge"
                        style={{ background: p.layer ? colorByLayer[p.layer] ?? DEFAULT_PIN_COLOR : DEFAULT_PIN_COLOR }}
                      >
                        {(() => {
                          const key = p.layer ? styles[p.layer]?.icon : undefined;
                          const Icon = iconComponent(key);
                          return Icon ? <Icon size={13} color="#fff" strokeWidth={2.5} /> : i + 1;
                        })()}
                      </span>
                      <span className="pin-legend-label">
                        {labelFor(p) || <em className="muted">(no label)</em>}
                      </span>
                      {p.layer && <span className="pin-legend-layer muted">{p.layer}</span>}
                    </button>
                  </li>
                ))}
              </ol>
            )}

            {selectedPin && (
              <PinEditor
                key={selectedPin.id}
                pin={selectedPin}
                articles={articles}
                layers={layers}
                onSave={(fields) => savePin(selectedPin, fields)}
                onOpen={onOpenArticle}
                onDelete={() => deletePin(selectedPin)}
              />
            )}

            {printOpen && (
              <MapPrintView
                map={selected}
                pins={pins}
                layers={layers}
                colorByLayer={colorByLayer}
                iconByLayer={iconByLayer}
                pinLabels={pinLabels}
                defaultColor={DEFAULT_PIN_COLOR}
                onClose={() => setPrintOpen(false)}
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}

interface PinEditorProps {
  pin: MapPin;
  articles: ArticleSummary[];
  layers: string[];
  onSave: (fields: { label: string; layer: string; articleId: string }) => void;
  onOpen: (articleId: string) => void;
  onDelete: () => void;
}

/** Inline editor for a selected pin (label, layer, linked article). */
function PinEditor({ pin, articles, layers, onSave, onOpen, onDelete }: PinEditorProps) {
  const [label, setLabel] = useState(pin.label ?? '');
  const [layer, setLayer] = useState(pin.layer ?? '');
  const [articleId, setArticleId] = useState(pin.articleId ?? '');

  const dirty =
    label !== (pin.label ?? '') || layer !== (pin.layer ?? '') || articleId !== (pin.articleId ?? '');

  const fallbackTitle =
    pin.label || (pin.articleId ? articles.find((a) => a.id === pin.articleId)?.title : '') || 'Pin';

  return (
    <div className="card pin-panel">
      <strong>{fallbackTitle}</strong>
      <label>
        Label
        <input value={label} onChange={(e) => setLabel(e.target.value)} placeholder="e.g. Old Keep" />
      </label>
      <label>
        Layer
        <input
          value={layer}
          list="pin-layer-options"
          onChange={(e) => setLayer(e.target.value)}
          placeholder="e.g. cities"
        />
        <datalist id="pin-layer-options">
          {layers.map((l) => (
            <option key={l} value={l} />
          ))}
        </datalist>
      </label>
      <label>
        Linked article
        <select value={articleId} onChange={(e) => setArticleId(e.target.value)}>
          <option value="">— none —</option>
          {articles.map((a) => (
            <option key={a.id} value={a.id}>
              {a.title}
            </option>
          ))}
        </select>
      </label>
      <div className="editor-actions">
        <button onClick={() => onSave({ label, layer, articleId })} disabled={!dirty}>
          Save pin
        </button>
        {pin.articleId && (
          <button className="link-button" onClick={() => onOpen(pin.articleId!)}>
            Open article
          </button>
        )}
        <button className="link-button danger" onClick={onDelete}>
          Delete pin
        </button>
      </div>
    </div>
  );
}
