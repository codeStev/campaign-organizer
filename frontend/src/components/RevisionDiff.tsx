interface Version {
  label: string;
  title: string;
  body: string;
}

interface Props {
  a: Version;
  b: Version;
}

type Seg = { type: 'same' | 'add' | 'del'; text: string };

/** Our media URLs are /api/media/{id}/content — the id is the stable identifier. */
function imageLabel(alt: string | undefined, src: string): string {
  const id = /\/api\/media\/([^/]+)\/content/.exec(src)?.[1];
  return alt || id || src || 'image';
}

/** A short, stable marker for a raw-HTML sized image (still valid in Markdown source). */
function imageMarkerFromHtml(tag: string): string {
  const alt = /alt="([^"]*)"/i.exec(tag)?.[1];
  const src = /src="([^"]*)"/i.exec(tag)?.[1] ?? '';
  return `\n🖼 [image: ${imageLabel(alt, src)}]\n`;
}

/**
 * Markdown -> readable plain text, preserving image markers (ADR-0054). No
 * HTML-tag stripping here — the body is Markdown now, and running it through
 * DOMParser (as the old HTML-diffing version did) would mis-parse a literal
 * "<"/">" in ordinary prose (e.g. "AC < 15") as a tag and drop it.
 */
function textFromMarkdown(markdown: string): string {
  return (markdown || '')
    .replace(/<img\b[^>]*>/gi, imageMarkerFromHtml)
    .replace(/!\[([^\]]*)\]\(([^)]*)\)/g, (_match, alt: string, src: string) =>
      `\n🖼 [image: ${imageLabel(alt, src)}]\n`,
    )
    .replace(/\n{3,}/g, '\n\n')
    .trimEnd();
}

/** Generic LCS diff over an array of items; returns ops in order. */
function lcsDiff<T>(A: T[], B: T[]): { type: 'same' | 'add' | 'del'; value: T }[] {
  const n = A.length;
  const m = B.length;
  const ops: { type: 'same' | 'add' | 'del'; value: T }[] = [];

  if (n * m > 4_000_000) {
    A.forEach((v) => ops.push({ type: 'del', value: v }));
    B.forEach((v) => ops.push({ type: 'add', value: v }));
    return ops;
  }

  const dp: number[][] = Array.from({ length: n + 1 }, () => new Array(m + 1).fill(0));
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      dp[i][j] = A[i] === B[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
    }
  }
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (A[i] === B[j]) {
      ops.push({ type: 'same', value: A[i] });
      i++;
      j++;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      ops.push({ type: 'del', value: A[i] });
      i++;
    } else {
      ops.push({ type: 'add', value: B[j] });
      j++;
    }
  }
  while (i < n) ops.push({ type: 'del', value: A[i++] });
  while (j < m) ops.push({ type: 'add', value: B[j++] });
  return ops;
}

function tokenize(s: string): string[] {
  return s.split(/(\s+)/).filter((t) => t.length > 0);
}

/** Word-level diff of a changed line pair, split per side for intra-line highlight. */
function wordSegs(aLine: string, bLine: string): { del: Seg[]; add: Seg[] } {
  const ops = lcsDiff(tokenize(aLine), tokenize(bLine));
  const del: Seg[] = [];
  const add: Seg[] = [];
  const push = (arr: Seg[], type: Seg['type'], text: string) => {
    const last = arr[arr.length - 1];
    if (last && last.type === type) last.text += text;
    else arr.push({ type, text });
  };
  for (const op of ops) {
    if (op.type === 'same') {
      push(del, 'same', op.value);
      push(add, 'same', op.value);
    } else if (op.type === 'del') {
      push(del, 'del', op.value);
    } else {
      push(add, 'add', op.value);
    }
  }
  return { del, add };
}

interface Row {
  type: 'same' | 'add' | 'del';
  segs: Seg[]; // for a changed line, may carry intra-line word highlights
  oldNo?: number; // line number in the older version (blank for added lines)
  newNo?: number; // line number in the newer version (blank for removed lines)
}

interface LineOp {
  type: 'same' | 'add' | 'del';
  value: string;
  oldNo?: number;
  newNo?: number;
}

/** Turn line ops into GitHub-style rows, pairing adjacent del/add lines for word highlight. */
function buildRows(aText: string, bText: string): Row[] {
  const ops = lcsDiff(aText.split('\n'), bText.split('\n'));

  // Assign line numbers per side before pairing so they stay monotonic.
  const annotated: LineOp[] = [];
  let oldNo = 1;
  let newNo = 1;
  for (const op of ops) {
    if (op.type === 'same') annotated.push({ type: 'same', value: op.value, oldNo: oldNo++, newNo: newNo++ });
    else if (op.type === 'del') annotated.push({ type: 'del', value: op.value, oldNo: oldNo++ });
    else annotated.push({ type: 'add', value: op.value, newNo: newNo++ });
  }

  const rows: Row[] = [];
  let dels: LineOp[] = [];
  let adds: LineOp[] = [];

  const flush = () => {
    const paired = Math.min(dels.length, adds.length);
    for (let k = 0; k < paired; k++) {
      const { del, add } = wordSegs(dels[k].value, adds[k].value);
      rows.push({ type: 'del', segs: del, oldNo: dels[k].oldNo });
      rows.push({ type: 'add', segs: add, newNo: adds[k].newNo });
    }
    for (let k = paired; k < dels.length; k++) {
      rows.push({ type: 'del', segs: [{ type: 'del', text: dels[k].value }], oldNo: dels[k].oldNo });
    }
    for (let k = paired; k < adds.length; k++) {
      rows.push({ type: 'add', segs: [{ type: 'add', text: adds[k].value }], newNo: adds[k].newNo });
    }
    dels = [];
    adds = [];
  };

  for (const op of annotated) {
    if (op.type === 'del') dels.push(op);
    else if (op.type === 'add') adds.push(op);
    else {
      flush();
      rows.push({ type: 'same', segs: [{ type: 'same', text: op.value }], oldNo: op.oldNo, newNo: op.newNo });
    }
  }
  flush();
  return rows;
}

function SegText({ segs }: { segs: Seg[] }) {
  return (
    <>
      {segs.map((s, i) =>
        s.type === 'same' ? (
          <span key={i}>{s.text || ' '}</span>
        ) : (
          <span key={i} className={s.type === 'add' ? 'diff-word-add' : 'diff-word-del'}>
            {s.text}
          </span>
        ),
      )}
    </>
  );
}

export function RevisionDiff({ a, b }: Props) {
  const rows = buildRows(textFromMarkdown(a.body), textFromMarkdown(b.body));
  const unchanged = rows.every((r) => r.type === 'same');

  return (
    <div className="diff card">
      <div className="diff-head muted">
        <span className="diff-del-key">− {a.label}</span>
        <span className="diff-add-key">+ {b.label}</span>
      </div>
      {a.title !== b.title && (
        <p className="diff-title">
          Title: <span className="diff-word-del">{a.title}</span> →{' '}
          <span className="diff-word-add">{b.title}</span>
        </p>
      )}
      {unchanged ? (
        <p className="muted">No text differences.</p>
      ) : (
        <div className="diff-view">
          {rows.map((r, i) => (
            <div key={i} className={`diff-row ${r.type}`}>
              <span className="diff-lineno">{r.oldNo ?? ''}</span>
              <span className="diff-lineno">{r.newNo ?? ''}</span>
              <span className="diff-gutter">{r.type === 'add' ? '+' : r.type === 'del' ? '−' : ' '}</span>
              <span className="diff-line">
                <SegText segs={r.segs} />
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
