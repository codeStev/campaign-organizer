import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { globalOverviewApi, worldsApi, GlobalOverviewStats, ApiError } from '../api/client';
import { Spinner } from '../components/ui/spinner';
import { Button } from '../components/ui/button';
import { getCampaignColor } from '../lib/campaignColor';

interface Props {
  onAuthExpired: () => void;
}

function formatDate(iso: string): string {
  return new Date(iso + 'T00:00:00').toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  });
}

/**
 * The account-wide landing page (issue #68): every upcoming session across
 * every world/campaign the account owns, soonest-first, plus campaigns
 * needing attention (issue #64). One composed read from globalOverviewApi,
 * same "no widget round-trips" shape as NextOverviewPage/NextCampaignsPage's
 * overview widgets.
 */
export function NextHomePage({ onAuthExpired }: Props) {
  const navigate = useNavigate();
  const [stats, setStats] = useState<GlobalOverviewStats | null>(null);
  const [hasAnyWorlds, setHasAnyWorlds] = useState(true);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([globalOverviewApi().get(), worldsApi.list()])
      .then(([overview, worlds]) => {
        setStats(overview);
        setHasAnyWorlds(worlds.length > 0);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) onAuthExpired();
      })
      .finally(() => setLoading(false));
  }, [onAuthExpired]);

  if (loading) {
    return (
      <p className="muted loading-row">
        <Spinner /> Loading…
      </p>
    );
  }

  if (!hasAnyWorlds) {
    return (
      <div className="card">
        <h3>Welcome!</h3>
        <p className="muted">You don't have any worlds yet — create one to get started.</p>
        <Button onClick={() => navigate('/next/worlds')}>Create a world</Button>
      </div>
    );
  }

  if (!stats) {
    return <p className="muted">Couldn't load the overview.</p>;
  }

  return (
    <div className="next-overview-grid">
      <section className="card">
        <h3 className="eyebrow">Upcoming sessions</h3>
        {stats.upcomingSessions.length === 0 ? (
          <p className="muted">Nothing scheduled yet.</p>
        ) : (
          <ul className="next-overview-list">
            {stats.upcomingSessions.map((s) => (
              <li key={s.sessionId} className="next-overview-clock">
                <button
                  className="article-link"
                  onClick={() => navigate(`/next/worlds/${s.worldId}/sessions/${s.campaignId}/${s.sessionId}`)}
                >
                  <span
                    className="system-color-dot"
                    style={{ backgroundColor: getCampaignColor({ id: s.campaignId, color: s.campaignColor }) }}
                  />
                  <strong>{formatDate(s.date)}</strong>{' '}
                  <span className="muted">
                    {s.worldName} › {s.campaignName} — {s.title}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h3 className="eyebrow">Needs attention</h3>
        {stats.campaignsNeedingAttention.length === 0 ? (
          <p className="muted">Nothing needs attention right now.</p>
        ) : (
          <ul className="next-overview-list">
            {stats.campaignsNeedingAttention.map((c) => (
              <li key={c.campaignId} className="next-overview-clock">
                <button
                  className="article-link"
                  onClick={() => navigate(`/next/worlds/${c.worldId}/campaigns/${c.campaignId}`)}
                >
                  <span className="muted">
                    {c.worldName} › {c.campaignName}
                  </span>{' '}
                  <span className={`campaign-status campaign-${c.status.toLowerCase().replace('_', '-')}`}>
                    {c.status.toLowerCase().replace('_', ' ')}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
