import { PacketArticle, PacketBeat } from '../api/client';

/**
 * Layout rules for the session packet (ADR-0118): which lettered sections the
 * printed document has, in what order, how articles are grouped, and which
 * material each beat points at. Pure, so the view stays markup-only.
 */

export type PacketSectionId =
  | 'cheatSheet'
  | 'beats'
  | 'articles'
  | 'statblocks'
  | 'tables'
  | 'maps'
  | 'handouts'
  | 'clocks';

export interface PacketSection {
  id: PacketSectionId;
  /** Tab letter shown in the contents, the running header and the "Needs" chips. */
  letter: string;
  title: string;
}

/** Document order of the lettered sections. */
const SECTION_ORDER: Array<{ id: PacketSectionId; title: string }> = [
  { id: 'cheatSheet', title: 'Cheat sheet' },
  { id: 'beats', title: 'Beats' },
  { id: 'articles', title: 'NPCs, places & notes' },
  { id: 'statblocks', title: 'Statblocks' },
  { id: 'tables', title: 'Tables & decks' },
  { id: 'maps', title: 'Maps' },
  { id: 'handouts', title: 'Handouts' },
  { id: 'clocks', title: 'Clocks' },
];

/** Sections that have content, lettered A, B, C… without gaps. */
export function buildSections(counts: Record<PacketSectionId, number>): PacketSection[] {
  return SECTION_ORDER.filter((s) => counts[s.id] > 0).map((s, i) => ({
    ...s,
    letter: String.fromCharCode(65 + i),
  }));
}

export interface ArticleGroup {
  template: string;
  articles: PacketArticle[];
}

/** "NPC" / "LOCATION" / "SESSION_NOTE" → "Npc" / "Location" / "Session note". */
export function templateLabel(template: string): string {
  const words = template.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** Articles grouped by template (groups alphabetical), alphabetical by title within a group. */
export function groupArticlesByTemplate(articles: PacketArticle[]): ArticleGroup[] {
  const groups = new Map<string, PacketArticle[]>();
  for (const a of articles) {
    const list = groups.get(a.template) ?? [];
    list.push(a);
    groups.set(a.template, list);
  }
  return [...groups.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([template, list]) => ({
      template,
      articles: [...list].sort((a, b) => a.title.localeCompare(b.title)),
    }));
}

export interface NeedChip {
  key: string;
  label: string;
  /** Letter of the section the item is printed in. */
  letter: string;
  /** Anchor id of the printed item (the chip links to it on screen). */
  anchor: string;
}

export interface NeedsLookup {
  sections: PacketSection[];
  articles: Map<string, string>;
  statblocks: Map<string, string>;
  tables: Map<string, string>;
  decks: Map<string, string>;
}

export function anchorId(kind: 'article' | 'statblock' | 'table' | 'deck', id: string): string {
  return `pk-${kind}-${id}`;
}

/**
 * What a beat needs at the table: the articles, statblocks, roll tables and
 * decks it links to, each with the section letter they are printed under.
 * Items that were excluded (or are not printed) are left out.
 */
export function beatNeeds(beat: PacketBeat, lookup: NeedsLookup): NeedChip[] {
  const letterOf = (id: PacketSectionId) => lookup.sections.find((s) => s.id === id)?.letter;
  const chips: NeedChip[] = [];
  const add = (
    kind: 'article' | 'statblock' | 'table' | 'deck',
    ids: string[],
    names: Map<string, string>,
    section: PacketSectionId,
  ) => {
    const letter = letterOf(section);
    if (!letter) return;
    for (const id of ids) {
      const label = names.get(id);
      if (label) chips.push({ key: `${kind}:${id}`, label, letter, anchor: anchorId(kind, id) });
    }
  };
  add('article', beat.articleIds, lookup.articles, 'articles');
  add('statblock', beat.statblockIds, lookup.statblocks, 'statblocks');
  add('table', beat.tableIds, lookup.tables, 'tables');
  add('deck', beat.deckIds, lookup.decks, 'tables');
  return chips;
}
