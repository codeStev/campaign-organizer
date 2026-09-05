import { useMemo, useState } from 'react';
import { Button } from './ui/button';
import { TruncatedLabel } from './TruncatedLabel';

export interface SessionCalendarEntry {
  id: string;
  /** ISO date (yyyy-mm-dd), as stored on Session.date. */
  date: string;
  title: string;
  sessionNumber?: number | null;
  campaignId: string;
  campaignName?: string;
  /** Resolved display color (see lib/campaignColor.ts) — this component
   * doesn't know about campaigns, only about already-colored entries. */
  color: string;
}

interface Props {
  entries: SessionCalendarEntry[];
  onSelectSession?: (entry: SessionCalendarEntry) => void;
  emptyLabel?: string;
}

function pad2(n: number): string {
  return String(n).padStart(2, '0');
}

function toIsoDate(year: number, month: number, day: number): string {
  return `${year}-${pad2(month + 1)}-${pad2(day)}`;
}

function startOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

/**
 * A plain Gregorian month-grid calendar (ADR-0107) — native Date math, no
 * dependency. Callers resolve each entry's display color up front (see
 * lib/campaignColor.ts) so this component stays agnostic of campaigns.
 */
export function SessionCalendar({ entries, onSelectSession, emptyLabel = 'No sessions scheduled.' }: Props) {
  const [viewMonth, setViewMonth] = useState(() => startOfMonth(new Date()));

  const entriesByDate = useMemo(() => {
    const map = new Map<string, SessionCalendarEntry[]>();
    for (const e of entries) {
      const bucket = map.get(e.date) ?? [];
      bucket.push(e);
      map.set(e.date, bucket);
    }
    return map;
  }, [entries]);

  const year = viewMonth.getFullYear();
  const month = viewMonth.getMonth();
  const firstWeekday = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const todayIso = toIsoDate(new Date().getFullYear(), new Date().getMonth(), new Date().getDate());

  const cells: { day: number | null; iso: string | null }[] = [];
  for (let i = 0; i < firstWeekday; i++) cells.push({ day: null, iso: null });
  for (let day = 1; day <= daysInMonth; day++) cells.push({ day, iso: toIsoDate(year, month, day) });
  while (cells.length % 7 !== 0) cells.push({ day: null, iso: null });

  const hasAnyThisMonth = cells.some((c) => c.iso && entriesByDate.has(c.iso));

  return (
    <div className="session-calendar">
      <div className="session-calendar-nav">
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setViewMonth(new Date(year, month - 1, 1))}
        >
          ‹
        </Button>
        <strong>{viewMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}</strong>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setViewMonth(new Date(year, month + 1, 1))}
        >
          ›
        </Button>
        <Button type="button" variant="link" size="sm" onClick={() => setViewMonth(startOfMonth(new Date()))}>
          Today
        </Button>
      </div>
      <div className="session-calendar-grid">
        {WEEKDAY_LABELS.map((label) => (
          <div key={label} className="session-calendar-weekday muted">
            {label}
          </div>
        ))}
        {cells.map((cell, i) => (
          <div
            key={i}
            className={cell.iso === todayIso ? 'session-calendar-day today' : 'session-calendar-day'}
          >
            {cell.day != null && (
              <>
                <span className="session-calendar-day-number muted">{cell.day}</span>
                {(entriesByDate.get(cell.iso!) ?? []).map((entry) => (
                  <button
                    key={entry.id}
                    type="button"
                    className="session-calendar-pill"
                    style={{ backgroundColor: entry.color }}
                    onClick={() => onSelectSession?.(entry)}
                  >
                    <TruncatedLabel
                      label={
                        entry.campaignName
                          ? `${entry.campaignName} — ${entry.title}`
                          : entry.title
                      }
                    >
                      {entry.sessionNumber != null ? `#${entry.sessionNumber} ` : ''}
                      {entry.title}
                    </TruncatedLabel>
                  </button>
                ))}
              </>
            )}
          </div>
        ))}
      </div>
      {!hasAnyThisMonth && <p className="muted session-calendar-empty">{emptyLabel}</p>}
    </div>
  );
}
