import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { worldOverviewApi, ApiError, WorldOverviewStats } from '../api/client';
import { Spinner } from '../components/ui/spinner';
import { Button } from '../components/ui/button';
import { SessionCalendar } from '../components/SessionCalendar';
import { getCampaignColor } from '../lib/campaignColor';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

function formatDate(iso: string): string {
  return new Date(iso + 'T00:00:00').toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  });
}

/** "12m ago" / "3h ago" / "5d ago" — matches the mockup's Recently Edited feed. */
function relativeTime(iso: string): string {
  const ms = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(ms / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

/**
 * World Overview dashboard (docs/ui-overhaul-plan.md Phase 4): stats strip,
 * next-session card, recently-edited feed, Clocks widget, Loose Threads
 * widget, session calendar (ADR-0107) — all one composed read from
 * worldOverviewApi (FR-62, ADR-0102, ADR-0103). No in-world-date widget:
 * nothing in this app persists a campaign's current in-world date yet,
 * and Phase 4 is scoped to composing existing data, not new domain state.
 */
export function NextOverviewPage({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const [stats, setStats] = useState<WorldOverviewStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    worldOverviewApi(worldId)
      .get()
      .then(setStats)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) onAuthExpired();
      })
      .finally(() => setLoading(false));
  }, [worldId, onAuthExpired]);

  if (loading) {
    return (
      <p className="muted loading-row">
        <Spinner /> Loading…
      </p>
    );
  }
  if (!stats) {
    return <p className="muted">Couldn't load the overview.</p>;
  }

  return (
    <div className="next-overview">
      <div className="next-overview-stats">
        <div className="next-overview-stat">
          <span className="next-overview-stat-value">{stats.articleCount}</span>
          <span className="muted">Articles</span>
        </div>
        <div className="next-overview-stat">
          <span className="next-overview-stat-value">{stats.sessionsRunCount}</span>
          <span className="muted">Sessions run</span>
        </div>
        <div className="next-overview-stat">
          <span className="next-overview-stat-value">{stats.openLooseThreads.length}</span>
          <span className="muted">Loose threads</span>
        </div>
        <div className="next-overview-stat next-overview-stat-session">
          {stats.nextSession ? (
            <>
              <span className="next-overview-stat-value">{formatDate(stats.nextSession.date)}</span>
              <span className="muted">
                Next: {stats.nextSession.title} · {stats.nextSession.campaignName}
              </span>
            </>
          ) : (
            <>
              <span className="next-overview-stat-value">—</span>
              <span className="muted">No session scheduled</span>
            </>
          )}
        </div>
      </div>

      <section className="card">
        <h3 className="eyebrow">Session calendar</h3>
        <SessionCalendar
          entries={stats.scheduledSessions.map((s) => ({
            id: s.sessionId,
            date: s.date,
            title: s.title,
            sessionNumber: s.sessionNumber,
            campaignId: s.campaignId,
            campaignName: s.campaignName,
            color: getCampaignColor({ id: s.campaignId, color: s.campaignColor }),
          }))}
          onSelectSession={(entry) =>
            navigate(`/next/worlds/${worldId}/sessions/${entry.campaignId}/${entry.id}`)
          }
        />
      </section>

      <div className="next-overview-grid">
        <section className="card">
          <h3 className="eyebrow">Recently edited</h3>
          {stats.recentlyEdited.length === 0 ? (
            <p className="muted">No articles yet.</p>
          ) : (
            <ul className="next-overview-list">
              {stats.recentlyEdited.map((a) => (
                <li key={a.articleId} className="next-overview-recent-row">
                  <Button variant="link" onClick={() => onOpenArticle(a.articleId)}>
                    {a.title}
                  </Button>
                  <span className="muted">{relativeTime(a.updatedAt)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <h3 className="eyebrow">Clocks</h3>
          {stats.openClocks.length === 0 ? (
            <p className="muted">No clocks in progress.</p>
          ) : (
            <ul className="next-overview-list">
              {stats.openClocks.map((c) => (
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
        </section>

        <section className="card">
          <h3 className="eyebrow">Loose threads</h3>
          {stats.openLooseThreads.length === 0 ? (
            <p className="muted">Nothing open.</p>
          ) : (
            <ul className="next-overview-list">
              {stats.openLooseThreads.map((t) => (
                <li key={t.threadId} className="next-overview-clock">
                  <div className="next-overview-clock-head">
                    <span>{t.text}</span>
                    <span className="muted">{t.campaignName}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
