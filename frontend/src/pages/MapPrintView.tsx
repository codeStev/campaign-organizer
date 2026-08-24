import { useMemo, useState } from 'react';
import { NewWindowPortal, useNewWindowContainer } from '../components/NewWindowPortal';
import { WorldMap, MapPin } from '../api/client';
import { iconComponent } from '../components/mapIcons';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';

interface Props {
  map: WorldMap;
  pins: MapPin[];
  layers: string[];
  colorByLayer: Record<string, string>;
  iconByLayer: Record<string, string>;
  pinLabels: Record<string, string>;
  defaultColor: string;
  onClose: () => void;
}

// Visual filters applied to the map image (CSS filter; prints in Chromium).
const FILTERS: { key: string; label: string; css: string }[] = [
  { key: 'none', label: 'None', css: '' },
  { key: 'grayscale', label: 'Grayscale', css: 'grayscale(1)' },
  { key: 'sepia', label: 'Sepia', css: 'sepia(0.6)' },
  { key: 'parchment', label: 'Parchment', css: 'sepia(0.5) contrast(0.9) brightness(1.05)' },
  { key: 'blueprint', label: 'Blueprint', css: 'invert(1) hue-rotate(180deg) contrast(1.1)' },
  { key: 'contrast', label: 'High contrast', css: 'contrast(1.4)' },
];

/**
 * A separate component (not inlined below) so useNewWindowContainer() runs as
 * a descendant of NewWindowPortal's provider — see the identical note in
 * PrintView.tsx's ScopeSelect.
 */
function FilterSelect({ filterKey, onChange }: { filterKey: string; onChange: (key: string) => void }) {
  const container = useNewWindowContainer();
  return (
    <Select value={filterKey} onValueChange={onChange}>
      <SelectTrigger>
        <SelectValue />
      </SelectTrigger>
      <SelectContent container={container}>
        {FILTERS.map((f) => (
          <SelectItem key={f.key} value={f.key}>
            {f.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

/**
 * Prints a single map in its own tab (ADR-0047) with a customization menu:
 * scale, a visual filter, per-layer pin filtering (hide locations from players),
 * and label/legend toggles.
 */
export function MapPrintView({
  map,
  pins,
  layers,
  colorByLayer,
  iconByLayer,
  pinLabels,
  defaultColor,
  onClose,
}: Props) {
  const [scale, setScale] = useState(100);
  const [filterKey, setFilterKey] = useState('none');
  const [showPins, setShowPins] = useState(true);
  const [showLabels, setShowLabels] = useState(false);
  const [showLegend, setShowLegend] = useState(true);
  const [excludedLayers, setExcludedLayers] = useState<Set<string>>(new Set());
  const [includeUnlayered, setIncludeUnlayered] = useState(true);

  const filterCss = FILTERS.find((f) => f.key === filterKey)?.css ?? '';

  const shownPins = useMemo(() => {
    if (!showPins) return [];
    return pins.filter((p) => (p.layer ? !excludedLayers.has(p.layer) : includeUnlayered));
  }, [pins, showPins, excludedLayers, includeUnlayered]);

  function toggleLayer(layer: string) {
    setExcludedLayers((prev) => {
      const next = new Set(prev);
      if (next.has(layer)) next.delete(layer);
      else next.add(layer);
      return next;
    });
  }

  function colorOf(p: MapPin): string {
    return p.layer ? colorByLayer[p.layer] ?? defaultColor : defaultColor;
  }

  function iconOf(p: MapPin) {
    return iconComponent(p.layer ? iconByLayer[p.layer] : null);
  }

  return (
    <NewWindowPortal title={`Map — ${map.name}`} onClose={onClose}>
      <div className="print-toolbar map-print-toolbar">
        <strong>Print map</strong>
        <label className="print-check">
          Scale
          <input
            type="range"
            min={30}
            max={100}
            value={scale}
            onChange={(e) => setScale(Number(e.target.value))}
          />
          {scale}%
        </label>
        <label className="print-check">
          Filter
          <FilterSelect filterKey={filterKey} onChange={setFilterKey} />
        </label>
        <label className="print-check">
          <Checkbox checked={showPins} onCheckedChange={(checked) => setShowPins(checked === true)} />
          Pins
        </label>
        <label className="print-check">
          <Checkbox checked={showLabels} onCheckedChange={(checked) => setShowLabels(checked === true)} />
          Labels
        </label>
        <label className="print-check">
          <Checkbox checked={showLegend} onCheckedChange={(checked) => setShowLegend(checked === true)} />
          Legend
        </label>
        <span className="print-toolbar-spacer" />
        <Button onClick={() => window.print()}>🖨 Print</Button>
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>

      {showPins && (layers.length > 0 || pins.some((p) => !p.layer)) && (
        <div className="print-toolbar map-print-layers">
          <span className="muted">Show layers:</span>
          {layers.map((l) => (
            <label key={l} className="print-check">
              <Checkbox
                checked={!excludedLayers.has(l)}
                onCheckedChange={() => toggleLayer(l)}
              />
              <span className="map-print-swatch" style={{ background: colorByLayer[l] ?? defaultColor }} />
              {l}
            </label>
          ))}
          {pins.some((p) => !p.layer) && (
            <label className="print-check">
              <Checkbox
                checked={includeUnlayered}
                onCheckedChange={(checked) => setIncludeUnlayered(checked === true)}
              />
              (no layer)
            </label>
          )}
        </div>
      )}

      <div className="print-doc">
        <h1 className="map-print-title">{map.name}</h1>
        {map.imageUrl ? (
          <div className="print-map-figure" style={{ width: `${scale}%` }}>
            <img src={map.imageUrl} alt={map.name} style={{ filter: filterCss || undefined }} />
            {shownPins.map((p, i) => {
              const Icon = iconOf(p);
              return (
                <span
                  key={p.id}
                  className="map-print-marker"
                  style={{ left: `${p.x * 100}%`, top: `${p.y * 100}%` }}
                >
                  <span className="pin-badge" style={{ background: colorOf(p) }}>
                    {Icon ? <Icon size={13} color="#fff" strokeWidth={2.5} /> : i + 1}
                  </span>
                  {showLabels && pinLabels[p.id] && (
                    <span className="map-print-label">{pinLabels[p.id]}</span>
                  )}
                </span>
              );
            })}
          </div>
        ) : (
          <p className="print-status">This map's image is missing.</p>
        )}

        {showLegend && shownPins.length > 0 && (
          <ul className="print-map-legend map-print-legend">
            {shownPins.map((p, i) => {
              const Icon = iconOf(p);
              return (
                <li key={p.id} className="map-print-legend-item">
                  <span className="pin-legend-badge" style={{ background: colorOf(p) }}>
                    {Icon ? <Icon size={13} color="#fff" strokeWidth={2.5} /> : i + 1}
                  </span>
                  <span>{pinLabels[p.id] || 'Unlabeled pin'}</span>
                  {p.layer && <span className="muted">— {p.layer}</span>}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </NewWindowPortal>
  );
}
