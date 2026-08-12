// One-off seeder: imports an Obsidian vault into a Campaign Organizer world.
// Preserves [[wiki-links]] (incl. [[Target|alias]]) so the app's auto-linker resolves them.
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const API = process.env.API ?? 'http://localhost:8080/api';
const PASSWORD = process.env.APP_PASSWORD ?? 'changeme';
const VAULT = process.env.VAULT ?? '/home/codestev/Obsidian/echoes of chaos';

// Directory -> article template. Root files fall back to GENERIC.
const DIR_TEMPLATE = { Characters: 'CHARACTER', Locations: 'LOCATION', Arcs: 'EVENT' };

let token;
async function api(method, path, body) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status} ${await res.text()}`);
  return res.status === 204 ? null : res.json();
}

function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// Inline markdown -> HTML. Keeps [[...]] intact (the app resolves those).
// Wiki-links are stashed behind null-byte-delimited placeholders so they never
// collide with real numbers in the surrounding text.
function inline(text) {
  const links = [];
  let t = text.replace(/\[\[[^\]]+\]\]/g, (m) => {
    links.push(m);
    return `\x00${links.length - 1}\x00`;
  });
  t = escapeHtml(t);
  t = t.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  t = t.replace(/(^|[^*])\*(?!\*)(.+?)\*(?!\*)/g, '$1<em>$2</em>');
  t = t.replace(/\x00(\d+)\x00/g, (_, i) => links[Number(i)]);
  return t;
}

// Block-level markdown -> HTML. Drops frontmatter and the leading H1 title.
function toHtml(md) {
  let body = md.replace(/^---\n[\s\S]*?\n---\n?/, ''); // frontmatter
  body = body.replace(/^\s*#\s+.*\n?/, ''); // leading H1 (title used separately)
  const lines = body.split('\n');
  const out = [];
  let para = [];
  let list = [];
  const flushPara = () => {
    if (para.length) {
      out.push(`<p>${inline(para.join(' ').trim())}</p>`);
      para = [];
    }
  };
  const flushList = () => {
    if (list.length) {
      out.push(`<ul>${list.map((li) => `<li>${inline(li)}</li>`).join('')}</ul>`);
      list = [];
    }
  };
  for (const raw of lines) {
    const line = raw.trimEnd();
    if (line.trim() === '') { flushPara(); flushList(); continue; }
    const h = line.match(/^(#{2,6})\s+(.*)/);
    const li = line.match(/^[-*]\s+(.*)/);
    const bq = line.match(/^>\s?(.*)/);
    if (h) { flushPara(); flushList(); const lvl = Math.min(h[1].length, 4); out.push(`<h${lvl}>${inline(h[2])}</h${lvl}>`); }
    else if (li) { flushPara(); list.push(li[1]); }
    else if (bq) { flushPara(); flushList(); out.push(`<blockquote>${inline(bq[1])}</blockquote>`); }
    else { flushList(); para.push(line); }
  }
  flushPara(); flushList();
  return out.join('\n');
}

function collect() {
  const items = [];
  // Root-level notes -> Overview.
  for (const f of readdirSync(VAULT)) {
    if (f.endsWith('.md')) items.push({ dir: null, file: f });
  }
  for (const [dir] of Object.entries(DIR_TEMPLATE)) {
    for (const f of readdirSync(join(VAULT, dir))) {
      if (f.endsWith('.md')) items.push({ dir, file: f });
    }
  }
  return items;
}

async function main() {
  token = (await api('POST', '/auth/login', { password: PASSWORD })).token;
  console.log('Logged in.');

  const world = await api('POST', '/worlds', {
    name: 'Echoes of Chaos',
    description: 'A D&D campaign — four chromatic dragons work to revive Tiamat.',
  });
  console.log(`World: ${world.name} (${world.id})`);

  // Categories.
  const catNames = ['Characters', 'Locations', 'Arcs', 'Overview'];
  const cats = {};
  for (const name of catNames) {
    cats[name] = (await api('POST', `/worlds/${world.id}/categories`, { name })).id;
  }
  console.log(`Categories: ${catNames.join(', ')}`);

  let n = 0;
  for (const { dir, file } of collect()) {
    const md = readFileSync(join(VAULT, dir ?? '', file), 'utf8');
    const title = file.replace(/\.md$/, '');
    const template = dir ? DIR_TEMPLATE[dir] : 'GENERIC';
    const categoryId = dir ? cats[dir] : cats.Overview;
    await api('POST', `/worlds/${world.id}/articles`, {
      title,
      template,
      categoryId,
      body: toHtml(md),
    });
    n++;
  }
  console.log(`Imported ${n} articles.`);
  console.log(`\nOpen http://localhost:3000 and log in with "${PASSWORD}".`);
}

main().catch((e) => { console.error(e); process.exit(1); });
