import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  aiApi,
  arcsApi,
  campaignsApi,
  sessionsApi,
  Beat,
  Campaign,
  Session,
  ApiError,
} from '../api/client';
import { fetchCampaignBeats } from '../lib/beats';
import { SessionPacketView } from './SessionPacketView';
import { RecapView } from './RecapView';
import { CheatSheetView } from './CheatSheetView';
import { LooseThreadsPanel } from '../components/LooseThreadsPanel';
import { AttendancePanel } from '../components/AttendancePanel';
import { TodoListPanel } from '../components/TodoListPanel';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { renderMarkdown } from '../lib/markdown';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { toast } from 'sonner';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

interface Draft {
  id: string | null;
  title: string;
  sessionNumber: string;
  date: string;
  summary: string;
  notes: string;
}

const EMPTY_DRAFT: Draft = { id: null, title: '', sessionNumber: '', date: '', summary: '', notes: '' };

/**
 * Sessions get their own screen (ADR-0105 follow-up): a campaign picker,
 * that campaign's sessions in a plain list (no custom categories — sessions
 * are already campaign-scoped, confirmed with the user), and one session's
 * full detail — matching the mockup's isCampaign view — instead of
 * SessionLog's old pattern of listing every session inline on the Campaigns
 * workspace. Reuses every leaf widget SessionLog did (AttendancePanel,
 * LooseThreadsPanel, TodoListPanel, SessionPacketView, RecapView,
 * CheatSheetView) unchanged.
 */
