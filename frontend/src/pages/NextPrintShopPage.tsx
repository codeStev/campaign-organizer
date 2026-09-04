import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { campaignsApi, Campaign, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { NextPrintView } from './NextPrintView';

interface Props {
  worldId: string;
  worldName: string;
  onAuthExpired: () => void;
}

/**
 * Print Shop (docs/ui-overhaul-plan.md Phase 2) — one place to find every
 * print output, matching the reviewed mockup's intent. Only "Full
 * compendium" is a genuine aggregator here (it opens NextPrintView,
 * ADR-0106's fork of PrintView, as an overlay — no new print logic); the
 * other three outputs are scoped to screens with their own print flow
 * (Sessions, Sheets, Handouts), so they link to that screen rather than a
 * live inline preview here — that's a bigger, separate composition effort,
 * not a rebuild-as-is.
 */
export function NextPrintShopPage({ worldId, worldName, onAuthExpired }: Props) {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [printOpen, setPrintOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    campaignsApi(worldId)
      .list()
      .then(setCampaigns)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) return onAuthExpired();
        setError(err instanceof Error ? err.message : 'Something went wrong');
      });
  }, [worldId, onAuthExpired]);

  function handlePrintError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) return onAuthExpired();
    setError(err instanceof Error ? err.message : 'Something went wrong');
  }

  return (
    <div className="card">
      <h1>Print Shop</h1>
      <p className="muted">Built for paper — every output opens in its own paper view, never the app chrome.</p>
      {error && <p className="error">{error}</p>}
      {printOpen && (
        <NextPrintView
          worldId={worldId}
          worldName={worldName}
          campaigns={campaigns}
          onClose={() => setPrintOpen(false)}
          onError={handlePrintError}
        />
      )}
      <ul className="article-list">
        <li className="rel-row">
          <Button variant="link" className="template-open" onClick={() => setPrintOpen(true)}>
            <strong>Full compendium</strong>{' '}
            <small className="muted">whole world or a campaign, article scope picker</small>
          </Button>
        </li>
        <li className="rel-row">
          <Button variant="link" className="template-open" asChild>
            <NavLink to={`/next/worlds/${worldId}/sessions`}>
              <strong>Session prep packet</strong> <small className="muted">from a campaign's session</small>
            </NavLink>
          </Button>
        </li>
        <li className="rel-row">
          <Button variant="link" className="template-open" asChild>
            <NavLink to={`/next/worlds/${worldId}/sheets/statblocks`}>
              <strong>Statblock cards</strong> <small className="muted">Sheets</small>
            </NavLink>
          </Button>
        </li>
        <li className="rel-row">
          <Button variant="link" className="template-open" asChild>
            <NavLink to={`/next/worlds/${worldId}/handouts`}>
              <strong>Player handouts</strong> <small className="muted">Handouts</small>
            </NavLink>
          </Button>
        </li>
      </ul>
    </div>
  );
}
