import { createPortal } from 'react-dom';
import { Statblock } from '../api/client';

interface Props {
  statblocks: Statblock[];
  title: string;
  onClose: () => void;
}

/**
 * Prints statblocks as compact cut-out cards for behind the GM screen (ADR-0037).
 * Cards tile several per page and never split across a page break. Portalled to
 * <body> so the print CSS (ADR-0035) can hide the app.
 */
export function StatblockCardsView({ statblocks, title, onClose }: Props) {
  return createPortal(
    <div className="print-overlay">
      <div className="print-toolbar">
        <strong>Statblock cards</strong>
        <span className="muted">{title}</span>
        <span className="print-toolbar-spacer" />
        <button onClick={() => window.print()} disabled={statblocks.length === 0}>
          🖨 Print
        </button>
        <button className="link-button" onClick={onClose}>
          Close
        </button>
      </div>

      <div className="print-doc card-sheet">
        {statblocks.length === 0 && <p className="print-status">No statblocks to print.</p>}
        {statblocks.map((sb) => {
          const stats = Object.entries(sb.stats ?? {});
          return (
            <div key={sb.id} className="stat-card">
              <div className="stat-card-name">{sb.name}</div>
              {stats.length > 0 && (
                <dl className="stat-card-stats">
                  {stats.map(([k, v]) => (
                    <div key={k} className="stat-card-stat">
                      <dt>{k}</dt>
                      <dd>{String(v)}</dd>
                    </div>
                  ))}
                </dl>
              )}
              {sb.notes && <p className="stat-card-notes">{sb.notes}</p>}
            </div>
          );
        })}
      </div>
    </div>,
    document.body,
  );
}
