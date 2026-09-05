import { useEffect, useMemo, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { MoreHorizontal } from 'lucide-react';
import { campaignsApi, sessionsApi, arcsApi, Campaign, Session, Arc } from '../api/client';
import { TruncatedLabel } from './TruncatedLabel';
import { PromptDialog } from './PromptDialog';
import { ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger } from './ui/context-menu';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from './ui/dropdown-menu';

interface Props {
  worldId: string;
}

interface CampaignChildren {
  sessions: Session[];
  arcs: Arc[];
}

type RenameTarget =
  | { kind: 'campaign'; campaign: Campaign }
  | { kind: 'session'; campaignId: string; session: Session }
  | { kind: 'arc'; campaignId: string; arc: Arc };

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
 *
 * The "Sessions"/"Story Arcs" kind rows are fold-only, not links — they
 * used to route to that kind's list-only page just to get to a "+ New X"
 * button. New items are created straight from the tree instead, via a "+"
 * button and a matching right-click menu (same affordance as CategoryTree's
 * category rows), so opening the fold and creating a new item are separate
 * actions rather than one row trying to do both.
 */
// Tap-accessible equivalent of a row's right-click "Rename" item — touch
// devices have no right-click.
function RowActionsMenu({ onRename }: { onRename: () => void }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="category-tree-action"
          title="Actions"
          onClick={(e) => e.stopPropagation()}
        >
          <MoreHorizontal size={14} />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent onClick={(e) => e.stopPropagation()}>
        <DropdownMenuItem onSelect={onRename}>Rename</DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export function CampaignNavTree({ worldId }: Props) {
  const location = useLocation();
  const navigate = useNavigate();
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [expandedCampaigns, setExpandedCampaigns] = useState<Set<string>>(new Set());
  const [expandedKinds, setExpandedKinds] = useState<Set<string>>(new Set());
  const [children, setChildren] = useState<Map<string, CampaignChildren>>(new Map());
  const [newArcCampaignId, setNewArcCampaignId] = useState<string | null>(null);
  const [renameTarget, setRenameTarget] = useState<RenameTarget | null>(null);

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

  async function loadChildren(campaignId: string, force = false) {
    if (!force && children.has(campaignId)) return;
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

  // Self-heal: if the open session/arc isn't in the cached branch (created
  // or deleted from its own page rather than the tree), refetch that
  // campaign's children so the tree catches up.
  useEffect(() => {
    if (!match.campaignId || !match.entityId) return;
    if (match.section !== 'sessions' && match.section !== 'arcs') return;
    const kids = children.get(match.campaignId);
    if (!kids) return;
    const list = match.section === 'sessions' ? kids.sessions : kids.arcs;
    if (!list.some((item) => item.id === match.entityId)) {
      void loadChildren(match.campaignId, true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [match.campaignId, match.section, match.entityId, children]);

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

  function newSession(campaignId: string) {
    navigate(`sessions/${campaignId}/new`);
  }

  async function createArc(campaignId: string, title: string) {
    try {
      const created = await arcsApi(worldId, campaignId).create({ title });
      setChildren((prev) => {
        const next = new Map(prev);
        const existing = next.get(campaignId) ?? { sessions: [], arcs: [] };
        next.set(campaignId, { ...existing, arcs: [...existing.arcs, created] });
        return next;
      });
      setExpandedKinds((prev) => new Set(prev).add(`${campaignId}:arcs`));
      navigate(`arcs/${campaignId}/${created.id}`);
    } catch {
      // Best-effort — the dialog just closes on failure.
    }
  }

  // Campaign's list response already carries every field its update needs,
  // so this updates directly without a full-fetch-first step (mirrors
  // CategoryTree's rename pattern for Atlas/Handouts/Tables & Decks).
  async function renameCampaign(campaign: Campaign, newName: string) {
    try {
      await campaignsApi(worldId).update(campaign.id, {
        name: newName,
        description: campaign.description ?? null,
        notes: campaign.notes ?? null,
        status: campaign.status,
        systemId: campaign.systemId ?? null,
      });
      setCampaigns((prev) => prev.map((c) => (c.id === campaign.id ? { ...c, name: newName } : c)));
    } catch {
      // Best-effort — the dialog just closes on failure.
    }
  }

  async function renameSession(campaignId: string, session: Session, newTitle: string) {
    try {
      await sessionsApi(worldId, campaignId).update(session.id, {
        title: newTitle,
        sessionNumber: session.sessionNumber ?? null,
        date: session.date ?? null,
        summary: session.summary ?? null,
        notes: session.notes ?? null,
      });
      setChildren((prev) => {
        const existing = prev.get(campaignId);
        if (!existing) return prev;
        const next = new Map(prev);
        next.set(campaignId, {
          ...existing,
          sessions: existing.sessions.map((s) => (s.id === session.id ? { ...s, title: newTitle } : s)),
        });
        return next;
      });
    } catch {
      // Best-effort — the dialog just closes on failure.
    }
  }

  async function renameArc(campaignId: string, arc: Arc, newTitle: string) {
    try {
      await arcsApi(worldId, campaignId).update(arc.id, {
        title: newTitle,
        description: arc.description ?? null,
        status: arc.status,
        position: arc.position,
      });
      setChildren((prev) => {
        const existing = prev.get(campaignId);
        if (!existing) return prev;
        const next = new Map(prev);
        next.set(campaignId, {
          ...existing,
          arcs: existing.arcs.map((a) => (a.id === arc.id ? { ...a, title: newTitle } : a)),
        });
        return next;
      });
    } catch {
      // Best-effort — the dialog just closes on failure.
    }
  }

  return (
    <ul className="category-tree campaign-nav-tree">
      {campaigns.map((c) => {
        const campaignExpanded = expandedCampaigns.has(c.id);
        const kids = children.get(c.id);
        const sessionsKey = `${c.id}:sessions`;
        const arcsKey = `${c.id}:arcs`;
        const sessionsExpanded = expandedKinds.has(sessionsKey);
        const arcsExpanded = expandedKinds.has(arcsKey);
        return (
          <li key={c.id}>
            <ContextMenu>
              <ContextMenuTrigger asChild>
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
                      isActive && match.section === 'campaigns'
                        ? 'category-tree-article active'
                        : 'category-tree-article'
                    }
                  >
                    <TruncatedLabel label={c.name}>{c.name}</TruncatedLabel>
                  </NavLink>
                  <RowActionsMenu onRename={() => setRenameTarget({ kind: 'campaign', campaign: c })} />
                </div>
              </ContextMenuTrigger>
              <ContextMenuContent>
                <ContextMenuItem onSelect={() => setRenameTarget({ kind: 'campaign', campaign: c })}>
                  Rename campaign
                </ContextMenuItem>
              </ContextMenuContent>
            </ContextMenu>
            {campaignExpanded && (
              <ul className="article-list-nested">
                <li>
                  <ContextMenu>
                    <ContextMenuTrigger asChild>
                      <div className="category-tree-row">
                        <button
                          type="button"
                          className="article-tree-toggle"
                          onClick={() => toggleKind(sessionsKey)}
                          title={sessionsExpanded ? 'Collapse' : 'Expand'}
                        >
                          {sessionsExpanded ? '▾' : '▸'}
                        </button>
                        <button
                          type="button"
                          className={
                            match.campaignId === c.id && match.section === 'sessions' && !match.entityId
                              ? 'category-tree-article active'
                              : 'category-tree-article'
                          }
                          onClick={() => toggleKind(sessionsKey)}
                        >
                          Sessions
                        </button>
                        <button
                          type="button"
                          className="category-tree-action"
                          title="New session"
                          onClick={(e) => {
                            e.stopPropagation();
                            newSession(c.id);
                          }}
                        >
                          +
                        </button>
                      </div>
                    </ContextMenuTrigger>
                    <ContextMenuContent>
                      <ContextMenuItem onSelect={() => newSession(c.id)}>+ New session</ContextMenuItem>
                    </ContextMenuContent>
                  </ContextMenu>
                  {sessionsExpanded && (
                    <ul className="article-list-nested">
                      {(kids?.sessions ?? []).map((s) => (
                        <li key={s.id}>
                          <div className="category-tree-row">
                            <ContextMenu>
                              <ContextMenuTrigger asChild>
                                <NavLink
                                  to={`sessions/${c.id}/${s.id}`}
                                  className={({ isActive }) =>
                                    isActive ? 'category-tree-article active' : 'category-tree-article'
                                  }
                                >
                                  <TruncatedLabel label={s.title}>
                                    {s.sessionNumber != null && (
                                      <span className="session-num">#{s.sessionNumber} </span>
                                    )}
                                    {s.title}
                                  </TruncatedLabel>
                                </NavLink>
                              </ContextMenuTrigger>
                              <ContextMenuContent>
                                <ContextMenuItem
                                  onSelect={() => setRenameTarget({ kind: 'session', campaignId: c.id, session: s })}
                                >
                                  Rename
                                </ContextMenuItem>
                              </ContextMenuContent>
                            </ContextMenu>
                            <RowActionsMenu
                              onRename={() => setRenameTarget({ kind: 'session', campaignId: c.id, session: s })}
                            />
                          </div>
                        </li>
                      ))}
                      {kids && kids.sessions.length === 0 && <li className="muted">No sessions yet.</li>}
                    </ul>
                  )}
                </li>
                <li>
                  <ContextMenu>
                    <ContextMenuTrigger asChild>
                      <div className="category-tree-row">
                        <button
                          type="button"
                          className="article-tree-toggle"
                          onClick={() => toggleKind(arcsKey)}
                          title={arcsExpanded ? 'Collapse' : 'Expand'}
                        >
                          {arcsExpanded ? '▾' : '▸'}
                        </button>
                        <button
                          type="button"
                          className={
                            match.campaignId === c.id && match.section === 'arcs' && !match.entityId
                              ? 'category-tree-article active'
                              : 'category-tree-article'
                          }
                          onClick={() => toggleKind(arcsKey)}
                        >
                          Story Arcs
                        </button>
                        <button
                          type="button"
                          className="category-tree-action"
                          title="New arc"
                          onClick={(e) => {
                            e.stopPropagation();
                            setNewArcCampaignId(c.id);
                          }}
                        >
                          +
                        </button>
                      </div>
                    </ContextMenuTrigger>
                    <ContextMenuContent>
                      <ContextMenuItem onSelect={() => setNewArcCampaignId(c.id)}>+ New arc</ContextMenuItem>
                    </ContextMenuContent>
                  </ContextMenu>
                  {arcsExpanded && (
                    <ul className="article-list-nested">
                      {(kids?.arcs ?? []).map((a) => (
                        <li key={a.id}>
                          <div className="category-tree-row">
                            <ContextMenu>
                              <ContextMenuTrigger asChild>
                                <NavLink
                                  to={`arcs/${c.id}/${a.id}`}
                                  className={({ isActive }) =>
                                    isActive ? 'category-tree-article active' : 'category-tree-article'
                                  }
                                >
                                  <TruncatedLabel label={a.title}>
                                    <span className={`arc-status arc-${a.status.toLowerCase()}`}>
                                      {a.status.toLowerCase()}
                                    </span>{' '}
                                    {a.title}
                                  </TruncatedLabel>
                                </NavLink>
                              </ContextMenuTrigger>
                              <ContextMenuContent>
                                <ContextMenuItem
                                  onSelect={() => setRenameTarget({ kind: 'arc', campaignId: c.id, arc: a })}
                                >
                                  Rename
                                </ContextMenuItem>
                              </ContextMenuContent>
                            </ContextMenu>
                            <RowActionsMenu
                              onRename={() => setRenameTarget({ kind: 'arc', campaignId: c.id, arc: a })}
                            />
                          </div>
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
      <PromptDialog
        open={newArcCampaignId !== null}
        onOpenChange={(open) => {
          if (!open) setNewArcCampaignId(null);
        }}
        title="New story arc"
        label="Arc title"
        onSubmit={(title) => {
          const campaignId = newArcCampaignId;
          setNewArcCampaignId(null);
          if (campaignId) void createArc(campaignId, title);
        }}
      />
      <PromptDialog
        open={renameTarget !== null}
        onOpenChange={(open) => {
          if (!open) setRenameTarget(null);
        }}
        title={
          renameTarget?.kind === 'campaign'
            ? 'Rename campaign'
            : renameTarget?.kind === 'session'
              ? 'Rename session'
              : 'Rename arc'
        }
        label="Name"
        defaultValue={
          renameTarget?.kind === 'campaign'
            ? renameTarget.campaign.name
            : renameTarget?.kind === 'session'
              ? renameTarget.session.title
              : renameTarget?.kind === 'arc'
                ? renameTarget.arc.title
                : ''
        }
        onSubmit={(name) => {
          const target = renameTarget;
          setRenameTarget(null);
          if (!target) return;
          if (target.kind === 'campaign') void renameCampaign(target.campaign, name);
          else if (target.kind === 'session') void renameSession(target.campaignId, target.session, name);
          else void renameArc(target.campaignId, target.arc, name);
        }}
      />
    </ul>
  );
}
