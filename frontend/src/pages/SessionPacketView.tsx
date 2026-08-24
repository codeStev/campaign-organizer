import { useEffect, useState } from 'react';
import { NewWindowPortal } from '../components/NewWindowPortal';
import { Button } from '../components/ui/button';
import { sessionsApi, fieldTemplatesApi, SessionPacket, FieldTemplate } from '../api/client';
import { orderedStatEntries } from '../lib/statblockDisplay';
import { renderMarkdown } from '../lib/markdown';

interface Props {
  worldId: string;
  campaignId: string;
  sessionId: string;
  onClose: () => void;
  onError: (err: unknown) => void;
}

/**
 * Full-screen print packet for one session (ADR-0036): its scheduled beats, the
 * articles those beats reference, and the campaign's statblocks — everything for
 * the night on one printable document. Portalled to <body> so print CSS can hide
 * the app (reuses the print styles from ADR-0035).
 */
export function SessionPacketView({ worldId, campaignId, sessionId, onClose, onError }: Props) {
  const [packet, setPacket] = useState<SessionPacket | null>(null);
  const [templates, setTemplates] = useState<FieldTemplate[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    sessionsApi(worldId, campaignId)
      .packet(sessionId)
      .then((p) => active && setPacket(p))
      .catch(onError)
      .finally(() => active && setLoading(false));
    fieldTemplatesApi(worldId)
      .list('STATBLOCK')
      .then((t) => active && setTemplates(t))
      .catch(onError);
    return () => {
      active = false;
    };
  }, [worldId, campaignId, sessionId, onError]);

  const s = packet?.session;
  const heading = s
    ? `${s.sessionNumber != null ? `Session ${s.sessionNumber}: ` : ''}${s.title}`
    : 'Session';

  return (
    <NewWindowPortal title={`Packet — ${heading}`} onClose={onClose}>
      <div className="print-toolbar">
        <strong>Session packet</strong>
        <span className="print-toolbar-spacer" />
        <Button onClick={() => window.print()} disabled={loading || !packet}>
          🖨 Print
        </Button>
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>

      <div className="print-doc">
        <section className="print-cover">
          <h1>{heading}</h1>
          <p className="print-subtitle">
            {packet?.campaignName}
            {s?.date ? ` · ${s.date}` : ''}
          </p>
        </section>

        {loading && <p className="print-status">Preparing packet…</p>}

        {!loading && packet && (
          <>
            {(s?.summary || s?.notes) && (
              <section className="print-article">
                <h1>Session overview</h1>
                {s?.summary && (
                  <>
                    <p className="print-kicker">summary</p>
                    <div
                      className="preview-body"
                      dangerouslySetInnerHTML={{ __html: renderMarkdown(s.summary) }}
                    />
                  </>
                )}
                {s?.notes && (
                  <>
                    <p className="print-kicker">gm notes</p>
                    <div
                      className="preview-body"
                      dangerouslySetInnerHTML={{ __html: renderMarkdown(s.notes) }}
                    />
                  </>
                )}
              </section>
            )}

            <section className="print-article">
              <h1>Beats</h1>
              {packet.beats.length === 0 && (
                <p className="print-status">No beats scheduled into this session.</p>
              )}
              <ol className="print-beats">
                {packet.beats.map((b) => (
                  <li key={b.id} className="print-beat">
                    <span className="print-beat-title">
                      {b.done ? '☑' : '☐'} {b.title}
                    </span>
                    {b.arcTitle && <span className="print-beat-arc"> — {b.arcTitle}</span>}
                    {b.body && (
                      <div
                        className="preview-body"
                        dangerouslySetInnerHTML={{ __html: renderMarkdown(b.body) }}
                      />
                    )}
                  </li>
                ))}
              </ol>
            </section>

            {packet.articles.length > 0 && (
              <div className="print-divider">
                <h1>Referenced material</h1>
                <p className="print-kicker">articles linked from this session's beats</p>
              </div>
            )}

            {packet.articles.map((a) => (
              <article key={a.id} className="print-article">
                <h1>{a.title}</h1>
                <p className="print-kicker">{a.template.toLowerCase()}</p>
                {/* eslint-disable-next-line react/no-danger */}
                <div
                  className="print-body"
                  dangerouslySetInnerHTML={{ __html: a.bodyHtml || '<p><em>(empty)</em></p>' }}
                />
              </article>
            ))}

            {packet.maps.map((m) => (
              <section key={m.id} className="print-map-section">
                <h1>{m.name}</h1>
                {m.imageUrl && (
                  <div className="print-map-figure">
                    <img src={m.imageUrl} alt={m.name} />
                    {m.pins.map((p, i) => (
                      <span
                        key={i}
                        className="print-map-marker"
                        style={{ left: `${p.x * 100}%`, top: `${p.y * 100}%` }}
                      >
                        {i + 1}
                      </span>
                    ))}
                  </div>
                )}
                {m.pins.length > 0 && (
                  <ol className="print-map-legend">
                    {m.pins.map((p, i) => (
                      <li key={i}>{p.label || 'Unlabeled pin'}</li>
                    ))}
                  </ol>
                )}
              </section>
            ))}

            {packet.statblocks.length > 0 && (
              <section className="print-map-section">
                <h1>Statblocks</h1>
                {packet.statblocks.map((sb) => (
                  <div key={sb.id} className="print-statblock">
                    <h2>{sb.name}</h2>
                    <dl className="print-stats">
                      {orderedStatEntries(sb.stats, sb.templateId, templates).map((entry) => (
                        <div key={entry.key} className="print-stat">
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
                    {sb.notes && (
                      <div
                        className="preview-body"
                        dangerouslySetInnerHTML={{ __html: renderMarkdown(sb.notes) }}
                      />
                    )}
                  </div>
                ))}
              </section>
            )}
          </>
        )}
      </div>
    </NewWindowPortal>
  );
}
