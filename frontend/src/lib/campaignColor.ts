/**
 * Deterministic fallback color for a campaign that hasn't been assigned one
 * yet (see ADR-0107). Not the muted `--chart-1..5` tokens used for actual
 * charts elsewhere — those are deliberately low-chroma/near-gray, too close
 * to tell apart on a calendar where distinction is the whole point.
 */
const FALLBACK_PALETTE = [
  '#e63946', // red
  '#f4a261', // orange
  '#e9c46a', // gold
  '#2a9d8f', // teal
  '#457b9d', // blue
  '#9b5de5', // violet
];

function hashString(value: string): number {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

/** A campaign's own color if set, else a stable hash-derived fallback — same
 * function everywhere so an uncolored campaign looks identical on every
 * screen it appears on. */
export function getCampaignColor(campaign: { id: string; color?: string | null }): string {
  if (campaign.color) return campaign.color;
  return FALLBACK_PALETTE[hashString(campaign.id) % FALLBACK_PALETTE.length];
}
