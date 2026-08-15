import { useEffect, useRef } from 'react';
import * as L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin } from '../api/client';

interface Props {
  imageUrl: string;
  pins: MapPin[];
  selectedPinId?: string | null;
  /** Fill colour per layer name; pins without a layer use defaultColor. */
  colorByLayer?: Record<string, string>;
  defaultColor?: string;
  /** Effective label per pin id (own label or article-title fallback). */
  pinLabels?: Record<string, string>;
  /** When true, labels are shown permanently beside the pin (else on hover). */
  showLabels?: boolean;
  /** SVG markup for a pin's layer icon, or null to show its number. */
  pinIcon?: (pin: MapPin) => string | null;
  onMapClick: (x: number, y: number) => void;
  onPinClick: (pinId: string) => void;
}

/**
 * Renders a non-geographic image map with Leaflet (CRS.Simple) and pins placed
 * by fractional coordinates (ADR-0018). Leaflet is driven imperatively.
 */
export function MapCanvas({
  imageUrl,
  pins,
  selectedPinId,
  colorByLayer = {},
  defaultColor = '#6d54c9',
  pinLabels = {},
  showLabels = false,
  pinIcon,
  onMapClick,
  onPinClick,
}: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const markersRef = useRef<L.LayerGroup | null>(null);
  const dimsRef = useRef<{ w: number; h: number } | null>(null);

  // Keep latest callbacks without re-initializing the map.
  const clickRef = useRef(onMapClick);
  clickRef.current = onMapClick;
  const pinClickRef = useRef(onPinClick);
  pinClickRef.current = onPinClick;

  useEffect(() => {
    if (!containerRef.current) return undefined;
    const map = L.map(containerRef.current, {
      crs: L.CRS.Simple,
      minZoom: -5,
      attributionControl: false,
    });
    mapRef.current = map;
    markersRef.current = L.layerGroup().addTo(map);
    dimsRef.current = null;

    const img = new Image();
    img.onload = () => {
      const w = img.naturalWidth || 1000;
      const h = img.naturalHeight || 1000;
      dimsRef.current = { w, h };
      const bounds: L.LatLngBoundsExpression = [
        [0, 0],
        [h, w],
      ];
      L.imageOverlay(imageUrl, bounds).addTo(map);
      map.fitBounds(bounds);
    };
    img.src = imageUrl;

    map.on('click', (e: L.LeafletMouseEvent) => {
      const d = dimsRef.current;
      if (!d) return;
      const x = e.latlng.lng / d.w;
      const y = 1 - e.latlng.lat / d.h;
      if (x >= 0 && x <= 1 && y >= 0 && y <= 1) clickRef.current(x, y);
    });

    return () => {
      map.remove();
      mapRef.current = null;
      markersRef.current = null;
      dimsRef.current = null;
    };
  }, [imageUrl]);

  useEffect(() => {
    const draw = () => {
      const markers = markersRef.current;
      const d = dimsRef.current;
      if (!markers || !d) return false;
      markers.clearLayers();
      pins.forEach((pin, i) => {
        const selected = pin.id === selectedPinId;
        const fill = pin.layer ? colorByLayer[pin.layer] ?? defaultColor : defaultColor;
        // Badge shows the layer icon when set, else the pin number; layer colour fills it.
        const inner = pinIcon?.(pin) || `${i + 1}`;
        const icon = L.divIcon({
          className: 'pin-div',
          html: `<span class="pin-badge${selected ? ' selected' : ''}" style="background:${fill}">${inner}</span>`,
          iconSize: [22, 22],
          iconAnchor: [11, 11],
        });
        const marker = L.marker([d.h * (1 - pin.y), d.w * pin.x], { icon });
        const label = pinLabels[pin.id] || pin.label || '';
        if (label) {
          marker.bindTooltip(
            label,
            showLabels
              ? { permanent: true, direction: 'right', offset: [10, 0], className: 'pin-label-tip' }
              : undefined,
          );
        }
        marker.on('click', () => pinClickRef.current(pin.id));
        marker.addTo(markers);
      });
      return true;
    };
    if (draw()) return undefined;
    // Image dimensions not ready yet; retry until the overlay has loaded.
    const timer = setInterval(() => {
      if (draw()) clearInterval(timer);
    }, 100);
    return () => clearInterval(timer);
  }, [pins, selectedPinId, colorByLayer, defaultColor, pinLabels, showLabels, pinIcon]);

  return <div ref={containerRef} className="map-canvas" />;
}
