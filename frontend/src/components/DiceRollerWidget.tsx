import { FormEvent, useState } from 'react';
import { diceApi, DiceRollResult, ApiError } from '../api/client';

interface Props {
  onAuthExpired: () => void;
}

const PRESETS = ['1d20', '2d20kh1', '2d20kl1', '1d20+5', '2d6+3', '1d100'];

export function DiceRollerWidget({ onAuthExpired }: Props) {
  const [expr, setExpr] = useState('1d20');
  const [result, setResult] = useState<DiceRollResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function roll(expression: string) {
    setError(null);
    try {
      setResult(await diceApi.roll(expression));
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Bad expression');
    }
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    if (expr.trim()) void roll(expr.trim());
  }

  return (
    <div className="card dice-widget">
      <strong>🎲 Dice</strong>
      <form className="dice-form" onSubmit={submit}>
        <input value={expr} onChange={(e) => setExpr(e.target.value)} placeholder="2d6+3" />
        <button type="submit">Roll</button>
      </form>
      <div className="dice-presets">
        {PRESETS.map((p) => (
          <button key={p} type="button" className="type-chip" onClick={() => roll(p)}>
            {p}
          </button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}
      {result && (
        <div className="dice-result">
          <span className="dice-total">{result.total}</span>
          <span className="muted">{result.breakdown}</span>
        </div>
      )}
    </div>
  );
}
