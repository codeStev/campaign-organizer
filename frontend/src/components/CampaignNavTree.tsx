import { useEffect, useMemo, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { campaignsApi, sessionsApi, arcsApi, Campaign, Session, Arc } from '../api/client';
import { TruncatedLabel } from './TruncatedLabel';

interface Props {
  worldId: string;
}

interface CampaignChildren {
  sessions: Session[];
  arcs: Arc[];
}

/**
 * Nested campaign navigation for the /next sidebar (ADR-0105 follow-up):
 * Campaigns > [campaign] > Sessions/Story Arcs/Encounters > individual
 * entries, all in one persistent tree — instead of Sessions/Story Arcs/
 * Encounters as separate top-level nav entries that each made you re-pick
 * a campaign on landing. Reuses the same expand/collapse chevron classes
 * as the old Wiki tree (.article-tree-toggle/.article-list-nested) for
 * visual consistency, hand-rolled rather than shadcn's SidebarMenuSub
 * since that's only designed for one level, and this needs three
 * (campaign → kind → entity).
 */
export function CampaignNavTree({ worldId }: Props) {
  const location = useLocation();
  const navigate = useNavigate();
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [expandedCampaigns, setExpandedCampaigns] = useState<Set<string>>(new Set());
  const [expandedKinds, setExpandedKinds] = useState<Set<string>>(new Set());
  const [children, setChildren] = useState<Map<string, CampaignChildren>>(new Map());

  useEffect(() => {
    campaignsApi(worldId).list().then(setCampaigns).catch(() => {});
  }, [worldId]);

  // Which campaign/section/entity the current route is on, so a deep link
  // (reload, direct URL) lands with the right branches already expanded.
  const match = useMemo(() => {
    const m = location.pathname.match(
      /\/next\/worlds\/[^/]+\/(campaigns|sessions|arcs|encounters)(?:\/([^/]+))?(?:\/([^/]+))?/,
    );
    return {
      section: m?.[1] as 'campaigns' | 'sessions' | 'arcs' | 'encounters' | undefined,
      campaignId: m?.[2],
      entityId: m?.[3],
    };
  }, [location.pathname]);

  async function loadChildren(campaignId: string) {
    if (children.has(campaignId)) return;
    try {
      const [sessions, arcs] = await Promise.all([
        sessionsApi(worldId, campaignId).list(),
        arcsApi(worldId, campaignId).list(),
      ]);
      setChildren((prev) => new Map(prev).set(campaignId, { sessions, arcs }));
    } catch {
      // Best-effort — an empty branch just shows nothing under it.
    }
  }

  // Auto-expand to the active campaign/section on deep link.
  useEffect(() => {
    if (!match.campaignId) return;
    setExpandedCampaigns((prev) => (prev.has(match.campaignId!) ? prev : new Set(prev).add(match.campaignId!)));
    void loadChildren(match.campaignId);
    if (match.section === 'sessions' || match.section === 'arcs') {
      const key = `${match.campaignId}:${match.section}`;
      setExpandedKinds((prev) => (prev.has(key) ? prev : new Set(prev).add(key)));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [match.campaignId, match.section]);

  function toggleCampaign(id: string) {
    setExpandedCampaigns((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
    void loadChildren(id);
  }

  function toggleKind(key: string) {
    setExpandedKinds((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  return (
    <ul className="category-tree campaign-nav-tree">
      {campaigns.map((c) => {
        const campaignExpanded = expandedCampaigns.has(c.id);
        const kids = children.get(c.id);
        const sessionsKey = `${c.id}:sessions`;
        const arcsKey = `${c.id}:arcs`;
        return (
          <li key={c.id}>
            <div className="category-tree-row">
              <button
                type="button"
                className="article-tree-toggle"
                onClick={() => toggleCampaign(c.id)}
                title={campaignExpanded ? 'Collapse' : 'Expand'}
              >
                {campaignExpanded ? '▾' : '▸'}
              </button>
              <NavLink
                to={`campaigns/${c.id}`}
                className={({ isActive }) =>
                  isActive && match.section === 'campaigns' ? 'category-tree-article active' : 'category-tree-article'
                }
              >
                <TruncatedLabel label={c.name}>{c.name}</TruncatedLabel>
              </NavLink>
            </div>
            {campaignExpanded && (
              <ul className="article-list-nested">
                <li>
                  <div className="category-tree-row">
                    <button
                      type="button"
                      className="article-tree-toggle"
                      onClick={() => toggleKind(sessionsKey)}
                      title={expandedKinds.has(sessionsKey) ? 'Collapse' : 'Expand'}
                    >
                      {expandedKinds.has(sessionsKey) ? '▾' : '▸'}
                    </button>
                    <NavLink
                      to={`sessions/${c.id}`}
                      className={({ isActive }) => (isActive ? 'category-tree-article active' : 'category-tree-article')}
                    >
                      Sessions
                    </NavLink>
                  </div>
                  {expandedKinds.has(sessionsKey) && (
                    <ul className="article-list-nested">
                      {(kids?.sessions ?? []).map((s) => (
                        <li key={s.id}>
                          <NavLink
                            to={`sessions/${c.id}/${s.id}`}
                            className={({ isActive }) =>
                              isActive ? 'category-tree-article active' : 'category-tree-article'
                            }
                          >
                            <TruncatedLabel label={s.title}>
                              {s.sessionNumber != null && <span className="session-num">#{s.sessionNumber} </span>}
                              {s.title}
                            </TruncatedLabel>
                          </NavLink>
                        </li>
                      ))}
                      {kids && kids.sessions.length === 0 && <li className="muted">No sessions yet.</li>}
                    </ul>
                  )}
                </li>
                <li>
                  <div className="category-tree-row">
                    <button
                      type="button"
                      className="article-tree-toggle"
                      onClick={() => toggleKind(arcsKey)}
                      title={expandedKinds.has(arcsKey) ? 'Collapse' : 'Expand'}
                    >
                      {expandedKinds.has(arcsKey) ? '▾' : '▸'}
                    </button>
                    <NavLink
                      to={`arcs/${c.id}`}
                      className={({ isActive }) => (isActive ? 'category-tree-article active' : 'category-tree-article')}
                    >
                      Story Arcs
                    </NavLink>
                  </div>
                  {expandedKinds.has(arcsKey) && (
                    <ul className="article-list-nested">
                      {(kids?.arcs ?? []).map((a) => (
                        <li key={a.id}>
                          <NavLink
                            to={`arcs/${c.id}/${a.id}`}
                            className={({ isActive }) =>
                              isActive ? 'category-tree-article active' : 'category-tree-article'
                            }
                          >
                            <TruncatedLabel label={a.title}>
                              <span className={`arc-status arc-${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>{' '}
                              {a.title}
                            </TruncatedLabel>
                          </NavLink>
                        </li>
                      ))}
                      {kids && kids.arcs.length === 0 && <li className="muted">No arcs yet.</li>}
                    </ul>
                  )}
                </li>
                <li>
                  <div className="category-tree-row">
                    <span className="article-tree-toggle-spacer" />
                    <NavLink
                      to={`encounters/${c.id}`}
                      className={({ isActive }) => (isActive ? 'category-tree-article active' : 'category-tree-article')}
                    >
                      Encounters
                    </NavLink>
                  </div>
                </li>
              </ul>
            )}
          </li>
        );
      })}
      {campaigns.length === 0 && (
        <li className="muted">
          <button type="button" className="category-tree-article" onClick={() => navigate('campaigns')}>
            No campaigns yet
          </button>
        </li>
      )}
    </ul>
  );
}
