import { ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  mapsApi,
  mapCategoriesApi,
  pinsApi,
  mediaApi,
  articlesApi,
  layerStylesApi,
  WorldMap,
  MapCategory,
  MapPin,
  ArticleSummary,
  LayerStyle,
  ApiError,
} from '../api/client';
import { MapCanvas } from '../components/MapCanvas';
import { MapPrintView } from './MapPrintView';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { CategoryTree } from '../components/CategoryTree';
import { MobileBackButton } from '../components/MobileBackButton';
import { LAYER_ICONS, iconComponent, iconSvg } from '../components/mapIcons';
import { Button } from '../components/ui/button';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { PromptDialog } from '../components/PromptDialog';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { toast } from 'sonner';
import { Checkbox } from '../components/ui/checkbox';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so a meaningfully persistent "none" state goes through this sentinel.
const NONE_VALUE = '__none__';

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

export function NextMapsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { mapId: urlMapId } = useParams<{ mapId: string }>();
  const maps = useMemo(() => mapsApi(worldId), [worldId]);
  const mapCategories = useMemo(() => mapCategoriesApi(worldId), [worldId]);
  const media = useMemo(() => mediaApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);
  const stylesApi = useMemo(() => layerStylesApi(worldId), [worldId]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [list, setList] = useState<WorldMap[]>([]);
  const [categories, setCategories] = useState<MapCategory[]>([]);
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
  const [pendingMapFile, setPendingMapFile] = useState<File | null>(null);
  // Set by the tree's "+ New map" on a category, before the file picker
  // opens — carried through to createMapWithName so the map lands pre-assigned.
  const [pendingCategoryId, setPendingCategoryId] = useState<string | null>(null);
  const [mapNameOpen, setMapNameOpen] = useState(false);

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

  const refreshMaps = useCallback(() => {
    setLoading(true);
    return Promise.all([maps.list(), mapCategories.list()])
      .then(([m, c]) => {
        setList(m);
        setCategories(c);
      })
      .catch(handleError)
      .finally(() => setLoading(false));
  }, [maps, mapCategories, handleError]);

  useEffect(() => {
    void refreshMaps();
    articleApi.list().then(setArticles).catch(handleError);
    stylesApi.get().then(setStyles).catch(handleError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [articleApi, stylesApi, handleError]);

  async function createMapCategory(name: string, parentId: string | null) {
    try {
      await mapCategories.create({ name, parentId });
      await refreshMaps();
    } catch (err) {
      handleError(err);
    }
  }

  async function removeMapCategory(category: MapCategory) {
    try {
      await mapCategories.remove(category.id);
      await refreshMaps();
    } catch (err) {
      handleError(err);
    }
  }

  async function renameMapCategory(category: MapCategory, newName: string) {
    try {
      await mapCategories.update(category.id, { name: newName, parentId: category.parentId ?? null });
      await refreshMaps();
    } catch (err) {
      handleError(err);
    }
  }

  async function renameMap(map: WorldMap, newName: string) {
    if (map.name === newName) return;
    try {
      const updated = await maps.update(map.id, {
        name: newName,
        mediaId: map.mediaId ?? '',
        categoryId: map.categoryId ?? null,
      });
      if (selected?.id === updated.id) setSelected(updated);
      await refreshMaps();
    } catch (err) {
      handleError(err);
    }
  }

  // WorldMap's list response already carries every field an update needs
  // (unlike Article, there's no separate "summary vs full" split to worry
  // about), so this can update directly without a full-fetch-first step.
  async function moveMapToCategory(map: WorldMap, categoryId: string | null) {
    if ((map.categoryId ?? null) === categoryId) return;
    try {
      const updated = await maps.update(map.id, {
        name: map.name,
        mediaId: map.mediaId ?? '',
        categoryId,
      });
      if (selected?.id === updated.id) setSelected(updated);
      await refreshMaps();
    } catch (err) {
      handleError(err);
    }
  }

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

  function handleNewMapFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setPendingMapFile(file);
    setMapNameOpen(true);
  }

  async function createMapWithName(name: string) {
    const file = pendingMapFile;
    if (!file) return;
    try {
      const asset = await media.upload(file);
      const map = await maps.create({ name, mediaId: asset.id, categoryId: pendingCategoryId });
      setPendingCategoryId(null);
      setList(await maps.list());
      await selectMap(map);
      navigate(urlMapId ? `../${map.id}` : map.id, { relative: 'path' });
      toast.success(`Map "${map.name}" added`);
    } catch (err) {
      handleError(err);
    } finally {
      setPendingMapFile(null);
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
      toast.success('Pin saved');
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

  // Context-menu "Print" on a tree row — the map may not be the open one,
  // so its pins need loading first (MapCanvas/MapPrintView need them).
  async function printMap(map: WorldMap) {
    if (selected?.id !== map.id) await selectMap(map);
    setPrintOpen(true);
  }

  async function deleteMap(map: WorldMap) {
    try {
      await maps.remove(map.id);
      if (selected?.id === map.id) {
        setSelected(null);
        setPins([]);
        navigate('..', { relative: 'path' });
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
    <div className="wiki-layout maps-layout" data-has-selection={!!selected}>
      <aside className="wiki-sidebar">
        <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleNewMapFile} />
        <PromptDialog
          open={mapNameOpen}
          onOpenChange={setMapNameOpen}
          title="Name this map"
          label="Map name"
          defaultValue={pendingMapFile ? pendingMapFile.name.replace(/\.[^.]+$/, '') : ''}
          onSubmit={(name) => void createMapWithName(name)}
        />
        <CategoryTree
          categories={categories}
          entities={list}
          entityId={(m) => m.id}
          entityLabel={(m) => m.name}
          entityCategoryId={(m) => m.categoryId ?? null}
          activeEntityId={selected?.id ?? null}
          onOpenEntity={(id) => navigate(urlMapId ? `../${id}` : id, { relative: 'path' })}
          onMoveEntity={(m, categoryId) => void moveMapToCategory(m, categoryId)}
          onCreateCategory={(name, parentId) => void createMapCategory(name, parentId)}
          onRemoveCategory={(c) => void removeMapCategory(c)}
          onRenameCategory={(c, name) => void renameMapCategory(c, name)}
          onRenameEntity={(m, name) => void renameMap(m, name)}
          onDeleteEntity={(m) => void deleteMap(m)}
          onPrintEntity={(m) => void printMap(m)}
          newEntityActions={[
            {
              label: 'New map',
              onCreate: (categoryId) => {
                setPendingCategoryId(categoryId);
                fileInputRef.current?.click();
              },
            },
          ]}
          loading={loading}
          searchPlaceholder="Search maps…"
          emptyLabel="No maps yet."
          renderEntityRow={(m) => (
            <TruncatedLabel label={m.name} data-testid="map-name">
              {m.name}
            </TruncatedLabel>
          )}
        />

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
                <Select
                  value={styles[layer]?.icon || NONE_VALUE}
                  onValueChange={(v) => setLayerIcon(layer, v === NONE_VALUE ? '' : v)}
                >
                  <SelectTrigger className="layer-icon-select" title={`Icon for ${layer}`}>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NONE_VALUE}>#</SelectItem>
                    {LAYER_ICONS.map((ic) => (
                      <SelectItem key={ic.key} value={ic.key}>
                        {ic.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <label className="layer-toggle">
                  <Checkbox
                    checked={!hiddenLayers.has(layer)}
                    onCheckedChange={() => toggleLayer(layer)}
                  />
                  {layer}
                </label>
              </div>
            ))}
          </div>
        )}
      </aside>

      <div className="wiki-main">
        <MobileBackButton />
        {error && <p className="error">{error}</p>}
        {!selected && <p className="muted">Select or create a map. Click the image to drop a pin.</p>}
        {selected && (
          <>
            <div className="map-bar">
              <strong>{selected.name}</strong>
              <label className="layer-toggle" title="Show each pin's label on the map">
                <Checkbox
                  checked={showLabels}
                  onCheckedChange={(checked) => setShowLabels(checked === true)}
                />
                Labels
              </label>
              <Button variant="link" onClick={() => setPrintOpen(true)} title="Print or save as PDF">
                🖨 Print map
              </Button>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    Delete map
                  </Button>
                }
                title="Delete map?"
                description={`This permanently deletes "${selected.name}" and its pins. This cannot be undone.`}
                onConfirm={() => deleteMap(selected)}
              />
            </div>

            <div className="maps-canvas-row">
              <div className="maps-canvas-col">
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
              </div>

              <div className="maps-side-col">
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
              </div>
            </div>

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
        <Input value={label} onChange={(e) => setLabel(e.target.value)} placeholder="e.g. Old Keep" />
      </label>
      <label>
        Layer
        <Input
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
        <Select value={articleId || NONE_VALUE} onValueChange={(v) => setArticleId(v === NONE_VALUE ? '' : v)}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NONE_VALUE}>— none —</SelectItem>
            {articles.map((a) => (
              <SelectItem key={a.id} value={a.id}>
                {a.title}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </label>
      <div className="editor-actions">
        <Button onClick={() => onSave({ label, layer, articleId })} disabled={!dirty}>
          Save pin
        </Button>
        {pin.articleId && (
          <Button variant="link" onClick={() => onOpen(pin.articleId!)}>
            Open article
          </Button>
        )}
        <ConfirmDeleteDialog
          trigger={
            <Button variant="link" className="text-destructive hover:text-destructive">
              Delete pin
            </Button>
          }
          title="Delete pin?"
          description="This permanently removes this pin from the map. This cannot be undone."
          onConfirm={onDelete}
        />
      </div>
    </div>
  );
}
