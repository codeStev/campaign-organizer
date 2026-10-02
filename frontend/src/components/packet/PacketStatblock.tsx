import { ReactNode } from 'react';
import { FieldTemplate, GlobalFieldTemplate, Statblock } from '../../api/client';
import { orderedStatEntries, StatEntry, templateIdOf } from '../../lib/statblockDisplay';
import { renderMarkdown } from '../../lib/markdown';

interface Props {
  statblock: Statblock;
  templates: Array<FieldTemplate | GlobalFieldTemplate>;
  anchor: string;
}

function displayValue(entry: StatEntry): string {
  if (typeof entry.value === 'boolean') return entry.value ? 'Yes' : 'No';
  return String(entry.value);
}

function isBlank(entry: StatEntry): boolean {
  return entry.value == null || String(entry.value).trim() === '';
}

/**
 * One statblock as a real table (ADR-0118): shaded name row, template
 * sections as sub-header rows, short fields in a label | value | label | value
 * grid and long text fields as full-width rows.
 */
export function PacketStatblock({ statblock: sb, templates, anchor }: Props) {
  const entries = orderedStatEntries(sb.stats, templateIdOf(sb), templates).filter((e) => !isBlank(e));

  // Consecutive entries of the same section; stray keys form their own trailing group.
  const groups: Array<{ section?: string; entries: StatEntry[] }> = [];
  for (const entry of entries) {
    const last = groups[groups.length - 1];
    if (last && last.section === entry.section) last.entries.push(entry);
    else groups.push({ section: entry.section, entries: [entry] });
  }
  const showSections = groups.some((g) => g.section);

  const rows: ReactNode[] = [];
  groups.forEach((group, gi) => {
    if (showSections) {
      rows.push(
        <tr key={`s${gi}`} className="print-statblock-section">
          <th colSpan={4}>{group.section || 'Other'}</th>
        </tr>,
      );
    }
    let shortRun: StatEntry[] = [];
    const flushShort = (key: string) => {
      for (let i = 0; i < shortRun.length; i += 2) {
        const [a, b] = [shortRun[i], shortRun[i + 1]];
        rows.push(
          <tr key={`${key}-${i}`}>
            <th>{a.label}</th>
            <td colSpan={b ? 1 : 3}>{displayValue(a)}</td>
            {b && (
              <>
                <th>{b.label}</th>
                <td>{displayValue(b)}</td>
              </>
            )}
          </tr>,
        );
      }
      shortRun = [];
    };
    group.entries.forEach((entry, ei) => {
      if (entry.type === 'TEXTAREA') {
        flushShort(`g${gi}-${ei}`);
        rows.push(
          <tr key={`t${gi}-${ei}`} className="print-statblock-long">
            <td colSpan={4}>
              <strong>{entry.label}</strong>
              <div
                className="preview-body"
                dangerouslySetInnerHTML={{ __html: renderMarkdown(String(entry.value)) }}
              />
            </td>
          </tr>,
        );
      } else {
        shortRun.push(entry);
      }
    });
    flushShort(`g${gi}-end`);
  });

  return (
    <table className="print-statblock-table" id={anchor}>
      <thead>
        <tr>
          <th colSpan={4} className="print-statblock-name">
            {sb.name}
          </th>
        </tr>
      </thead>
      <tbody>
        {rows}
        {sb.notes && (
          <tr className="print-statblock-long">
            <td colSpan={4}>
              <strong>Notes</strong>
              <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(sb.notes) }} />
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}
