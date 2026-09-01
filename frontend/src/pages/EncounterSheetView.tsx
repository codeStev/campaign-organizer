import { useEffect, useMemo, useState } from 'react';
import { characterSheetsApi, CharacterSheet, FieldTemplate, GlobalFieldTemplate, Statblock } from '../api/client';
import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Checkbox } from '../components/ui/checkbox';
import { orderedStatEntries, templateIdOf } from '../lib/statblockDisplay';

interface Props {
  worldId: string;
  statblocks: Statblock[];
  templates: FieldTemplate[];
  globalTemplates: GlobalFieldTemplate[];
  onClose: () => void;
}

interface EntryState {
  qty: number;
  maxHp: string;
}

/** First numeric value under an HP-like key ("hp", "max_hp", "Hit Points"). */
export function detectMaxHp(values: Record<string, unknown>): string {
  const entries = Object.entries(values);
  for (const [key, value] of entries) {
    if (!/hp|hit.?points/i.test(key)) continue;
    const n = Number(value);
    if (value !== '' && !Number.isNaN(n) && n > 0) return String(Math.floor(n));
  }
  return '';
}

/**
 * FR-44: printable combat tracker over picked statblocks (and optionally PC
 * sheets) — one row per combatant with an initiative column, HP tick-boxes
 * and the statblock's quick numbers. Pure assembly, nothing persisted; run
 * from paper like the rest of the session material.
 */
export function EncounterSheetView({ worldId, statblocks, templates, globalTemplates, onClose }: Props) {
  const allTemplates = useMemo(() => [...templates, ...globalTemplates], [templates, globalTemplates]);
  const [entries, setEntries] = useState<Record<string, EntryState>>(() =>
    Object.fromEntries(
      statblocks.map((sb) => [sb.id, { qty: 1, maxHp: detectMaxHp(sb.stats) }]),
    ),
  );
  const [sheets, setSheets] = useState<CharacterSheet[]>([]);
  const [includedSheets, setIncludedSheets] = useState<Set<string>>(new Set());
  const { opts: printOpts, setOpts: setPrintOpts, docProps: printDocProps } = usePrintOptions();

  useEffect(() => {
    let active = true;
    characterSheetsApi(worldId)
      .list()
      .then((all) => {
        if (!active) return;
        setSheets(all);
        setIncludedSheets(new Set());
      })
      .catch(() => {
        /* PC participation is optional; the tracker works without sheets. */
      });
    return () => {
      active = false;
    };
  }, [worldId]);

  function patch(id: string, p: Partial<EntryState>) {
    setEntries((e) => ({ ...e, [id]: { ...e[id], ...p } }));
  }

  type Row = { key: string; name: string; maxHp: string; quickStats: string };

  const rows = useMemo<Row[]>(() => {
    const out: Row[] = [];
    for (const sb of statblocks) {
      const e = entries[sb.id];
      const qty = Math.max(0, Math.min(20, e?.qty ?? 1));
      const quick = quickStats(sb.stats ?? {}, templateIdOf(sb), allTemplates);
      for (let i = 0; i < qty; i++) {
        out.push({
          key: `${sb.id}#${i}`,
          name: qty > 1 ? `${sb.name} ${i + 1}` : sb.name,
          maxHp: e?.maxHp ?? '',
          quickStats: quick,
        });
      }
    }
    for (const cs of sheets) {
      if (!includedSheets.has(cs.id)) continue;
      out.push({
        key: cs.id,
        name: cs.name,
        maxHp: detectMaxHp(cs.values ?? {}),
        quickStats: quickStats(cs.values ?? {}, templateIdOf(cs), allTemplates),
      });
    }
    return out;
  }, [statblocks, entries, sheets, includedSheets, allTemplates]);

  return (
    <NewWindowPortal title="Encounter sheet" onClose={onClose}>
      <div className="print-toolbar">
        <strong>Encounter sheet</strong>
        <span className="muted">{rows.length} combatants</span>
        <PrintOptionsMenu opts={printOpts} onChange={setPrintOpts} />
        <span className="print-toolbar-spacer" />
        <PrintButton disabled={rows.length === 0} />
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>

      {/* Screen-only staging: counts and HP prefill before printing. */}
      <div className="card encounter-setup">
        <h4>Combatants</h4>
        {statblocks.map((sb) => (
          <div key={sb.id} className="encounter-row">
            <span className="encounter-name">{sb.name}</span>
            <label className="muted">
              ×
              <Input
                type="number"
                className="num-input"
                min={0}
                max={20}
                value={entries[sb.id]?.qty ?? 1}
                onChange={(e) => patch(sb.id, { qty: Number(e.target.value) })}
              />
            </label>
            <label className="muted">
              Max HP
              <Input
                className="encounter-hp-input"
                placeholder={detectMaxHp(sb.stats) || '—'}
                value={entries[sb.id]?.maxHp ?? ''}
                onChange={(e) => patch(sb.id, { maxHp: e.target.value })}
              />
            </label>
          </div>
        ))}
        {sheets.length > 0 && (
          <>
            <h4>Player characters</h4>
            {sheets.map((cs) => (
              <label key={cs.id} className="encounter-sheet-pick">
                <Checkbox
                  checked={includedSheets.has(cs.id)}
                  onCheckedChange={() =>
                    setIncludedSheets((prev) => {
                      const next = new Set(prev);
                      if (next.has(cs.id)) next.delete(cs.id);
                      else next.add(cs.id);
                      return next;
                    })
                  }
                />
                <span>{cs.name}</span>
              </label>
            ))}
          </>
        )}
      </div>

      <div className="print-doc" {...printDocProps}>
        <section className="print-cover">
          <h1>Encounter</h1>
          <p className="print-subtitle">
            {new Date().toLocaleDateString()} · {rows.length} combatants
          </p>
        </section>
        <table className="encounter-table">
          <thead>
            <tr>
              <th className="encounter-init">Init</th>
              <th>Combatant</th>
              <th className="encounter-hp">HP</th>
              <th>Key stats</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.key}>
                <td className="encounter-init" />
                <td>{r.name}</td>
                <td className="encounter-hp">
                  <HpBoxes maxHp={r.maxHp} />
                </td>
                <td className="encounter-stats">{r.quickStats}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </NewWindowPortal>
  );
}