export function NextSessionsPage({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { campaignId: urlCampaignId, sessionId: urlSessionId } = useParams<{
    campaignId?: string;
    sessionId?: string;
  }>();
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [campaignBeats, setCampaignBeats] = useState<Beat[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  const [error, setError] = useState<string | null>(null);
  const [summarizing, setSummarizing] = useState(false);
  const [summaryText, setSummaryText] = useState<string | null>(null);
  const [summaryError, setSummaryError] = useState<string | null>(null);
  const [packetOpen, setPacketOpen] = useState(false);
  const [recapOpen, setRecapOpen] = useState(false);
  const [cheatOpen, setCheatOpen] = useState(false);

  const campaign = campaigns.find((c) => c.id === urlCampaignId) ?? null;
  const api = useMemo(
    () => (urlCampaignId ? sessionsApi(worldId, urlCampaignId) : null),
    [worldId, urlCampaignId],
  );

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  useEffect(() => {
    campaignsApi(worldId).list().then(setCampaigns).catch(handleError);
  }, [worldId, handleError]);

  const refreshSessions = useCallback(async () => {
    if (!api) {
      setSessions([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      setSessions(await api.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refreshSessions();
  }, [refreshSessions]);

  // Every beat across every arc in the campaign, so a session's "staged
  // beats" (beats tagged to it, regardless of which arc they belong to) can
  // be shown — mirrors SessionLog's own campaignBeats fetch.
  useEffect(() => {
    if (!urlCampaignId) {
      setCampaignBeats([]);
      return;
    }
    let active = true;
    arcsApi(worldId, urlCampaignId)
      .list()
      .then((arcs) => fetchCampaignBeats(worldId, urlCampaignId, arcs))
      .then((beats) => active && setCampaignBeats(beats))
      .catch(handleError);
    return () => {
      active = false;
    };
  }, [worldId, urlCampaignId, handleError]);

  function resetSummary() {
    setSummarizing(false);
    setSummaryText(null);
    setSummaryError(null);
  }

  const openSession = urlSessionId && urlSessionId !== 'new' ? sessions.find((s) => s.id === urlSessionId) ?? null : null;
  const openBeats = urlSessionId
    ? campaignBeats.filter((b) => b.sessionId === urlSessionId).sort((a, b) => a.position - b.position)
    : [];

  // The URL is the source of truth for which session is open (ADR-0053).
  useEffect(() => {
    resetSummary();
    setPacketOpen(false);
    setRecapOpen(false);
    setCheatOpen(false);
    if (!urlSessionId) {
      setDraft(null);
      return;
    }
    if (urlSessionId === 'new') {
      setDraft({ ...EMPTY_DRAFT });
      setMode('edit');
      return;
    }
    const found = sessions.find((s) => s.id === urlSessionId);
    if (found) {
      setDraft({
        id: found.id,
        title: found.title,
        sessionNumber: found.sessionNumber != null ? String(found.sessionNumber) : '',
        date: found.date ?? '',
        summary: found.summary ?? '',
        notes: found.notes ?? '',
      });
      setMode('read');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlSessionId, sessions]);

  function startEdit() {
    if (!openSession) return;
    setDraft({
      id: openSession.id,
      title: openSession.title,
      sessionNumber: openSession.sessionNumber != null ? String(openSession.sessionNumber) : '',
      date: openSession.date ?? '',
      summary: openSession.summary ?? '',
      notes: openSession.notes ?? '',
    });
    setMode('edit');
  }

  async function save() {
    if (!draft || !api || !urlCampaignId) return;
    const body = {
      title: draft.title,
      sessionNumber: draft.sessionNumber ? Number(draft.sessionNumber) : null,
      date: draft.date || null,
      summary: draft.summary || null,
      notes: draft.notes || null,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      await refreshSessions();
      setMode('read');
      navigate(`/next/worlds/${worldId}/sessions/${urlCampaignId}/${saved.id}`);
      toast.success(`Session "${body.title}" saved`);
    } catch (err) {
      handleError(err);
    }
  }

  async function remove() {
    if (!draft?.id || !api || !urlCampaignId) return;
    try {
      await api.remove(draft.id);
      await refreshSessions();
      navigate(`/next/worlds/${worldId}/sessions/${urlCampaignId}`);
    } catch (err) {
      handleError(err);
    }
  }

  async function summarizeNotes() {
    if (!openSession?.notes) return;
    setSummarizing(true);
    setSummaryError(null);
    try {
      const result = await aiApi(worldId).summarizeSessionNotes(openSession.notes);
      setSummaryText(result.text);
    } catch (err) {
      setSummaryError(err instanceof Error ? err.message : 'Summary failed');
    } finally {
      setSummarizing(false);
    }
  }

  return (
    <div className="session-workspace-layout">
      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        {!urlCampaignId && <p className="muted">Select a campaign from the sidebar to see its sessions.</p>}
        {urlCampaignId && (
          <div className="editor-actions">
            <Button
              className="sidebar-new-button"
              size="sm"
              onClick={() => navigate(`/next/worlds/${worldId}/sessions/${urlCampaignId}/new`)}
            >
              + New session
            </Button>
          </div>
        )}
        {loading && (
          <p className="muted loading-row">
            <Spinner /> Loading…
          </p>
        )}
        {urlCampaignId && !loading && !draft && (
          <p className="muted">Select a session from the sidebar, or create a new one.</p>
        )}

        {draft && mode === 'edit' && (
          <div className="card">
            <strong>{draft.id ? 'Edit session' : 'New session'}</strong>
            <div className="session-form-row">
              <Input
                type="number"
                className="num-input"
                placeholder="#"
                value={draft.sessionNumber}
                onChange={(e) => setDraft({ ...draft, sessionNumber: e.target.value })}
              />
              <Input type="date" value={draft.date} onChange={(e) => setDraft({ ...draft, date: e.target.value })} />
              <Input
                placeholder="Session title"
                value={draft.title}
                onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                required
              />
            </div>
            <MarkdownEditor value={draft.summary} onChange={(summary) => setDraft({ ...draft, summary })} />
            <label className="sheet-article">
              <span className="muted">GM notes (private)</span>
              <MarkdownEditor value={draft.notes} onChange={(notes) => setDraft({ ...draft, notes })} />
            </label>
            <div className="editor-actions">
              <Button type="button" onClick={save} disabled={!draft.title}>
                {draft.id ? 'Save session' : 'Add session'}
              </Button>
              {draft.id && (
                <Button type="button" variant="link" onClick={() => setMode('read')}>
                  Cancel
                </Button>
              )}
              {draft.id && (
                <ConfirmDeleteDialog
                  trigger={
                    <Button type="button" variant="link" className="text-destructive hover:text-destructive">
                      Delete
                    </Button>
                  }
                  title="Delete session?"
                  description={`This permanently deletes "${draft.title}" and cannot be undone.`}
                  onConfirm={remove}
                />
              )}
            </div>
          </div>
        )}

        {draft && mode === 'read' && openSession && (
          <article className="card article-read">
            <div className="article-read-head">
              <h2>
                {openSession.sessionNumber != null ? `#${openSession.sessionNumber} ` : ''}
                {openSession.title}
                {openSession.date && <span className="print-kicker"> — {openSession.date}</span>}
              </h2>
              <div className="editor-actions">
                <Button type="button" onClick={startEdit}>
                  Edit
                </Button>
                <Button variant="link" onClick={() => setCheatOpen(true)} title="Condensed one-page GM cheat sheet">
                  📋 Cheat sheet
                </Button>
                <Button variant="link" onClick={() => setPacketOpen(true)} title="Print a one-page prep packet">
                  🖨 Packet
                </Button>
                <Button variant="link" onClick={() => setRecapOpen(true)} title="Print the story so far">
                  🖨 Recap
                </Button>
              </div>
            </div>

            {openSession.summary ? (
              <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(openSession.summary) }} />
            ) : (
              <p className="muted">(no summary)</p>
            )}

            <strong className="muted">Staged beats</strong>
            {openBeats.length === 0 && <p className="muted">No beats tagged to this session yet.</p>}
            {openBeats.length > 0 && (
              <ul>
                {openBeats.map((b) => (
                  <li key={b.id}>
                    <strong className={b.done ? 'beat-done' : ''}>{b.title}</strong>
                    {b.body && <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(b.body) }} />}
                  </li>
                ))}
              </ul>
            )}

            <TodoListPanel worldId={worldId} campaignId={urlCampaignId!} sessionId={openSession.id} onError={handleError} />

            <strong className="muted">GM notes (private)</strong>
            {openSession.notes ? (
              <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(openSession.notes) }} />
            ) : (
              <p className="muted">(no notes)</p>
            )}
            {openSession.notes && (
              <div className="editor-actions">
                <Button type="button" variant="outline" size="sm" disabled={summarizing} onClick={() => void summarizeNotes()}>
                  {summarizing ? '✨ Summarizing…' : '✨ Summarize notes'}
                </Button>
              </div>
            )}
            {summaryError && <p className="error">{summaryError}</p>}
            {summaryText && (
              <div className="card">
                <strong className="muted">AI summary</strong>
                <p>{summaryText}</p>
              </div>
            )}
          </article>
        )}
      </div>

      <aside className="wiki-sidebar session-workspace-side">
        {openSession && urlCampaignId && (
          <>
            <AttendancePanel worldId={worldId} campaignId={urlCampaignId} sessionId={openSession.id} onError={handleError} />
            <LooseThreadsPanel worldId={worldId} campaignId={urlCampaignId} sessionId={openSession.id} onError={handleError} />
          </>
        )}
      </aside>

      {cheatOpen && openSession && urlCampaignId && (
        <CheatSheetView
          worldId={worldId}
          campaignId={urlCampaignId}
          sessionId={openSession.id}
          sessionTitle={openSession.sessionNumber != null ? `Session ${openSession.sessionNumber}: ${openSession.title}` : openSession.title}
          onClose={() => setCheatOpen(false)}
          onError={handleError}
        />
      )}

      {packetOpen && openSession && urlCampaignId && (
        <SessionPacketView
          worldId={worldId}
          campaignId={urlCampaignId}
          sessionId={openSession.id}
          onClose={() => setPacketOpen(false)}
          onError={handleError}
        />
      )}

      {recapOpen && campaign && urlCampaignId && (
        <RecapView
          worldId={worldId}
          campaignId={urlCampaignId}
          campaignName={campaign.name}
          onClose={() => setRecapOpen(false)}
          onError={handleError}
        />
      )}
    </div>
  );
}
