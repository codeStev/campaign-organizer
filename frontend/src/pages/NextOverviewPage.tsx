import { useEffect, useState } from 'react';
import { worldOverviewApi, ApiError, WorldOverviewStats } from '../api/client';
import { Spinner } from '../components/ui/spinner';
import { Button } from '../components/ui/button';

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

/**
 * World Overview dashboard (docs/ui-overhaul-plan.md Phase 4): stats strip,
 * next-session card, recently-edited feed, Clocks widget, Loose Threads
 * widget — all one composed read from worldOverviewApi (FR-62, ADR-0102,
 * ADR-0103). No in-world-date widget: nothing in this app persists a
 * campaign's current in-world date yet, and Phase 4 is scoped to
 * composing existing data, not new domain state.
 */
export function NextOverviewPage({ worldId, onOpenArticle, onAuthExpired }: Props) {
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

      <div className="next-overview-grid">
        <section className="card">
          <h3>Recently edited</h3>
          {stats.recentlyEdited.length === 0 ? (
            <p className="muted">No articles yet.</p>
          ) : (
            <ul className="next-overview-list">
              {stats.recentlyEdited.map((a) => (
                <li key={a.articleId}>
                  <Button variant="link" onClick={() => onOpenArticle(a.articleId)}>
                    {a.title}
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <h3>Clocks</h3>
          {stats.openClocks.length === 0 ? (
            <p className="muted">No clocks in progress.</p>
          ) : (
            <ul className="next-overview-list">
              {stats.openClocks.map((c) => (
                <li key={c.clockId} className="next-overview-clock">
                  <span>{c.title}</span>
                  <span className="muted">
                    {c.filledSegments}/{c.totalSegments} · {c.campaignName}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <h3>Loose threads</h3>
          {stats.openLooseThreads.length === 0 ? (
            <p className="muted">Nothing open.</p>
          ) : (
            <ul className="next-overview-list">
              {stats.openLooseThreads.map((t) => (
                <li key={t.threadId} className="next-overview-clock">
                  <span>{t.text}</span>
                  <span className="muted">{t.campaignName}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