/** Ten tick-boxes per group, capped at sixty so ancient dragons stay sane. */
function HpBoxes({ maxHp }: { maxHp: string }) {
  const max = Number(maxHp);
  if (!maxHp || Number.isNaN(max) || max <= 0) {
    return <span className="encounter-hp-blank">____ / ____</span>;
  }
  const shown = Math.min(Math.floor(max), 60);
  return (
    <span className="encounter-hp-boxes" title={`${max} HP`}>
      {Array.from({ length: shown }, (_, i) => (
        <span
          key={i}
          className={'encounter-box' + ((i + 1) % 10 === 0 ? ' encounter-box-gap' : '')}
        >
          {(i + 1) % 10 === 0 ? <small>{i + 1}</small> : null}
        </span>
      ))}
      <span className="encounter-hp-total">
        /{max}
      </span>
    </span>
  );
}

/** Short non-narrative stats for the tracker's last column, e.g. "AC 13 · Speed 30 ft". */
function quickStats(
  values: Record<string, unknown>,
  templateId: string | null | undefined,
  templates: Array<{ id: string; sections: FieldTemplate['sections'] }>,
): string {
  return orderedStatEntries(values, templateId, templates)
    .filter((e) => e.type !== 'TEXTAREA' && String(e.value).trim() !== '')
    .slice(0, 6)
    .map((e) => `${e.label} ${String(e.value)}`)
    .join(' · ');
}
