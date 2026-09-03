import { useEffect, useState } from 'react';
import { rollTablesApi, worldOverviewApi, diceApi, ApiError, RollTable, RollTableEntry, ClockSummary } from '../api/client';
import { DiceRollerWidget } from './DiceRollerWidget';
import { Button } from './ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';

interface Props {
  worldId: string;
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
 * top bar on every /next screen within a world. Reuses the existing dice
 * roller and roll tables (FR-19, FR-40/41) as-is, and the world overview's
 * openClocks (FR-62/ADR-0103) for the mini Clocks view — no new backend.
 */
export function TableToolsDock({ worldId, onAuthExpired, onClose }: Props) {
  const [tables, setTables] = useState<RollTable[]>([]);
  const [selectedTableId, setSelectedTableId] = useState('');
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

  async function rollTable() {
    const table = tables.find((t) => t.id === selectedTableId);
    if (!table) return;
    try {
      const result = await diceApi.roll(table.diceExpression);
      setTableRoll({ total: result.total, breakdown: result.breakdown, entry: matchingEntry(table.entries, result.total) });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) onAuthExpired();
    }
  }

  return (
    <aside className="table-tools-dock">
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
            <Select value={selectedTableId} onValueChange={setSelectedTableId}>
              <SelectTrigger>
                <SelectValue placeholder="Pick a table…" />
              </SelectTrigger>
              <SelectContent>
                {tables.map((t) => (
                  <SelectItem key={t.id} value={t.id}>
                    {t.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button type="button" variant="outline" disabled={!selectedTableId} onClick={() => void rollTable()}>
              Roll
            </Button>
            {tableRoll && (
              <p className="dice-result">
                <span className="dice-total">{tableRoll.total}</span>
                <span className="muted">
                  {tableRoll.breakdown}
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
                <span>{c.title}</span>
                <span className="muted">
                  {c.filledSegments}/{c.totalSegments} · {c.campaignName}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </aside>
  );
}
