import { ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  mapsApi,
  pinsApi,
  mediaApi,
  articlesApi,
  WorldMap,
  MapPin,
  ArticleSummary,
  ApiError,
} from '../api/client';
import { MapCanvas } from '../components/MapCanvas';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

export function MapsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const maps = useMemo(() => mapsApi(worldId), [worldId]);
  const media = useMemo(() => mediaApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [list, setList] = useState<WorldMap[]>([]);
  const [selected, setSelected] = useState<WorldMap | null>(null);
  const [pins, setPins] = useState<MapPin[]>([]);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [hiddenLayers, setHiddenLayers] = useState<Set<string>>(new Set());
  const [selectedPinId, setSelectedPinId] = useState<string | null>(null);
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
    maps.list().then(setList).catch(handleError);
    articleApi.list().then(setArticles).catch(handleError);
  }, [maps, articleApi, handleError]);

  async function selectMap(map: WorldMap) {
    setSelected(map);
    setSelectedPinId(null);
    setHiddenLayers(new Set());
    await loadPins(map.id);
  }

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
    } catch (err) {
      handleError(err);
    }
  }

  async function addPin(x: number, y: number) {
    if (!selected) return;
    const label = window.prompt('Pin label (optional)') ?? undefined;
    const layer = window.prompt('Layer (optional, e.g. cities)') ?? undefined;
    try {
      await pinsApi(worldId, selected.id).create({ x, y, label, layer });
      await loadPins(selected.id);
    } catch (err) {
      handleError(err);
    }
  }

  async function linkArticle(pin: MapPin, articleId: string | null) {
    if (!selected) return;
    try {
      await pinsApi(worldId, selected.id).update(pin.id, {
        x: pin.x,
        y: pin.y,
        label: pin.label,
        layer: pin.layer,
        articleId,
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
                onClick={() => selectMap(m)}
              >
                <span>{m.name}</span>
              </button>
            </li>
          ))}
          {list.length === 0 && <li className="muted">No maps yet.</li>}
        </ul>

        {layers.length > 0 && (
          <div className="layer-toggles">
            <strong className="muted">Layers</strong>
            {layers.map((layer) => (
              <label key={layer} className="layer-toggle">
                <input
                  type="checkbox"
                  checked={!hiddenLayers.has(layer)}
                  onChange={() => toggleLayer(layer)}
                />
                {layer}
              </label>
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
              <button className="link-button danger" onClick={() => deleteMap(selected)}>
                Delete map
              </button>
            </div>
            {selected.imageUrl ? (
              <MapCanvas
                imageUrl={selected.imageUrl}
                pins={visiblePins}
                selectedPinId={selectedPinId}
                onMapClick={addPin}
                onPinClick={setSelectedPinId}
              />
            ) : (
              <p className="error">This map's image is missing.</p>
            )}

            {selectedPin && (
              <div className="card pin-panel">
                <strong>{selectedPin.label || 'Pin'}</strong>
                <label>
                  Linked article
                  <select
                    value={selectedPin.articleId ?? ''}
                    onChange={(e) => linkArticle(selectedPin, e.target.value || null)}
                  >
                    <option value="">— none —</option>
                    {articles.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.title}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="editor-actions">
                  {selectedPin.articleId && (
                    <button onClick={() => onOpenArticle(selectedPin.articleId!)}>Open article</button>
                  )}
                  <button className="link-button danger" onClick={() => deletePin(selectedPin)}>
                    Delete pin
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
