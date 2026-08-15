import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  arcsApi,
  beatsApi,
  sessionsApi,
  Arc,
  ArcStatus,
  ARC_STATUSES,
  Beat,
  Session,
  ArticleSummary,
  Statblock,
} from '../api/client';

function sessionLabel(s: Session): string {
  const num = s.sessionNumber != null ? `#${s.sessionNumber} ` : '';
  const date = s.date ? `${s.date} ` : '';
  return `${num}${date}${s.title}`.trim();
}

interface Props {
  worldId: string;
  campaignId: string;
  articles: ArticleSummary[];
  statblocks: Statblock[];
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
}

export function ArcBoard({ worldId, campaignId, articles, statblocks, onOpenArticle, onError }: Props) {
  const api = useMemo(() => arcsApi(worldId, campaignId), [worldId, campaignId]);
  const sessionApi = useMemo(() => sessionsApi(worldId, campaignId), [worldId, campaignId]);
  const [arcs, setArcs] = useState<Arc[]>([]);
  const [loading, setLoading] = useState(true);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [newTitle, setNewTitle] = useState('');

  const refresh = useCallback(async () => {
    try {
      setArcs(await api.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
    sessionApi.list().then(setSessions).catch(onError);
  }, [refresh, sessionApi, onError]);

  async function addArc(e: FormEvent) {
    e.preventDefault();
    if (!newTitle) return;
    try {
      await api.create({ title: newTitle });
      setNewTitle('');
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function setStatus(arc: Arc, status: ArcStatus) {
    try {
      await api.update(arc.id, { title: arc.title, description: arc.description, status });
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function removeArc(arc: Arc) {
    try {
      await api.remove(arc.id);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  return (
    <section className="card">
      <h3>Story arcs</h3>
      <form className="editor-actions" onSubmit={addArc}>
        <input
          placeholder="New arc title"
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
        />
        <button type="submit" disabled={!newTitle}>
          Add arc
        </button>
      </form>

      <div className="arc-list">
        {arcs.map((arc) => (
          <ArcCard
            key={arc.id}
            worldId={worldId}
            campaignId={campaignId}
            arc={arc}
            articles={articles}
            statblocks={statblocks}
            sessions={sessions}
            onOpenArticle={onOpenArticle}
            onError={onError}
            onStatus={(s) => setStatus(arc, s)}
            onRemove={() => removeArc(arc)}
          />
        ))}
        {loading && <p className="muted">Loading…</p>}
        {!loading && arcs.length === 0 && <p className="muted">No arcs yet.</p>}
      </div>
    </section>
  );
}

interface ArcCardProps {
  worldId: string;
  campaignId: string;
  arc: Arc;
  articles: ArticleSummary[];
  statblocks: Statblock[];
  sessions: Session[];
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
  onStatus: (status: ArcStatus) => void;
  onRemove: () => void;
}

interface BeatDraft {
  title: string;
  body: string;
  articleIds: string[];
  statblockIds: string[];
  sessionId: string;
}

function ArcCard({
  worldId,
  campaignId,
  arc,
  articles,
  statblocks,
  sessions,
  onOpenArticle,
  onError,
  onStatus,
  onRemove,
}: ArcCardProps) {
  const api = useMemo(() => beatsApi(worldId, campaignId, arc.id), [worldId, campaignId, arc.id]);
  const [beats, setBeats] = useState<Beat[]>([]);
  const [open, setOpen] = useState(false);
  const [beatTitle, setBeatTitle] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<BeatDraft>({
    title: '',
    body: '',
    articleIds: [],
    statblockIds: [],
    sessionId: '',
  });
  const titleById = useMemo(() => new Map(articles.map((a) => [a.id, a.title])), [articles]);
  const statblockNameById = useMemo(
    () => new Map(statblocks.map((s) => [s.id, s.name])),
    [statblocks],
  );
  const sessionById = useMemo(() => new Map(sessions.map((s) => [s.id, s])), [sessions]);

  const refresh = useCallback(async () => {
    try {
      setBeats(await api.list());
    } catch (err) {
      onError(err);
    }
  }, [api, onError]);

  useEffect(() => {
    if (open) void refresh();
  }, [open, refresh]);

  async function addBeat(e: FormEvent) {
    e.preventDefault();
    if (!beatTitle) return;
    try {
      await api.create({ title: beatTitle });
      setBeatTitle('');
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function toggle(beat: Beat) {
    try {
      await api.update(beat.id, {
        title: beat.title,
        body: beat.body,
        done: !beat.done,
        articleIds: beat.articleIds,
        statblockIds: beat.statblockIds,
        sessionId: beat.sessionId,
        position: beat.position,
      });
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function removeBeat(beat: Beat) {
    try {
      await api.remove(beat.id);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  function startEdit(beat: Beat) {
    setEditingId(beat.id);
    setDraft({
      title: beat.title,
      body: beat.body ?? '',
      articleIds: beat.articleIds ?? [],
      statblockIds: beat.statblockIds ?? [],
      sessionId: beat.sessionId ?? '',
    });
  }

  function addDraftArticle(id: string) {
    if (id && !draft.articleIds.includes(id)) {
      setDraft({ ...draft, articleIds: [...draft.articleIds, id] });
    }
  }

  function addDraftStatblock(id: string) {
    if (id && !draft.statblockIds.includes(id)) {
      setDraft({ ...draft, statblockIds: [...draft.statblockIds, id] });
    }
  }

  async function saveEdit(beat: Beat) {
    try {
      await api.update(beat.id, {
        title: draft.title || beat.title,
        body: draft.body || null,
        done: beat.done,
        articleIds: draft.articleIds,
        statblockIds: draft.statblockIds,
        sessionId: draft.sessionId || null,
        position: beat.position,
      });
      setEditingId(null);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  return (
    <div className="arc-card">
      <div className="arc-head">
        <button className="arc-toggle" onClick={() => setOpen((v) => !v)}>
          <span className="caret">{open ? '▼' : '▶'}</span>
          <strong>{arc.title}</strong>
        </button>
        <span className={`arc-status arc-${arc.status.toLowerCase()}`}>{arc.status.toLowerCase()}</span>
        <select value={arc.status} onChange={(e) => onStatus(e.target.value as ArcStatus)}>
          {ARC_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s.charAt(0) + s.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
        <button className="link-button danger" onClick={onRemove}>
          ✕
        </button>
      </div>

      {open && (
        <div className="arc-beats">
          <ul className="beat-list">
            {beats.map((b) => (
              <li key={b.id} className="beat-item">
                <div className="beat-row">
                  <label className="beat-check">
                    <input type="checkbox" checked={b.done} onChange={() => toggle(b)} />
                    <span className={b.done ? 'beat-done' : ''}>{b.title}</span>
                  </label>
                  {b.articleIds
                    .filter((id) => titleById.has(id))
                    .map((id) => (
                      <button key={id} className="link-button beat-link" onClick={() => onOpenArticle(id)}>
                        {titleById.get(id)}
                      </button>
                    ))}
                  {b.statblockIds
                    .filter((id) => statblockNameById.has(id))
                    .map((id) => (
                      <span key={id} className="beat-statblock" title="Statblock">
                        ⚔ {statblockNameById.get(id)}
                      </span>
                    ))}
                  {b.sessionId && sessionById.has(b.sessionId) && (
                    <span className="beat-session muted">{sessionLabel(sessionById.get(b.sessionId)!)}</span>
                  )}
                  <span className="bf-spacer" />
                  <button className="link-button" onClick={() => (editingId === b.id ? setEditingId(null) : startEdit(b))}>
                    {editingId === b.id ? 'Close' : 'Edit'}
                  </button>
                  <button className="link-button danger" onClick={() => removeBeat(b)}>
                    ✕
                  </button>
                </div>

                {editingId !== b.id && b.body && <p className="beat-body muted">{b.body}</p>}

                {editingId === b.id && (
                  <div className="beat-editor">
                    <input
                      value={draft.title}
                      placeholder="Beat title"
                      onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                    />
                    <textarea
                      placeholder="Notes — e.g. 'a survivor begs the party for help'"
                      value={draft.body}
                      onChange={(e) => setDraft({ ...draft, body: e.target.value })}
                    />
                    {(draft.articleIds.length > 0 || draft.statblockIds.length > 0) && (
                      <div className="beat-article-chips">
                        {draft.articleIds.map((id) => (
                          <span key={id} className="beat-chip">
                            {titleById.get(id) ?? 'article'}
                            <button
                              type="button"
                              className="link-button danger"
                              onClick={() =>
                                setDraft({ ...draft, articleIds: draft.articleIds.filter((x) => x !== id) })
                              }
                            >
                              ✕
                            </button>
                          </span>
                        ))}
                        {draft.statblockIds.map((id) => (
                          <span key={id} className="beat-chip beat-chip-statblock">
                            ⚔ {statblockNameById.get(id) ?? 'statblock'}
                            <button
                              type="button"
                              className="link-button danger"
                              onClick={() =>
                                setDraft({
                                  ...draft,
                                  statblockIds: draft.statblockIds.filter((x) => x !== id),
                                })
                              }
                            >
                              ✕
                            </button>
                          </span>
                        ))}
                      </div>
                    )}
                    <div className="beat-links">
                      <select value="" onChange={(e) => addDraftArticle(e.target.value)}>
                        <option value="">+ link article (place, NPC…)</option>
                        {articles
                          .filter((a) => !draft.articleIds.includes(a.id))
                          .map((a) => (
                            <option key={a.id} value={a.id}>
                              {a.title}
                            </option>
                          ))}
                      </select>
                      <select value="" onChange={(e) => addDraftStatblock(e.target.value)}>
                        <option value="">+ link statblock (monster, NPC…)</option>
                        {statblocks
                          .filter((s) => !draft.statblockIds.includes(s.id))
                          .map((s) => (
                            <option key={s.id} value={s.id}>
                              {s.name}
                            </option>
                          ))}
                      </select>
                      <select value={draft.sessionId} onChange={(e) => setDraft({ ...draft, sessionId: e.target.value })}>
                        <option value="">link session…</option>
                        {sessions.map((s) => (
                          <option key={s.id} value={s.id}>
                            {sessionLabel(s)}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="editor-actions">
                      <button onClick={() => saveEdit(b)}>Save beat</button>
                      <button className="link-button" onClick={() => setEditingId(null)}>
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </li>
            ))}
            {beats.length === 0 && <li className="muted">No beats yet.</li>}
          </ul>
          <form className="beat-form" onSubmit={addBeat}>
            <input
              placeholder="New beat — then Edit to add notes & links"
              value={beatTitle}
              onChange={(e) => setBeatTitle(e.target.value)}
            />
            <button type="submit" disabled={!beatTitle}>
              Add
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
