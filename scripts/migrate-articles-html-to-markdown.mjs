// One-time data migration (ADR-0054): converts article bodies from stored
// HTML to Markdown, for both `articles.body` and every `article_revisions.body`
// snapshot. Talks to Postgres directly — no HTTP endpoint can rewrite historical
// revision rows, and going through PUT /articles/{id} would spuriously snapshot
// the pre-migration HTML as a new revision.
//
// Usage:
//   pg_dump the database first. Then:
//     node migrate-articles-html-to-markdown.mjs            # dry run, prints a diff summary
//     node migrate-articles-html-to-markdown.mjs --apply    # writes the conversion
//
// Connection: standard libpq env vars (PGHOST, PGPORT, PGUSER, PGPASSWORD,
// PGDATABASE) or a single PG_CONNECTION_STRING.
import pg from 'pg';
import TurndownService from 'turndown';

const APPLY = process.argv.includes('--apply');

const turndown = new TurndownService({ headingStyle: 'atx', codeBlockStyle: 'fenced' });

// Don't escape `[`/`]` — turndown's default escaping treats every literal
// square bracket as a would-be Markdown link and backslash-escapes it, which
// would corrupt every `[[wiki-link]]` already living in article bodies (plain
// text today; WikiLinker resolves it at read time) into `\[\[...\]\]`,
// breaking WikiLinker's regex match post-migration (ADR-0054).
turndown.escape = (text) =>
  text
    .replace(/\\/g, '\\\\')
    .replace(/\*/g, '\\*')
    .replace(/^-/g, '\\-')
    .replace(/^\+ /g, '\\+ ')
    .replace(/^(=+)/g, '\\$1')
    .replace(/^(#{1,6}) /g, '\\$1 ')
    .replace(/`/g, '\\`')
    .replace(/^~~~/g, '\\~~~')
    .replace(/^>/g, '\\>')
    .replace(/_/g, '\\_')
    .replace(/^(\d+)\. /g, '$1\\. ');

// Sized images (the old editor's width control) must stay raw HTML — Markdown
// image syntax has no width — so flexmark's raw-HTML passthrough keeps
// rendering them at their chosen size after the migration (ADR-0054).
turndown.addRule('sizedImage', {
  filter: (node) => node.nodeName === 'IMG' && !!node.getAttribute('width'),
  replacement: (_content, node) => {
    const src = node.getAttribute('src') ?? '';
    const alt = node.getAttribute('alt') ?? '';
    const width = node.getAttribute('width');
    return `<img src="${src}" alt="${alt}" width="${width}">`;
  },
});

function toMarkdown(html) {
  if (html == null) return html;
  if (!/[<&]/.test(html)) return html; // already plain text/markdown, nothing to convert
  return turndown.turndown(html);
}

async function main() {
  const client = new pg.Client(
    process.env.PG_CONNECTION_STRING ? { connectionString: process.env.PG_CONNECTION_STRING } : undefined,
  );
  await client.connect();

  try {
    const { rows: articles } = await client.query('SELECT id, body FROM articles');
    const { rows: revisions } = await client.query('SELECT id, body FROM article_revisions');

    console.log(`${articles.length} articles, ${revisions.length} revisions to convert.`);

    let changed = 0;
    for (const row of [...articles, ...revisions]) {
      const converted = toMarkdown(row.body);
      if (converted !== row.body) changed++;
    }
    console.log(`${changed} rows would change.`);

    if (!APPLY) {
      console.log('Dry run only — pass --apply to write changes.');
      return;
    }

    await client.query('BEGIN');
    for (const row of articles) {
      await client.query('UPDATE articles SET body = $1 WHERE id = $2', [toMarkdown(row.body), row.id]);
    }
    for (const row of revisions) {
      await client.query('UPDATE article_revisions SET body = $1 WHERE id = $2', [
        toMarkdown(row.body),
        row.id,
      ]);
    }
    await client.query('COMMIT');
    console.log('Applied.');
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    throw err;
  } finally {
    await client.end();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
