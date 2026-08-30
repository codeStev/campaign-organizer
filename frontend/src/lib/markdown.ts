import MarkdownIt from 'markdown-it';
import taskLists from 'markdown-it-task-lists';

/**
 * `breaks: true` preserves the visual line-break behavior these fields had
 * under plain `<textarea>` + CSS `white-space: pre-wrap` before markdown
 * rendering existed (ADR-0054) — without it, a single Enter in existing
 * prose collapses into a run-on paragraph under strict CommonMark.
 */
const md = new MarkdownIt({ html: false, breaks: true, linkify: true })
  // GFM task lists (`- [ ] `/`- [x] `, ADR-0076) render as real, disabled
  // checkboxes here - this is read-only preview, editing happens in
  // MarkdownEditor. `label: true` groups the checkbox with its text.
  .use(taskLists, { label: true });

export function renderMarkdown(source: string): string {
  return md.render(source || '');
}

// Same grammar as the backend WikiLinker (ADR-0014).
const WIKI_LINK = /\[\[\s*([^\]|]+?)\s*(?:\|\s*([^\]]+?)\s*)?\]\]/g;

/**
 * Client-side wiki-link pass for raw bodies printed outside articles (roll-table
 * entries, deck cards). Runs after Markdown rendering — unlike the server, which
 * runs before — because `html: false` would escape injected anchors; with all
 * user HTML already escaped by then, a remaining `[[…]]` can only be literal
 * link syntax. Anchors only: printouts never pull targets along (FR-40).
 */
export function renderLinkedMarkdown(
  source: string,
  /** Lowercase title/slug → display title; null when unresolved. */
  lookup: (name: string) => string | null,
): string {
  const html = renderMarkdown(source);
  if (!html.includes('[[')) return html;
  return html.replace(WIKI_LINK, (_match, target: string, label?: string) => {
    const title = lookup(target.toLowerCase());
    const text = label ?? title ?? target;
    return title != null
      ? `<a class="wiki-link" href="#">${text}</a>`
      : `<span class="broken-link">${text}</span>`;
  });
}
