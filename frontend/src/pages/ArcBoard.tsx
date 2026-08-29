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
import { MarkdownEditor } from '../components/MarkdownEditor';
import { renderMarkdown } from '../lib/markdown';
import { Button } from '../components/ui/button';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { Spinner } from '../components/ui/spinner';
import { toast } from 'sonner';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so a meaningfully persistent "none" state goes through this sentinel.
const NONE_VALUE = '__none__';

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
      const created = await api.create({ title: newTitle });
      setNewTitle('');
      await refresh();
      toast.success(`Arc "${created.title}" created`);
    } catch (err) {
      onError(err);
    }
  }

  async function setStatus(arc: Arc, status: ArcStatus) {
    try {
      await api.update(arc.id, { title: arc.title, description: arc.description, status });
      await refresh();
      toast.success(`"${arc.title}" marked ${status.toLowerCase()}`);
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
        <Input
          placeholder="New arc title"
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
        />
        <Button type="submit" disabled={!newTitle}>
          Add arc
        </Button>
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
        {loading && (
          <p className="muted loading-row">
            <Spinner /> Loading…
          </p>
        )}
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
      const created = await api.create({ title: beatTitle });
      setBeatTitle('');
      await refresh();
      toast.success(`Beat "${created.title}" added`);
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
      toast.success('Beat saved');
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
        <Select value={arc.status} onValueChange={(v) => onStatus(v as ArcStatus)}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {ARC_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {s.charAt(0) + s.slice(1).toLowerCase()}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <ConfirmDeleteDialog
          trigger={
            <Button variant="link" className="text-destructive hover:text-destructive">
              ✕
            </Button>
          }
          title="Delete arc?"
          description={`This permanently deletes "${arc.title}" and its beats. This cannot be undone.`}
          onConfirm={onRemove}
        />
      </div>

      {open && (
        <div className="arc-beats">
          <ul className="beat-list">
            {beats.map((b) => (
              <li key={b.id} className="beat-item">
                <div className="beat-row">
                  <label className="beat-check">
                    <Checkbox checked={b.done} onCheckedChange={() => toggle(b)} />
                    <span className={b.done ? 'beat-done' : ''}>{b.title}</span>
                  </label>
                  {b.articleIds
                    .filter((id) => titleById.has(id))
                    .map((id) => (
                      <Button key={id} variant="link" className="beat-link" onClick={() => onOpenArticle(id)}>
                        {titleById.get(id)}
                      </Button>
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
                  <Button variant="link" onClick={() => (editingId === b.id ? setEditingId(null) : startEdit(b))}>
                    {editingId === b.id ? 'Close' : 'Edit'}
                  </Button>
                  <ConfirmDeleteDialog
                    trigger={
                      <Button variant="link" className="text-destructive hover:text-destructive">
                        ✕
                      </Button>
                    }
                    title="Delete beat?"
                    description={`This permanently deletes "${b.title}" and cannot be undone.`}
                    onConfirm={() => removeBeat(b)}
                  />
                </div>

                {editingId !== b.id && b.body && (
                  <div
                    className="beat-body muted preview-body"
                    dangerouslySetInnerHTML={{ __html: renderMarkdown(b.body) }}
                  />
                )}

                {editingId === b.id && (
                  <div className="beat-editor">
                    <Input
                      value={draft.title}
                      placeholder="Beat title"
                      onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                    />
                    <MarkdownEditor value={draft.body} onChange={(body) => setDraft({ ...draft, body })} />
                    {(draft.articleIds.length > 0 || draft.statblockIds.length > 0) && (
                      <div className="beat-article-chips">
                        {draft.articleIds.map((id) => (
                          <span key={id} className="beat-chip">
                            {titleById.get(id) ?? 'article'}
                            <Button
                              type="button"
                              variant="link"
                              className="text-destructive hover:text-destructive"
                              onClick={() =>
                                setDraft({ ...draft, articleIds: draft.articleIds.filter((x) => x !== id) })
                              }
                            >
                              ✕
                            </Button>
                          </span>
                        ))}
                        {draft.statblockIds.map((id) => (
                          <span key={id} className="beat-chip beat-chip-statblock">
                            ⚔ {statblockNameById.get(id) ?? 'statblock'}
                            <Button
                              type="button"
                              variant="link"
                              className="text-destructive hover:text-destructive"
                              onClick={() =>
                                setDraft({
                                  ...draft,
                                  statblockIds: draft.statblockIds.filter((x) => x !== id),
                                })
                              }
                            >
                              ✕
                            </Button>
                          </span>
                        ))}
                      </div>
                    )}
                    <div className="beat-links">
                      {/* Always shows its placeholder: picking an item fires a one-shot
                          "add to list" side effect rather than persisting a selection, so
                          there's no real empty item to represent - value stays "". */}
                      <Select value="" onValueChange={addDraftArticle}>
                        <SelectTrigger>
                          <SelectValue placeholder="+ link article (place, NPC…)" />
                        </SelectTrigger>
                        <SelectContent>
                          {articles
                            .filter((a) => !draft.articleIds.includes(a.id))
                            .map((a) => (
                              <SelectItem key={a.id} value={a.id}>
                                {a.title}
                              </SelectItem>
                            ))}
                        </SelectContent>
                      </Select>
                      <Select value="" onValueChange={addDraftStatblock}>
                        <SelectTrigger>
                          <SelectValue placeholder="+ link statblock (monster, NPC…)" />
                        </SelectTrigger>
                        <SelectContent>
                          {statblocks
                            .filter((s) => !draft.statblockIds.includes(s.id))
                            .map((s) => (
                              <SelectItem key={s.id} value={s.id}>
                                {s.name}
                              </SelectItem>
                            ))}
                        </SelectContent>
                      </Select>
                      <Select
                        value={draft.sessionId || NONE_VALUE}
                        onValueChange={(v) => setDraft({ ...draft, sessionId: v === NONE_VALUE ? '' : v })}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={NONE_VALUE}>link session…</SelectItem>
                          {sessions.map((s) => (
                            <SelectItem key={s.id} value={s.id}>
                              {sessionLabel(s)}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="editor-actions">
                      <Button onClick={() => saveEdit(b)}>Save beat</Button>
                      <Button variant="link" onClick={() => setEditingId(null)}>
                        Cancel
                      </Button>
                    </div>
                  </div>
                )}
              </li>
            ))}
            {beats.length === 0 && <li className="muted">No beats yet.</li>}
          </ul>
          <form className="beat-form" onSubmit={addBeat}>
            <Input
              placeholder="New beat — then Edit to add notes & links"
              value={beatTitle}
              onChange={(e) => setBeatTitle(e.target.value)}
            />
            <Button type="submit" disabled={!beatTitle}>
              Add
            </Button>
          </form>
        </div>
      )}
    </div>
  );
}
