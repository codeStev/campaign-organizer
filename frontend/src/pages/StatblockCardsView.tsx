import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { Button } from '../components/ui/button';
import { Statblock, FieldTemplate } from '../api/client';
import { orderedStatEntries } from '../lib/statblockDisplay';
import { renderMarkdown } from '../lib/markdown';

interface Props {
  statblocks: Statblock[];
  templates: FieldTemplate[];
  title: string;
  onClose: () => void;
}

/**
 * Prints statblocks as compact cut-out cards for behind the GM screen (ADR-0037).
 * Cards tile several per page and never split across a page break. Portalled to
 * <body> so the print CSS (ADR-0035) can hide the app.
 */
export function StatblockCardsView({ statblocks, templates, title, onClose }: Props) {
  return (
    <NewWindowPortal title={`Cards — ${title}`} onClose={onClose}>
      <div className="print-toolbar">
        <strong>Statblock cards</strong>
        <span className="muted">{title}</span>
        <span className="print-toolbar-spacer" />
        <PrintButton disabled={statblocks.length === 0} />
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>

      <div className="print-doc card-sheet">
        {statblocks.length === 0 && <p className="print-status">No statblocks to print.</p>}
        {statblocks.map((sb) => {
          const stats = orderedStatEntries(sb.stats, sb.templateId, templates);
          return (
            <div key={sb.id} className="stat-card">
              <div className="stat-card-name">{sb.name}</div>
              {stats.length > 0 && (
                <dl className="stat-card-stats">
                  {stats.map((entry) => (
                    <div key={entry.key} className="stat-card-stat">
                      <dt>{entry.label}</dt>
                      {entry.type === 'TEXTAREA' ? (
                        <dd
                          className="preview-body"
                          dangerouslySetInnerHTML={{ __html: renderMarkdown(String(entry.value)) }}
                        />
                      ) : (
                        <dd>{String(entry.value)}</dd>
                      )}
                    </div>
                  ))}
                </dl>
              )}
              {sb.notes && (
                <div
                  className="stat-card-notes preview-body"
                  dangerouslySetInnerHTML={{ __html: renderMarkdown(sb.notes) }}
                />
              )}
            </div>
          );
        })}
      </div>
    </NewWindowPortal>
  );
}
