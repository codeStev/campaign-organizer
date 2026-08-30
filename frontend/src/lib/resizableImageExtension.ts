import TiptapImage from '@tiptap/extension-image';

// Same two forms the app has always accepted in article bodies: plain
// CommonMark image syntax, and a raw <img> tag when a display width is set
// (MarkdownRenderer.java/HtmlSanitizer.java both expect exactly this - see
// ADR-0076's follow-up note and the width comment in HtmlSanitizer).
const MARKDOWN_IMAGE_RE = /^!\[([^\]]*)]\(([^\s)]+)(?:\s+"([^"]*)")?\)/;
const HTML_IMG_RE = /^<img\s+([^>]*?)\/?>/i;

function parseHtmlAttrs(attrStr: string): Record<string, string> {
  const attrs: Record<string, string> = {};
  const re = /([\w-]+)=["']([^"']*)["']/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(attrStr))) attrs[m[1]] = m[2];
  return attrs;
}

/**
 * `@tiptap/extension-image`'s own markdown integration only ever emits
 * `![alt](src)` - it never round-trips the `width`/`height` a resize sets
 * (its ResizableNodeView updates the node's attrs in the editor, but
 * `renderMarkdown` silently drops them on save). Existing article bodies
 * that predate the Tiptap migration store a resized image as raw
 * `<img src width>` HTML instead (CommonMark passes raw HTML straight
 * through) - and `@tiptap/markdown` doesn't recognize that at all, so
 * opening one of those articles rendered the tag as literal escaped text
 * and re-saving would have permanently corrupted it into `&lt;img...&gt;`.
 *
 * This extension keeps the base Image node's resize NodeView/commands via
 * `.extend()`, and replaces only its markdown parsing/rendering with a
 * single tokenizer that recognizes both forms and a renderer that emits
 * `<img width>` whenever a size was set, plain `![]()` otherwise.
 */
export const ResizableImage = TiptapImage.extend({
  markdownTokenizer: {
    name: 'image',
    level: 'block',
    start: (src: string) => {
      const bracket = src.indexOf('![');
      const tag = src.search(/<img\s/i);
      if (bracket === -1) return tag;
      if (tag === -1) return bracket;
      return Math.min(bracket, tag);
    },
    tokenize: (src: string) => {
      const md = MARKDOWN_IMAGE_RE.exec(src);
      if (md) {
        return {
          type: 'image',
          raw: md[0],
          alt: md[1] || null,
          src: md[2],
          title: md[3] ?? null,
          width: null,
          height: null,
        };
      }
      const html = HTML_IMG_RE.exec(src);
      if (html) {
        const attrs = parseHtmlAttrs(html[1]);
        if (!attrs.src) return undefined;
        return {
          type: 'image',
          raw: html[0],
          alt: attrs.alt ?? null,
          src: attrs.src,
          title: attrs.title ?? null,
          width: attrs.width ? Number(attrs.width) : null,
          height: attrs.height ? Number(attrs.height) : null,
        };
      }
      return undefined;
    },
  },

  parseMarkdown: (token, helpers) =>
    helpers.createNode('image', {
      src: token.src,
      alt: token.alt,
      title: token.title,
      width: token.width,
      height: token.height,
    }),

  renderMarkdown: (node) => {
    const { src, alt, title, width, height } = node.attrs ?? {};
    if (width != null || height != null) {
      const parts = [`src="${src}"`];
      if (alt) parts.push(`alt="${alt}"`);
      if (title) parts.push(`title="${title}"`);
      if (width != null) parts.push(`width="${width}"`);
      if (height != null) parts.push(`height="${height}"`);
      return `<img ${parts.join(' ')}>`;
    }
    const altText = alt ?? '';
    return title ? `![${altText}](${src} "${title}")` : `![${altText}](${src})`;
  },
});
