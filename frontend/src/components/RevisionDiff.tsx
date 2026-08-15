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
  return (doc.body.textContent || '').replace(/\n{3,}/g, '\n\n').trim();
}

/** Split into words and whitespace runs (whitespace kept so newlines survive). */
function tokenize(s: string): string[] {
  return s.split(/(\s+)/).filter((t) => t.length > 0);
}

function push(segs: Seg[], type: Seg['type'], text: string) {
  const last = segs[segs.length - 1];
  if (last && last.type === type) last.text += text;
  else segs.push({ type, text });
}

/** Word-level LCS diff. Falls back to a coarse replace for very large inputs. */
function wordDiff(aText: string, bText: string): Seg[] {
  const A = tokenize(aText);
  const B = tokenize(bText);
  const n = A.length;
  const m = B.length;
  const segs: Seg[] = [];

  if (n * m > 3_000_000) {
    if (aText) push(segs, 'del', aText);
    if (bText) push(segs, 'add', bText);
    return segs;
  }

  // dp[i][j] = LCS length of A[i:] and B[j:].
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
      push(segs, 'same', A[i]);
      i++;
      j++;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      push(segs, 'del', A[i]);
      i++;
    } else {
      push(segs, 'add', B[j]);
      j++;
    }
  }
  while (i < n) push(segs, 'del', A[i++]);
  while (j < m) push(segs, 'add', B[j++]);
  return segs;
}

export function RevisionDiff({ a, b }: Props) {
  const segs = wordDiff(textFromHtml(a.body), textFromHtml(b.body));
  const unchanged = segs.every((s) => s.type === 'same');

  return (
    <div className="diff card">
      <div className="diff-head muted">
        <span className="diff-del-key">− {a.label}</span>
        <span className="diff-add-key">+ {b.label}</span>
      </div>
      {a.title !== b.title && (
        <p className="diff-title">
          Title: <span className="diff-del">{a.title}</span> →{' '}
          <span className="diff-add">{b.title}</span>
        </p>
      )}
      <div className="diff-view">
        {unchanged ? (
          <span className="muted">No text differences.</span>
        ) : (
          segs.map((s, i) =>
            s.type === 'same' ? (
              <span key={i}>{s.text}</span>
            ) : (
              <span key={i} className={s.type === 'add' ? 'diff-add' : 'diff-del'}>
                {s.text}
              </span>
            ),
          )
        )}
      </div>
    </div>
  );
}
