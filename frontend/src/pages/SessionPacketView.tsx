import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { sessionsApi, SessionPacket } from '../api/client';

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
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    sessionsApi(worldId, campaignId)
      .packet(sessionId)
      .then((p) => active && setPacket(p))
      .catch(onError)
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [worldId, campaignId, sessionId, onError]);

  const s = packet?.session;
  const heading = s
    ? `${s.sessionNumber != null ? `Session ${s.sessionNumber}: ` : ''}${s.title}`
    : 'Session';

  return createPortal(
    <div className="print-overlay">
      <div className="print-toolbar">
        <strong>Session packet</strong>
        <span className="print-toolbar-spacer" />
        <button onClick={() => window.print()} disabled={loading || !packet}>
          🖨 Print
        </button>
        <button className="link-button" onClick={onClose}>
          Close
        </button>
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
                    <p>{s.summary}</p>
                  </>
                )}
                {s?.notes && (
                  <>
                    <p className="print-kicker">gm notes</p>
                    <p className="print-prewrap">{s.notes}</p>
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
                    {b.body && <p className="print-prewrap">{b.body}</p>}
                  </li>
                ))}
              </ol>
            </section>

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

            {packet.statblocks.length > 0 && (
              <section className="print-map-section">
                <h1>Statblocks</h1>
                {packet.statblocks.map((sb) => (
                  <div key={sb.id} className="print-statblock">
                    <h2>{sb.name}</h2>
                    <dl className="print-stats">
                      {Object.entries(sb.stats ?? {}).map(([k, v]) => (
                        <div key={k} className="print-stat">
                          <dt>{k}</dt>
                          <dd>{String(v)}</dd>
                        </div>
                      ))}
                    </dl>
                    {sb.notes && <p className="print-prewrap">{sb.notes}</p>}
                  </div>
                ))}
              </section>
            )}
          </>
        )}
      </div>
    </div>,
    document.body,
  );
}
