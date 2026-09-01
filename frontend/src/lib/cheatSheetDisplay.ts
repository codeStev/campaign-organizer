import { FieldTemplate, Statblock } from '../api/client';
import { orderedStatEntries } from './statblockDisplay';

/** "2–7" / "≤12" / "—" for a table row's printed range. */
export function entryRange(e: { minResult?: number | null; maxResult?: number | null }): string {
  if (e.minResult == null && e.maxResult == null) return '—';
  if (e.minResult != null && (e.maxResult == null || e.minResult === e.maxResult)) {
    return String(e.minResult);
  }
  if (e.minResult == null) return `≤${e.maxResult}`;
  return `${e.minResult}–${e.maxResult}`;
}

/** `body` may be raw Markdown (the editor) or already-rendered HTML (the packet). */
export function cardLabel(c: { title?: string | null; body: string }): string {
  if (c.title) return c.title;
  const plain = c.body
    .replace(/<[^>]+>/g, ' ')
    .replace(/[#*_>`[\]]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  return plain.length > 48 ? `${plain.slice(0, 48)}…` : plain || '(untitled card)';
}

/** Condensed "AC 15 · HP 7 · Speed 30 ft" line for a referenced statblock. */
export function statblockLine(sb: Statblock, templates: FieldTemplate[]): string {
  return orderedStatEntries(sb.stats, sb.templateId, templates)
    .filter((e) => e.type !== 'TEXTAREA' && String(e.value).trim() !== '')
    .slice(0, 8)
    .map((e) => `${e.label} ${String(e.value)}`)
    .join(' · ');
}
