import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { calendarsApi, Calendar, CalendarMonthInput, ApiError } from '../api/client';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

interface Draft {
  id: string | null;
  name: string;
  daysPerWeek: string;
  months: CalendarMonthInput[];
}

const EMPTY_DRAFT: Draft = {
  id: null,
  name: '',
  daysPerWeek: '',
  months: [{ name: '', days: 30 }],
};

export function CalendarsView({ worldId, onAuthExpired }: Props) {
  const api = useMemo(() => calendarsApi(worldId), [worldId]);
  const [list, setList] = useState<Calendar[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [error, setError] = useState<string | null>(null);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(async () => {
    try {
      setList(await api.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  function edit(calendar: Calendar) {
    setDraft({
      id: calendar.id,
      name: calendar.name,
      daysPerWeek: calendar.daysPerWeek != null ? String(calendar.daysPerWeek) : '',
      months: calendar.months.map((m) => ({ ...m })),
    });
  }

  function setMonth(index: number, patch: Partial<CalendarMonthInput>) {
    setDraft((d) => ({
      ...d,
      months: d.months.map((m, i) => (i === index ? { ...m, ...patch } : m)),
    }));
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    const months = draft.months.filter((m) => m.name.trim() !== '');
    if (months.length === 0) {
      setError('Add at least one named month');
      return;
    }
    setError(null);
    const body = {
      name: draft.name,
      daysPerWeek: draft.daysPerWeek ? Number(draft.daysPerWeek) : null,
      months,
    };
    try {
      if (draft.id) await api.update(draft.id, body);
      else await api.create(body);
      setDraft(EMPTY_DRAFT);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  async function remove(calendar: Calendar) {
    try {
      await api.remove(calendar.id);
      if (draft.id === calendar.id) setDraft(EMPTY_DRAFT);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <button onClick={() => setDraft(EMPTY_DRAFT)}>+ New calendar</button>
        <ul className="article-list">
          {list.map((c) => (
            <li key={c.id}>
              <button
                className={c.id === draft.id ? 'article-link active' : 'article-link'}
                onClick={() => edit(c)}
              >
                <span>{c.name}</span>
                <small className="muted">{c.months.length} months</small>
              </button>
            </li>
          ))}
          {loading && <li className="muted">Loading…</li>}
          {!loading && list.length === 0 && <li className="muted">No calendars yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        <form className="card" onSubmit={save}>
          <strong>{draft.id ? 'Edit calendar' : 'New calendar'}</strong>
          <input
            placeholder="Calendar name"
            value={draft.name}
            onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            required
          />
          <label className="muted">
            Days per week (optional)
            <input
              type="number"
              min="1"
              value={draft.daysPerWeek}
              onChange={(e) => setDraft({ ...draft, daysPerWeek: e.target.value })}
            />
          </label>

          <strong className="muted">Months</strong>
          {draft.months.map((month, i) => (
            <div key={i} className="month-row">
              <input
                placeholder={`Month ${i + 1} name`}
                value={month.name}
                onChange={(e) => setMonth(i, { name: e.target.value })}
              />
              <input
                type="number"
                min="1"
                value={month.days}
                onChange={(e) => setMonth(i, { days: Number(e.target.value) })}
              />
              <button
                type="button"
                className="link-button danger"
                onClick={() => setDraft((d) => ({ ...d, months: d.months.filter((_, j) => j !== i) }))}
              >
                ✕
              </button>
            </div>
          ))}
          <button
            type="button"
            className="link-button"
            onClick={() => setDraft((d) => ({ ...d, months: [...d.months, { name: '', days: 30 }] }))}
          >
            + Add month
          </button>

          <div className="editor-actions">
            <button type="submit" disabled={draft.name.length === 0}>
              {draft.id ? 'Save calendar' : 'Create calendar'}
            </button>
            {draft.id && (
              <button
                type="button"
                className="link-button danger"
                onClick={() => remove(list.find((c) => c.id === draft.id)!)}
              >
                Delete
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
