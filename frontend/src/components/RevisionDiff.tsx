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

/** HTML → readable plain text, preserving block boundaries as newlines. */
function textFromHtml(html: string): string {
  const withBreaks = (html || '')
    .replace(/<\/(p|h1|h2|h3|h4|li|div|blockquote|tr)>/gi, '\n')
    .replace(/<br\s*\/?>/gi, '\n');
  const doc = new DOMParser().parseFromString(withBreaks, 'text/html');
  return (doc.body.textContent || '').replace(/\n{3,}/g, '\n\n').trimEnd();
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
}

/** Turn line ops into GitHub-style rows, pairing adjacent del/add lines for word highlight. */
function buildRows(aText: string, bText: string): Row[] {
  const ops = lcsDiff(aText.split('\n'), bText.split('\n'));
  const rows: Row[] = [];
  let dels: string[] = [];
  let adds: string[] = [];

  const flush = () => {
    const paired = Math.min(dels.length, adds.length);
    for (let k = 0; k < paired; k++) {
      const { del, add } = wordSegs(dels[k], adds[k]);
      rows.push({ type: 'del', segs: del });
      rows.push({ type: 'add', segs: add });
    }
    for (let k = paired; k < dels.length; k++) rows.push({ type: 'del', segs: [{ type: 'del', text: dels[k] }] });
    for (let k = paired; k < adds.length; k++) rows.push({ type: 'add', segs: [{ type: 'add', text: adds[k] }] });
    dels = [];
    adds = [];
  };

  for (const op of ops) {
    if (op.type === 'del') dels.push(op.value);
    else if (op.type === 'add') adds.push(op.value);
    else {
      flush();
      rows.push({ type: 'same', segs: [{ type: 'same', text: op.value }] });
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
  const rows = buildRows(textFromHtml(a.body), textFromHtml(b.body));
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
