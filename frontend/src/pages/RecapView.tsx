import { useEffect, useMemo, useState } from 'react';
import { arcsApi, sessionsApi, Arc, Beat, Session } from '../api/client';
import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';
import { Button } from '../components/ui/button';
import { renderMarkdown } from '../lib/markdown';
import { fetchCampaignBeats } from '../lib/beats';

interface Props {
  worldId: string;
  campaignId: string;
  campaignName: string;
  onClose: () => void;
  onError: (err: unknown) => void;
}

/**
 * FR-45: "the story so far" as one printable recap to open the next session
 * with — past sessions' summaries plus completed story beats, in play order.
 * Deliberately read-only assembly over existing endpoints, and session GM
 * notes are never fetched: the recap goes on the table.
 */
export function RecapView({ worldId, campaignId, campaignName, onClose, onError }: Props) {
  const [arcs, setArcs] = useState<Arc[]>([]);
  const [beats, setBeats] = useState<Beat[]>([]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const { opts: printOpts, setOpts: setPrintOpts, docProps: printDocProps } = usePrintOptions();

  useEffect(() => {
    let active = true;
    Promise.all([arcsApi(worldId, campaignId).list(), sessionsApi(worldId, campaignId).list()])
      .then(async ([arcList, sessionList]) => {
        if (!active) return;
        setArcs(arcList);
        setSessions(sessionList);
        const beatList = await fetchCampaignBeats(worldId, campaignId, arcList);
        if (active) setBeats(beatList);
      })
      .catch(onError)
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [worldId, campaignId, onError]);

  const orderedSessions = useMemo(
    () =>
      [...sessions].sort((a, b) => {
        const na = a.sessionNumber ?? Number.MAX_SAFE_INTEGER;
        const nb = b.sessionNumber ?? Number.MAX_SAFE_INTEGER;
        if (na !== nb) return na - nb;
        return (a.date ?? a.createdAt).localeCompare(b.date ?? b.createdAt);
      }),
    [sessions],
  );

  const doneBeatsByArc = useMemo(() => {
    const byArc = new Map<string, Beat[]>();
    for (const b of beats.filter((x) => x.done)) {
      const list = byArc.get(b.arcId) ?? [];
      list.push(b);
      byArc.set(b.arcId, list);
    }
    for (const list of byArc.values()) list.sort((a, b) => a.position - b.position);
    return byArc;
  }, [beats]);

  return (
    <NewWindowPortal title={`Recap — ${campaignName}`} onClose={onClose}>
      <div className="print-toolbar">
        <strong>Session recap</strong>
        <PrintOptionsMenu opts={printOpts} onChange={setPrintOpts} />
        <span className="print-toolbar-spacer" />
        <PrintButton disabled={loading} />
        <Button variant="link" onClick={onClose}>
          Close
        </Button>
      </div>
      <div className="print-doc" {...printDocProps}>
        <section className="print-cover">
          <h1>{campaignName}</h1>
          <p className="print-subtitle">The story so far</p>
        </section>

        <h2>Sessions</h2>
        {orderedSessions.length === 0 && <p className="muted">No sessions logged yet.</p>}
        {orderedSessions.map((s) => (
          <section key={s.id} className="recap-session">
            <h3>
              {s.sessionNumber != null ? `#${s.sessionNumber} ` : ''}
              {s.title}
              {s.date && <span className="print-kicker"> — {s.date}</span>}
            </h3>
            {/* Summary only; GM notes are private and never rendered here. */}
            {s.summary && <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(s.summary) }} />}
          </section>
        ))}

        <h2>Story so far</h2>
        {[...arcs]
          .sort((a, b) => a.position - b.position)
          .map((arc) => {
            const done = doneBeatsByArc.get(arc.id) ?? [];
            if (done.length === 0) return null;
            return (
              <section key={arc.id} className="recap-arc">
                <h3>{arc.title}</h3>
                <ul>
                  {done.map((b) => (
                    <li key={b.id}>
                      <strong>{b.title}</strong>
                      {b.body && (
                        <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(b.body) }} />
                      )}
                    </li>
                  ))}
                </ul>
              </section>
            );
          })}
        {[...doneBeatsByArc.values()].flat().length === 0 && (
          <p className="muted">No completed beats yet.</p>
        )}
      </div>
    </NewWindowPortal>
  );
}
