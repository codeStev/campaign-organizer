import { useEffect, useState } from 'react';
import { rollTablesApi, worldOverviewApi, diceApi, ApiError, RollTable, RollTableEntry, ClockSummary } from '../api/client';
import { DiceRollerWidget } from './DiceRollerWidget';
import { Button } from './ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from './ui/sheet';
import { useIsMobile } from '../hooks/use-mobile';

interface Props {
  worldId: string;
  open: boolean;
  onAuthExpired: () => void;
  onClose: () => void;
}

/** Row a rolled total lands on: explicit range first, else the catch-all row. Mirrors TablesView's matchingEntryIndex. */
function matchingEntry(entries: RollTableEntry[], total: number): RollTableEntry | null {
  const explicit = entries.find(
    (e) => e.minResult != null && e.maxResult != null && e.minResult <= total && total <= e.maxResult,
  );
  if (explicit) return explicit;
  return entries.find((e) => e.minResult == null && e.maxResult == null) ?? null;
}

/**
 * Persistent Table Tools dock (docs/ui-overhaul-plan.md Phase 4): dice
 * roller + roll-table shortcuts + a mini Clocks view, toggleable from the
 * top bar on every /next screen within a world. Always mounted inside the
 * sidebar-shell-next flex row (ADR-0105 follow-up) so it slides in from the
 * right the same way the left nav sidebar slides in, via a width
 * transition on the outer `.table-tools-dock`, rather than floating as a
 * fixed overlay above the top bar/content. Reuses the existing dice roller
 * and roll tables (FR-19, FR-40/41) as-is, and the world overview's
 * openClocks (FR-62/ADR-0103) for the mini Clocks view — no new backend.
 */
export function TableToolsDock({ worldId, open, onAuthExpired, onClose }: Props) {
  const isMobile = useIsMobile();
  const [tables, setTables] = useState<RollTable[]>([]);
  const [rolledTableId, setRolledTableId] = useState('');
  const [tableRoll, setTableRoll] = useState<{ total: number; breakdown: string; entry: RollTableEntry | null } | null>(null);
  const [clocks, setClocks] = useState<ClockSummary[]>([]);

  useEffect(() => {
    rollTablesApi(worldId)
      .list()
      .then(setTables)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) onAuthExpired();
      });
    worldOverviewApi(worldId)
      .get()
      .then((stats) => setClocks(stats.openClocks))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) onAuthExpired();
      });
  }, [worldId, onAuthExpired]);

  async function rollTable(table: RollTable) {
    setRolledTableId(table.id);
    try {
      const result = await diceApi.roll(table.diceExpression);
      setTableRoll({ total: result.total, breakdown: result.breakdown, entry: matchingEntry(table.entries, result.total) });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) onAuthExpired();
    }
  }

  const content = (
    <div className="table-tools-dock-inner">
      <div className="table-tools-dock-head">
        <strong>Table Tools</strong>
        <Button variant="link" size="sm" onClick={onClose}>
          ✕
        </Button>
      </div>

      <DiceRollerWidget onAuthExpired={onAuthExpired} />

      <div className="card">
        <strong>🎲 Roll tables</strong>
        {tables.length === 0 ? (
          <p className="muted">No roll tables in this world yet.</p>
        ) : (
          <>
            <ul className="table-tools-roll-list">
              {tables.map((t) => (
                <li key={t.id}>
                  <button type="button" className="table-tools-roll-item" onClick={() => void rollTable(t)}>
                    {t.title}
                  </button>
                </li>
              ))}
            </ul>
            {tableRoll && (
              <p className="dice-result">
                <span className="dice-total">{tableRoll.total}</span>
                <span className="muted">
                  {tables.find((t) => t.id === rolledTableId)?.title} — {tableRoll.breakdown}
                  {tableRoll.entry ? ` — ${tableRoll.entry.body}` : ' — no entry covers this result'}
                </span>
              </p>
            )}
          </>
        )}
      </div>

      <div className="card">
        <strong>⏱ Clocks</strong>
        {clocks.length === 0 ? (
          <p className="muted">No clocks in progress.</p>
        ) : (
          <ul className="next-overview-list">
            {clocks.map((c) => (
              <li key={c.clockId} className="next-overview-clock">
                <div className="next-overview-clock-head">
                  <span>{c.title}</span>
                  <span className="muted">
                    {c.filledSegments}/{c.totalSegments} · {c.campaignName}
                  </span>
                </div>
                <div className="next-overview-progress">
                  <div
                    className="next-overview-progress-fill"
                    style={{ width: `${(100 * c.filledSegments) / c.totalSegments}%` }}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );

  // A fixed 300px in-flow column doesn't fit a phone viewport — on mobile
  // this becomes a real overlay (Sheet) instead of a permanent flex
  // sibling squeezing the content column.
  if (isMobile) {
    return (
      <Sheet open={open} onOpenChange={(next) => !next && onClose()}>
        <SheetContent side="right" showCloseButton={false} className="table-tools-sheet">
          <SheetHeader className="sr-only">
            <SheetTitle>Table Tools</SheetTitle>
            <SheetDescription>Dice roller, roll tables, and clocks.</SheetDescription>
          </SheetHeader>
          {content}
        </SheetContent>
      </Sheet>
    );
  }

  return (
    <aside className="table-tools-dock" data-open={open}>
      {content}
    </aside>
  );
}
