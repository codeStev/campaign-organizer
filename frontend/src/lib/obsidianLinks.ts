// Hrefs Obsidian's own clipboard HTML carries for an internal wiki-link,
// confirmed against a live paste: `app://obsidian.md/<vault-relative-path>`
// (Obsidian's desktop app URL scheme) - `obsidian://` (the protocol-handler
// form some Obsidian exports use) is included too for robustness, though
// unconfirmed against a real paste.
const OBSIDIAN_HREF = /^(app:\/\/obsidian\.md\/|obsidian:\/\/)/;

export function isObsidianHref(href: string | null | undefined): boolean {
  return href != null && OBSIDIAN_HREF.test(href);
}
