import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { aiApi, arcsApi, sessionsApi, Beat, Session } from '../api/client';
import { SessionPacketView } from './SessionPacketView';
import { RecapView } from './RecapView';
import { CheatSheetView } from './CheatSheetView';
import { LooseThreadsPanel } from '../components/LooseThreadsPanel';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { renderMarkdown } from '../lib/markdown';
import { fetchCampaignBeats } from '../lib/beats';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { toast } from 'sonner';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';

interface Props {
  worldId: string;
  campaignId: string;
  campaignName: string;
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  title: string;
  sessionNumber: string;
  date: string;
  summary: string;
  notes: string;
}

const EMPTY: Draft = { id: null, title: '', sessionNumber: '', date: '', summary: '', notes: '' };

export function SessionLog({ worldId, campaignId, campaignName, onError }: Props) {
  const api = useMemo(() => sessionsApi(worldId, campaignId), [worldId, campaignId]);
  const ai = useMemo(() => aiApi(worldId), [worldId]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [campaignBeats, setCampaignBeats] = useState<Beat[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  // Which session is open (read or edit) and in which mode — mirrors the
  // article/statblock read-then-edit pattern instead of an always-open form.
  const [openSessionId, setOpenSessionId] = useState<string | null>(null);
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  // On-demand AI digest of the open session's GM notes (ADR-0082) — never persisted.
  const [summarizing, setSummarizing] = useState(false);
  const [summaryText, setSummaryText] = useState<string | null>(null);
  const [summaryError, setSummaryError] = useState<string | null>(null);
  // Session whose print packet is open (null = none).
  const [packetSessionId, setPacketSessionId] = useState<string | null>(null);
  // FR-45: printable "story so far" recap (null = closed).
  const [recapOpen, setRecapOpen] = useState(false);
  // FR-37: session whose cheat sheet is being edited (null = closed).
  const [cheatSession, setCheatSession] = useState<Session | null>(null);
  // Live view of the open sheet's session, so a title edit renames the panel.
  const cheatOpen = cheatSession
    ? sessions.find((s) => s.id === cheatSession.id) ?? null
    : null;
  const openSession = openSessionId ? sessions.find((s) => s.id === openSessionId) ?? null : null;
  const openBeats = openSessionId
    ? campaignBeats.filter((b) => b.sessionId === openSessionId).sort((a, b) => a.position - b.position)
    : [];

  const refresh = useCallback(async () => {
    try {
      setSessions(await api.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    let active = true;
    arcsApi(worldId, campaignId)
      .list()
      .then((arcs) => fetchCampaignBeats(worldId, campaignId, arcs))
      .then((beats) => active && setCampaignBeats(beats))
      .catch(onError);
    return () => {
      active = false;
    };
  }, [worldId, campaignId, onError]);

  function resetSummary() {
    setSummarizing(false);
    setSummaryText(null);
    setSummaryError(null);
  }

  function openRead(s: Session) {
    setOpenSessionId(s.id);
    setMode('read');
    resetSummary();
  }

  function startCreate() {
    setDraft(EMPTY);
    setOpenSessionId(null);
    setMode('edit');
    resetSummary();
  }

  function startEdit(s: Session) {
    setDraft({
      id: s.id,
      title: s.title,
      sessionNumber: s.sessionNumber != null ? String(s.sessionNumber) : '',
      date: s.date ?? '',
      summary: s.summary ?? '',
      notes: s.notes ?? '',
    });
    setOpenSessionId(s.id);
    setMode('edit');
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    const body = {
      title: draft.title,
      sessionNumber: draft.sessionNumber ? Number(draft.sessionNumber) : null,
      date: draft.date || null,
      summary: draft.summary || null,
      notes: draft.notes || null,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      setDraft(EMPTY);
      openRead(saved);
      await refresh();
      toast.success(`Session "${body.title}" saved`);
    } catch (err) {
      onError(err);
    }
  }

  async function remove(id: string) {
    try {
      await api.remove(id);
      if (draft.id === id) setDraft(EMPTY);
      if (openSessionId === id) setOpenSessionId(null);
      if (cheatSession?.id === id) setCheatSession(null);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function summarizeNotes() {
    if (!openSession?.notes) return;
    setSummarizing(true);
    setSummaryError(null);
    try {
      const result = await ai.summarizeSessionNotes(openSession.notes);
      setSummaryText(result.text);
    } catch (err) {
      setSummaryError(err instanceof Error ? err.message : 'Summary failed');
    } finally {
      setSummarizing(false);
    }
  }

  return (
    <section className="card">
      <h3 className="session-heading">
        Sessions
        <Button
          variant="link"
          onClick={() => setRecapOpen(true)}
          title="Print the story so far for the next session"
        >
          🖨 Recap
        </Button>
      </h3>

      <div className="editor-actions">
        <Button type="button" onClick={startCreate}>
          + Add session
        </Button>
      </div>

      {mode === 'edit' && (
        <form className="session-form" onSubmit={save}>
          <div className="session-form-row">
            <Input
              type="number"
              className="num-input"
              placeholder="#"
              value={draft.sessionNumber}
              onChange={(e) => setDraft({ ...draft, sessionNumber: e.target.value })}
            />
            <Input
              type="date"
              value={draft.date}
              onChange={(e) => setDraft({ ...draft, date: e.target.value })}
            />
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
          {draft.id && (
            <LooseThreadsPanel
              worldId={worldId}
              campaignId={campaignId}
              sessionId={draft.id}
              onError={onError}
            />
          )}
          <div className="editor-actions">
            <Button type="submit" disabled={!draft.title}>
              {draft.id ? 'Save session' : 'Add session'}
            </Button>
            {draft.id && (
              <Button type="button" variant="link" onClick={() => setMode('read')}>
                Cancel
              </Button>
            )}
          </div>
        </form>
      )}

      {mode === 'read' && openSession && (
        <article className="article-read">
          <div className="article-read-head">
            <h2>
              {openSession.sessionNumber != null ? `#${openSession.sessionNumber} ` : ''}
              {openSession.title}
              {openSession.date && <span className="print-kicker"> — {openSession.date}</span>}
            </h2>
            <div className="editor-actions">
              <Button type="button" onClick={() => startEdit(openSession)}>
                Edit
              </Button>
              <Button
                variant="link"
                onClick={() => setCheatSession(openSession)}
                title="Compose a condensed one-page GM cheat sheet for this session"
              >
                📋 Cheat sheet
              </Button>
              <Button
                variant="link"
                onClick={() => setPacketSessionId(openSession.id)}
                title="Print a one-page prep packet for this session"
              >
                🖨 Packet
              </Button>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    Delete
                  </Button>
                }
                title="Delete session?"
                description={`This permanently deletes "${openSession.title}" and cannot be undone.`}
                onConfirm={() => remove(openSession.id)}
              />
            </div>
          </div>

          {openSession.summary ? (
            <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(openSession.summary) }} />
          ) : (
            <p className="muted">(no summary)</p>
          )}

          <strong className="muted">Story beats in this session</strong>
          {openBeats.length === 0 && <p className="muted">No beats tagged to this session yet.</p>}
          {openBeats.length > 0 && (
            <ul>
              {openBeats.map((b) => (
                <li key={b.id}>
                  <strong className={b.done ? 'beat-done' : ''}>{b.title}</strong>
                  {b.body && (
                    <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(b.body) }} />
                  )}
                </li>
              ))}
            </ul>
          )}

          <strong className="muted">GM notes (private)</strong>
          {openSession.notes ? (
            <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(openSession.notes) }} />
          ) : (
            <p className="muted">(no notes)</p>
          )}
          {openSession.notes && (
            <div className="editor-actions">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={summarizing}
                onClick={() => void summarizeNotes()}
              >
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

          <LooseThreadsPanel
            worldId={worldId}
            campaignId={campaignId}
            sessionId={openSession.id}
            onError={onError}
            readOnly
          />
        </article>
      )}

      {cheatOpen && (
        <CheatSheetView
          worldId={worldId}
          campaignId={campaignId}
          sessionId={cheatOpen.id}
          sessionTitle={
            cheatOpen.sessionNumber != null
              ? `Session ${cheatOpen.sessionNumber}: ${cheatOpen.title}`
              : cheatOpen.title
          }
          onClose={() => setCheatSession(null)}
          onError={onError}
        />
      )}

      <ol className="session-list">
        {sessions.map((s) => (
          <li key={s.id} className="session-item">
            <div className="session-meta">
              {s.sessionNumber != null && <span className="session-num">#{s.sessionNumber}</span>}
              {s.date && <span className="muted">{s.date}</span>}
            </div>
            <button
              className={s.id === openSessionId ? 'article-link active' : 'article-link'}
              onClick={() => openRead(s)}
            >
              <strong>{s.title}</strong>
            </button>
          </li>
        ))}
        {loading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!loading && sessions.length === 0 && (
          <li className="muted">No sessions logged yet.</li>
        )}
      </ol>

      {packetSessionId && (
        <SessionPacketView
          worldId={worldId}
          campaignId={campaignId}
          sessionId={packetSessionId}
          onClose={() => setPacketSessionId(null)}
          onError={onError}
        />
      )}

      {recapOpen && (
        <RecapView
          worldId={worldId}
          campaignId={campaignId}
          campaignName={campaignName}
          onClose={() => setRecapOpen(false)}
          onError={onError}
        />
      )}
    </section>
  );
}
