import MarkdownIt from 'markdown-it';

/**
 * `breaks: true` preserves the visual line-break behavior these fields had
 * under plain `<textarea>` + CSS `white-space: pre-wrap` before markdown
 * rendering existed (ADR-0054) — without it, a single Enter in existing
 * prose collapses into a run-on paragraph under strict CommonMark.
 */
const md = new MarkdownIt({ html: false, breaks: true, linkify: true });

export function renderMarkdown(source: string): string {
  return md.render(source || '');
}
