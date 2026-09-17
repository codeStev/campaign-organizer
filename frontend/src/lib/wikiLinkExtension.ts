import { Mark, mergeAttributes, markInputRule } from '@tiptap/core';

// Same grammar as the backend WikiLinker (ADR-0014) and the client-side
// read-only renderer (lib/markdown.ts's WIKI_LINK).
const WIKI_LINK_RE = /^\[\[\s*([^\]|]+?)\s*(?:\|\s*([^\]]+?)\s*)?\]\]/;

// Input-rule variants of the same grammar, anchored at the cursor (`$`) so
// they fire the instant the closing `]]` is typed - see addInputRules below
// for why these exist separately from WIKI_LINK_RE/markdownTokenizer.
const WIKI_LINK_LABELED_INPUT_RE = /\[\[\s*([^\]|]+?)\s*\|\s*([^\]]+?)\s*\]\]$/;
const WIKI_LINK_UNLABELED_INPUT_RE = /\[\[\s*([^\]|]+?)\s*\]\]$/;

/**
 * `[[Article Title]]` / `[[Article Title|Display Text]]` support for the
 * live editor.
 *
 * Without this, `@tiptap/markdown`'s serializer backslash-escapes every
 * `[`/`]` in plain text (`escapeMarkdownSyntax`, unconditional, no config
 * knob to disable) - typing a wiki-link and saving would come back out as
 * `\[\[Article Title\]\]`, corrupting the syntax on every edit pass. Giving
 * wiki-links their own mark with dedicated parseMarkdown/renderMarkdown/
 * markdownTokenizer handlers (native `MarkConfig` fields, backed by
 * `@tiptap/core`'s marked.js integration) sidesteps the generic
 * text-escaping path entirely, since the target text is no longer a plain
 * "text" node once tokenized.
 *
 * Styled distinctly in the editor (`.wiki-link-editor`) but not
 * click-navigable there - resolving a title to an article is what the
 * separate read-only renderer's `lookup` callback already does
 * (renderLinkedMarkdown), and this component has no access to the live
 * article list.
 *
 * `markdownTokenizer`/`parseMarkdown` only run when markdown text is parsed
 * into the doc (e.g. loading an existing article for editing) - they do
 * nothing for a brand-new `[[Title]]` typed live into an already-open
 * editor, since that never re-runs the markdown parser. Without an input
 * rule, freshly-typed brackets stayed a plain text node and hit the exact
 * escaping bug this file's own header comment describes, corrupting the
 * link on first save (confirmed empirically: an article typed fresh here,
 * then pushed to Foundry VTT, came out the other end still containing
 * literal `\[\[Title\]\]`). `addInputRules` closes that gap by converting
 * the pattern to this mark the instant the closing `]]` is typed, the same
 * way Tiptap's own Bold/Italic extensions convert `**text**`/`*text*` live.
 */
export const WikiLink = Mark.create({
  name: 'wikiLink',

  addAttributes() {
    return {
      target: { default: null },
      // Frozen at parse time: whether the source had an explicit `|label`.
      // renderMarkdown can't compare live text against `target` to decide
      // this itself - `helpers.renderChildren` returns an internal
      // placeholder token there, substituted with the real text only after
      // this mark's renderer already returned, so the two are never
      // actually comparable at render time.
      labeled: { default: false },
    };
  },

  parseHTML() {
    return [{ tag: 'span[data-wiki-link]' }];
  },

  renderHTML({ HTMLAttributes, mark }) {
    return [
      'span',
      mergeAttributes(HTMLAttributes, { 'data-wiki-link': mark.attrs.target, class: 'wiki-link-editor' }),
      0,
    ];
  },

  markdownTokenizer: {
    name: 'wikiLink',
    level: 'inline',
    start: (src) => src.indexOf('[['),
    tokenize: (src) => {
      const match = WIKI_LINK_RE.exec(src);
      if (!match) return undefined;
      const target = match[1].trim();
      const label = match[2]?.trim() || target;
      return { type: 'wikiLink', raw: match[0], target, label };
    },
  },

  parseMarkdown: (token, helpers) =>
    helpers.applyMark('wikiLink', [{ type: 'text', text: token.label }], {
      target: token.target,
      labeled: token.label !== token.target,
    }),

  renderMarkdown: (node, helpers) => {
    const content = helpers.renderChildren(node);
    // Unlabeled form always uses the current visible text for both slots -
    // self-consistent even if the user edits the label text in place
    // without touching the mark's own `target` attribute.
    if (!node.attrs?.labeled) return `[[${content}]]`;
    return `[[${node.attrs.target}|${content}]]`;
  },

  addInputRules() {
    return [
      // Labeled form first: its regex requires a `|`, the unlabeled one
      // forbids it, so the two never both match the same typed text.
      markInputRule({
        find: WIKI_LINK_LABELED_INPUT_RE,
        type: this.type,
        getAttributes: (match) => ({ target: match[1].trim(), labeled: true }),
      }),
      markInputRule({
        find: WIKI_LINK_UNLABELED_INPUT_RE,
        type: this.type,
        getAttributes: (match) => ({ target: match[1].trim(), labeled: false }),
      }),
    ];
  },
});
