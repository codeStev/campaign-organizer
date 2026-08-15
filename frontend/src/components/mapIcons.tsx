import { renderToStaticMarkup } from 'react-dom/server';
import {
  MapPin,
  Castle,
  Building2,
  Landmark,
  House,
  Tent,
  Mountain,
  Trees,
  Waves,
  Anchor,
  Ship,
  Skull,
  Swords,
  Church,
  Gem,
  Crown,
  Flag,
  Flame,
  Shield,
  Key,
  Star,
  Compass,
  LucideIcon,
} from 'lucide-react';

/** The icons a map layer can be given (worldbuilding-flavoured). */
export const LAYER_ICONS: { key: string; label: string; Icon: LucideIcon }[] = [
  { key: 'pin', label: 'Pin', Icon: MapPin },
  { key: 'city', label: 'City', Icon: Building2 },
  { key: 'castle', label: 'Castle', Icon: Castle },
  { key: 'landmark', label: 'Landmark', Icon: Landmark },
  { key: 'house', label: 'House', Icon: House },
  { key: 'camp', label: 'Camp', Icon: Tent },
  { key: 'mountain', label: 'Mountain', Icon: Mountain },
  { key: 'forest', label: 'Forest', Icon: Trees },
  { key: 'water', label: 'Water', Icon: Waves },
  { key: 'port', label: 'Port', Icon: Anchor },
  { key: 'ship', label: 'Ship', Icon: Ship },
  { key: 'danger', label: 'Danger', Icon: Skull },
  { key: 'battle', label: 'Battle', Icon: Swords },
  { key: 'temple', label: 'Temple', Icon: Church },
  { key: 'treasure', label: 'Treasure', Icon: Gem },
  { key: 'capital', label: 'Capital', Icon: Crown },
  { key: 'quest', label: 'Quest', Icon: Flag },
  { key: 'fire', label: 'Fire', Icon: Flame },
  { key: 'guard', label: 'Guard', Icon: Shield },
  { key: 'secret', label: 'Secret', Icon: Key },
  { key: 'star', label: 'Star', Icon: Star },
  { key: 'compass', label: 'Landmark 2', Icon: Compass },
];

const BY_KEY = new Map(LAYER_ICONS.map((i) => [i.key, i.Icon]));

export function iconComponent(key?: string | null): LucideIcon | null {
  return key ? BY_KEY.get(key) ?? null : null;
}

/** Static SVG markup for a layer icon (for Leaflet divIcon HTML). Memoised. */
const svgCache = new Map<string, string>();
export function iconSvg(key: string, size = 14, color = '#fff'): string {
  const cacheKey = `${key}:${size}:${color}`;
  const cached = svgCache.get(cacheKey);
  if (cached) return cached;
  const Icon = BY_KEY.get(key);
  if (!Icon) return '';
  const svg = renderToStaticMarkup(<Icon size={size} color={color} strokeWidth={2.5} />);
  svgCache.set(cacheKey, svg);
  return svg;
}
