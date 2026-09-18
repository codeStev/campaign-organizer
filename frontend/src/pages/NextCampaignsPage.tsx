import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  campaignsApi,
  gameSystemsApi,
  sessionsApi,
  campaignOverviewApi,
  Campaign,
  CampaignStatus,
  CAMPAIGN_STATUSES,
  GameSystem,
  Session,
  CampaignOverviewStats,
  ApiError,
} from '../api/client';
import { ClockBoard } from './ClockBoard';
import { RosterPanel } from '../components/RosterPanel';
import { notifyDataChanged } from '../lib/dataRefresh';
import { TodoListPanel } from '../components/TodoListPanel';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { SessionCalendar } from '../components/SessionCalendar';
import { CampaignCalendarExport } from '../components/CampaignCalendarExport';
import { getCampaignColor } from '../lib/campaignColor';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { toast } from 'sonner';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { PromptDialog } from '../components/PromptDialog';
import { MobileBackButton } from '../components/MobileBackButton';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

function daysUntil(dateIso: string): number {
  const target = new Date(dateIso + 'T00:00:00');
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

function countdownLabel(dateIso: string): string {
  const days = daysUntil(dateIso);
  if (days === 0) return 'today';
  if (days === 1) return 'tomorrow';
  return `in ${days} days`;
}

/**
 * Campaigns dashboard (docs/ui-overhaul-plan.md Phase 5, trimmed per
 * ADR-0105 follow-up, "what's next" widgets added for issue #67): Clocks,
 * Roster, and campaign-standing Todos live here — the things that are
 * genuinely campaign-level, not session- or arc-specific. Sessions and
 * Story Arcs have their own full-management screens
 * (NextSessionsPage/NextArcsPage); this page shows a compact, read-only
 * summary of each, plus the campaignOverviewApi-composed "what do I need to
 * prep" widgets (next session + countdown, loose threads, clocks close to
 * filling, open beats in active arcs, next session's todos).
 */
export function NextCampaignsPage({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { campaignId: urlCampaignId } = useParams<{ campaignId: string }>();
  const api = useMemo(() => campaignsApi(worldId), [worldId]);
  const [list, setList] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Campaign | null>(null);
  const [systems, setSystems] = useState<GameSystem[]>([]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [overview, setOverview] = useState<CampaignOverviewStats | null>(null);
  const [notes, setNotes] = useState('');
  const [notesDirty, setNotesDirty] = useState(false);
  const [colorDraft, setColorDraft] = useState('#888888');
  const [error, setError] = useState<string | null>(null);
  const [namePromptOpen, setNamePromptOpen] = useState(false);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(async () => {
    try {
      setList(await api.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refresh();
    gameSystemsApi.list().then(setSystems).catch(() => {});
  }, [refresh, handleError]);

  function select(campaign: Campaign) {
    setSelected(campaign);
    setNotes(campaign.notes ?? '');
    setNotesDirty(false);
    setColorDraft(campaign.color ?? '#888888');
  }

  // Compact dashboard summary only — full session/arc management lives on
  // their own screens now.
  useEffect(() => {
    if (!selected) {
      setSessions([]);
      setOverview(null);
      return;
    }
    sessionsApi(worldId, selected.id).list().then(setSessions).catch(handleError);
    campaignOverviewApi(worldId, selected.id).get().then(setOverview).catch(handleError);
  }, [worldId, selected, handleError]);

  // The URL is the source of truth for which campaign is open (ADR-0053).
  useEffect(() => {
    if (!urlCampaignId || urlCampaignId === selected?.id) return;
    api.get(urlCampaignId).then(select).catch(handleError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlCampaignId]);

  async function createCampaign(name: string) {
    try {
      const created = await api.create({ name });
      await refresh();
      notifyDataChanged(worldId);
      select(created);
      navigate(`/next/worlds/${worldId}/campaigns/${created.id}`);
      toast.success(`Campaign "${created.name}" created`);
    } catch (err) {
      handleError(err);
    }
  }

  async function saveNotes() {
    if (!selected) return;
    try {
      const updated = await api.update(selected.id, {
        name: selected.name,
        description: selected.description,
        notes,
      });
      setSelected(updated);
      setNotesDirty(false);
      await refresh();
      toast.success('GM notes saved');
    } catch (err) {
      handleError(err);
    }
  }

  async function setStatus(campaign: Campaign, status: CampaignStatus) {
    try {
      const updated = await api.update(campaign.id, {
        name: campaign.name,
        description: campaign.description,
        notes: campaign.notes,
        status,
      });
      setSelected(updated);
      await refresh();
      toast.success(`"${campaign.name}" marked ${status.toLowerCase().replace('_', ' ')}`);
    } catch (err) {
      handleError(err);
    }
  }

  async function setSystem(campaign: Campaign, systemId: string | null) {
    try {
      const updated = await api.update(campaign.id, {
        name: campaign.name,
        description: campaign.description,
        notes: campaign.notes,
        status: campaign.status,
        systemId,
      });
      setSelected(updated);
      await refresh();
      toast.success(
        systemId ? `"${campaign.name}" set to ${systemName(systemId)}` : `"${campaign.name}" system cleared`,
      );
    } catch (err) {
      handleError(err);
    }
  }

  async function setColor(campaign: Campaign, color: string) {
    try {
      const updated = await api.update(campaign.id, {
        name: campaign.name,
        description: campaign.description,
        notes: campaign.notes,
        status: campaign.status,
        systemId: campaign.systemId,
        color,
      });
      setSelected(updated);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  function systemName(systemId: string | null | undefined): string | null {
    if (!systemId) return null;
    return systems.find((s) => s.id === systemId)?.name ?? null;
  }

  function systemColor(systemId: string | null | undefined): string | null {
    if (!systemId) return null;
    return systems.find((s) => s.id === systemId)?.color ?? null;
  }

  async function removeCampaign(campaign: Campaign) {
    try {
      await api.remove(campaign.id);
      if (selected?.id === campaign.id) {
        setSelected(null);
        navigate(`/next/worlds/${worldId}/campaigns`);
      }
      await refresh();
      notifyDataChanged(worldId);
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="wiki-layout" data-has-selection={!!selected}>
      <aside className="wiki-sidebar">
        <Button className="sidebar-new-button" size="sm" onClick={() => setNamePromptOpen(true)}>
          + New campaign
        </Button>
        <PromptDialog
          open={namePromptOpen}
          onOpenChange={setNamePromptOpen}
          title="New campaign"
          label="Campaign name"
          onSubmit={(name) => void createCampaign(name)}
        />
        <ul className="article-list">
          {list.map((c) => (
            <li key={c.id}>
              <button
                className={c.id === selected?.id ? 'article-link active' : 'article-link'}
                onClick={() => navigate(`/next/worlds/${worldId}/campaigns/${c.id}`)}
              >
                <TruncatedLabel label={c.name}>{c.name}</TruncatedLabel>
              </button>
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && list.length === 0 && <li className="muted">No campaigns yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        <MobileBackButton />
        {error && <p className="error">{error}</p>}
        {!selected && <p className="muted">Select or create a campaign.</p>}
        {selected && (
          <>
            <div className="map-bar">
              <h2>
                {systemColor(selected.systemId) && (
                  <span className="system-color-dot" style={{ backgroundColor: systemColor(selected.systemId)! }} />
                )}
                {selected.name}
              </h2>
              <span className={`campaign-status campaign-${selected.status.toLowerCase().replace('_', '-')}`}>
                {selected.status.toLowerCase().replace('_', ' ')}
              </span>
              <Select
                value={selected.status}
                onValueChange={(v) => void setStatus(selected, v as CampaignStatus)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CAMPAIGN_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {s.toLowerCase().replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select
                value={selected.systemId ?? 'none'}
                onValueChange={(v) => void setSystem(selected, v === 'none' ? null : v)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Game system…" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">— none —</SelectItem>
                  {systems.map((s) => (
                    <SelectItem key={s.id} value={s.id}>
                      {s.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <input
                type="color"
                value={colorDraft}
                onChange={(e) => setColorDraft(e.target.value)}
                onBlur={() => {
                  if (colorDraft !== (selected.color ?? '#888888')) void setColor(selected, colorDraft);
                }}
                title="Campaign color (used on the session calendar)"
              />
              <CampaignCalendarExport worldId={worldId} campaignId={selected.id} />
              <Button variant="link" size="sm" onClick={() => navigate(`/next/worlds/${worldId}/chronicle`)}>
                Chronicle →
              </Button>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    Delete campaign
                  </Button>
                }
                title="Delete campaign?"
                description={`This permanently deletes "${selected.name}" — its sessions, arcs, and beats. This cannot be undone.`}
                onConfirm={() => removeCampaign(selected)}
              />
            </div>
            {selected.description && <p className="muted">{selected.description}</p>}

            <div className="campaign-workspace">
              <div className="campaign-workspace-main">
                <section className="card">
                  <h3 className="eyebrow">Next session</h3>
                  {overview?.nextSession ? (
                    <>
                      <div className="next-overview-stat">
                        <span className="next-overview-stat-value">
                          {countdownLabel(overview.nextSession.date)}
                        </span>
                        <span className="muted">
                          {overview.nextSession.sessionNumber != null
                            ? `#${overview.nextSession.sessionNumber} `
                            : ''}
                          {overview.nextSession.title}
                        </span>
                      </div>
                      {overview.nextSessionTodos.length > 0 && (
                        <ul className="next-overview-list">
                          {overview.nextSessionTodos.map((t) => (
                            <li key={t.todoId} className="muted">
                              {t.text}
                            </li>
                          ))}
                        </ul>
                      )}
                    </>
                  ) : (
                    <p className="muted">No session scheduled.</p>
                  )}
                </section>

                <section className="card">
                  <h3 className="eyebrow">Loose threads</h3>
                  {overview && overview.openLooseThreads.length > 0 ? (
                    <ul className="next-overview-list">
                      {overview.openLooseThreads.map((t) => (
                        <li key={t.threadId} className="next-overview-clock">
                          <div className="next-overview-clock-head">
                            <span>{t.text}</span>
                          </div>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="muted">Nothing open.</p>
                  )}
                </section>

                <section className="card">
                  <h3 className="eyebrow">Clocks — close to filling</h3>
                  {overview && overview.openClocksNearFilling.length > 0 ? (
                    <ul className="next-overview-list">
                      {overview.openClocksNearFilling.map((c) => (
                        <li key={c.clockId} className="next-overview-clock">
                          <div className="next-overview-clock-head">
                            <span>{c.title}</span>
                            <span className="muted">
                              {c.filledSegments}/{c.totalSegments}
                            </span>
                          </div>
                          <div className="next-overview-progress">
                            <div
                              className="next-overview-progress-fill"
                              style={{ width: `${(100 * c.filledSegments) / c.totalSegments}%` }}
                            />
                          </div>
                          {c.nextUnfilledSegmentTitle && (
                            <span className="muted">Next: {c.nextUnfilledSegmentTitle}</span>
                          )}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="muted">No clocks in progress.</p>
                  )}
                </section>

                <section className="card">
                  <div className="form-actions">
                    <h3 style={{ margin: 0 }}>Sessions</h3>
                    <span className="print-toolbar-spacer" />
                    <Button
                      variant="link"
                      size="sm"
                      onClick={() => navigate(`/next/worlds/${worldId}/sessions/${selected.id}`)}
                    >
                      Open →
                    </Button>
                  </div>
                  <ul className="next-overview-list">
                    {sessions.slice(0, 5).map((s) => (
                      <li key={s.id}>
                        <Button
                          variant="link"
                          onClick={() => navigate(`/next/worlds/${worldId}/sessions/${selected.id}/${s.id}`)}
                        >
                          {s.sessionNumber != null ? `#${s.sessionNumber} ` : ''}
                          {s.title}
                        </Button>
                      </li>
                    ))}
                    {sessions.length === 0 && <li className="muted">No sessions logged yet.</li>}
                  </ul>
                </section>

                <section className="card">
                  <h3 className="eyebrow">Calendar</h3>
                  <SessionCalendar
                    entries={sessions
                      .filter((s) => !!s.date)
                      .map((s) => ({
                        id: s.id,
                        date: s.date!,
                        title: s.title,
                        sessionNumber: s.sessionNumber,
                        campaignId: selected.id,
                        color: getCampaignColor(selected),
                      }))}
                    onSelectSession={(entry) =>
                      navigate(`/next/worlds/${worldId}/sessions/${selected.id}/${entry.id}`)
                    }
                  />
                </section>

                <section className="card">
                  <div className="form-actions">
                    <h3 style={{ margin: 0 }}>Open beats</h3>
                    <span className="print-toolbar-spacer" />
                    <Button
                      variant="link"
                      size="sm"
                      onClick={() => navigate(`/next/worlds/${worldId}/arcs/${selected.id}`)}
                    >
                      Open →
                    </Button>
                  </div>
                  <ul className="next-overview-list">
                    {(overview?.openBeatsInActiveArcs ?? []).map((b) => (
                      <li key={b.beatId}>
                        <Button
                          variant="link"
                          onClick={() => navigate(`/next/worlds/${worldId}/arcs/${selected.id}/${b.arcId}`)}
                        >
                          <span className="muted">{b.arcTitle}:</span> {b.beatTitle}
                        </Button>
                      </li>
                    ))}
                    {overview && overview.openBeatsInActiveArcs.length === 0 && (
                      <li className="muted">No open beats in active arcs.</li>
                    )}
                  </ul>
                </section>

                <ClockBoard worldId={worldId} campaignId={selected.id} onError={handleError} />
              </div>

              <div className="campaign-workspace-side">
                <RosterPanel worldId={worldId} campaignId={selected.id} onError={handleError} />

                <section className="card">
                  <TodoListPanel worldId={worldId} campaignId={selected.id} onError={handleError} />
                </section>

                <section className="card">
                  <h3>GM notes</h3>
                  <div className="gm-notes">
                    <MarkdownEditor
                      value={notes}
                      onChange={(value) => {
                        setNotes(value);
                        setNotesDirty(true);
                      }}
                    />
                  </div>
                  <div className="editor-actions">
                    <Button onClick={saveNotes} disabled={!notesDirty}>
                      Save notes
                    </Button>
                  </div>
                </section>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
