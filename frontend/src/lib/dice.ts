// Mirrors the backend tables DiceExpression range parser (FR-40) so the
// builder shows the same min/max live while typing. Keep in sync.

export interface DiceRange {
  min: number;
  max: number;
}

const TERM = /([+-]?)\s*(?:(\d*)d(\d+)(?:k([hl])(\d+))?|(\d+))/i;
const MAX_DICE = 100;
const MAX_SIDES = 1000;

/** Inclusive range of totals an expression can produce, or null when invalid. */
export function diceRange(expression: string): DiceRange | null {
  if (!expression || !expression.trim()) return null;
  const expr = expression.replaceAll(/\s+/g, '');
  let min = 0;
  let max = 0;
  let matchedChars = 0;
  let sawTerm = false;

  let match: RegExpExecArray | null;
  let rest = expr;
  // Exec-loop over a shrinking string keeps indices aligned with contiguous matching.
  while ((match = TERM.exec(rest)) !== null) {
    if (match.index !== 0) break; // gap => invalid character sequence
    matchedChars += match[0].length;
    sawTerm = true;
    const negative = match[1] === '-';
    const plain = match[6];

    let termMin: number;
    let termMax: number;
    if (plain !== undefined) {
      termMin = termMax = Number(plain);
    } else {
      const count = match[2] === '' ? 1 : Number(match[2]);
      const sides = Number(match[3]);
      if (count < 1 || count > MAX_DICE) return null;
      if (sides < 1 || sides > MAX_SIDES) return null;
      if (match[4] !== undefined) {
        const kept = Number(match[5]);
        if (kept < 1 || kept > count) return null;
        termMin = kept;
        termMax = kept * sides;
      } else {
        termMin = count;
        termMax = count * sides;
      }
    }
    if (negative) {
      min -= termMax;
      max -= termMin;
    } else {
      min += termMin;
      max += termMax;
    }
    rest = rest.slice(match[0].length);
    if (rest.length === 0) break;
  }
  if (!sawTerm || matchedChars !== expr.length) return null;
  return { min, max };
}
