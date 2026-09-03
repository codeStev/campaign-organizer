import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  arcsApi,
  beatsApi,
  beatKindsApi,
  sessionsApi,
  campaignsApi,
  articlesApi,
  statblocksApi,
  encountersApi,
  Arc,
  ArcStatus,
  ARC_STATUSES,
  Beat,
  BeatKind,
  Session,
  Campaign,
  ArticleSummary,
  Statblock,
  Encounter,
  ApiError,
} from '../api/client';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { renderMarkdown } from '../lib/markdown';
import { Button } from '../components/ui/button';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { Badge } from '../components/ui/badge';
import { Spinner } from '../components/ui/spinner';
import { PromptDialog } from '../components/PromptDialog';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { toast } from 'sonner';

// Radix Select can't use "" as an item value (reserved for "no selection").
const NONE_VALUE = '__none__';

function sessionLabel(s: Session): string {
  const num = s.sessionNumber != null ? `#${s.sessionNumber} ` : '';
  const date = s.date ? `${s.date} ` : '';
  return `${num}${date}${s.title}`.trim();
}

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

interface BeatDraft {
  title: string;
  body: string;
  articleIds: string[];
  statblockIds: string[];
  encounterIds: string[];
  sessionId: string;
  kindId: string;
}

const EMPTY_BEAT_DRAFT: BeatDraft = {
  title: '',
  body: '',
  articleIds: [],
  statblockIds: [],
  encounterIds: [],
  sessionId: '',
  kindId: '',
};

/**
 * Story arcs get their own screen, same treatment as Sessions (ADR-0105
 * follow-up): a campaign picker, that campaign's arcs in a plain list, and
 * one arc's full beat-management view — instead of ArcBoard's old pattern
 * of listing every arc inline (each an expand/collapse card) on the
 * Campaigns workspace. Beat CRUD logic is ported from ArcBoard's ArcCard
 * near-verbatim; only the always-visible-once-open framing changes.
 */
export function NextArcsPage({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { campaignId: urlCampaignId, arcId: urlArcId } = useParams<{ campaignId?: string; arcId?: string }>();
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [arcs, setArcs] = useState<Arc[]>([]);
  const [loading, setLoading] = useState(true);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [beatKinds, setBeatKinds] = useState<BeatKind[]>([]);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [statblocks, setStatblocks] = useState<Statblock[]>([]);
  const [encounters, setEncounters] = useState<Encounter[]>([]);
  const [beats, setBeats] = useState<Beat[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [newArcTitle, setNewArcTitle] = useState('');

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  useEffect(() => {
    campaignsApi(worldId).list().then(setCampaigns).catch(handleError);
    articlesApi(worldId).list().then(setArticles).catch(handleError);
    statblocksApi(worldId).list().then(setStatblocks).catch(handleError);
  }, [worldId, handleError]);

  const arcApi = useMemo(
    () => (urlCampaignId ? arcsApi(worldId, urlCampaignId) : null),
    [worldId, urlCampaignId],
  );
  const beatKindApi = useMemo(() => beatKindsApi(worldId), [worldId]);

  const refreshArcs = useCallback(async () => {
    if (!arcApi) {
      setArcs([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      setArcs(await arcApi.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [arcApi, handleError]);

  useEffect(() => {
    void refreshArcs();
  }, [refreshArcs]);

  useEffect(() => {
    if (!urlCampaignId) {
      setSessions([]);
      setEncounters([]);
      return;
    }
    sessionsApi(worldId, urlCampaignId).list().then(setSessions).catch(handleError);
    encountersApi(worldId, urlCampaignId).list().then(setEncounters).catch(handleError);
    beatKindApi.list().then(setBeatKinds).catch(handleError);
  }, [worldId, urlCampaignId, beatKindApi, handleError]);

  const arc = urlArcId ? arcs.find((a) => a.id === urlArcId) ?? null : null;
  const beatsApiRef = useMemo(
    () => (urlCampaignId && urlArcId ? beatsApi(worldId, urlCampaignId, urlArcId) : null),
    [worldId, urlCampaignId, urlArcId],
  );

  const refreshBeats = useCallback(async () => {
    if (!beatsApiRef) {
      setBeats([]);
      return;
    }
    try {
      setBeats(await beatsApiRef.list());
    } catch (err) {
      handleError(err);
    }
  }, [beatsApiRef, handleError]);

  useEffect(() => {
    void refreshBeats();
  }, [refreshBeats]);

  const titleById = useMemo(() => new Map(articles.map((a) => [a.id, a.title])), [articles]);
  const statblockNameById = useMemo(() => new Map(statblocks.map((s) => [s.id, s.name])), [statblocks]);
  const encounterNameById = useMemo(() => new Map(encounters.map((e) => [e.id, e.name])), [encounters]);
  const sessionById = useMemo(() => new Map(sessions.map((s) => [s.id, s])), [sessions]);
  const kindById = useMemo(() => new Map(beatKinds.map((k) => [k.id, k])), [beatKinds]);

  async function addArc(e: FormEvent) {
    e.preventDefault();
    if (!newArcTitle || !arcApi) return;
    try {
      const created = await arcApi.create({ title: newArcTitle });
      setNewArcTitle('');
      await refreshArcs();
      toast.success(`Arc "${created.title}" created`);
    } catch (err) {
      handleError(err);
    }
  }

  async function setStatus(status: ArcStatus) {
    if (!arc || !arcApi) return;
    try {
      await arcApi.update(arc.id, { title: arc.title, description: arc.description, status });
      await refreshArcs();
      toast.success(`"${arc.title}" marked ${status.toLowerCase()}`);
    } catch (err) {
      handleError(err);
    }
  }

  async function removeArc() {
    if (!arc || !arcApi || !urlCampaignId) return;
    try {
      await arcApi.remove(arc.id);
      await refreshArcs();
      navigate(`/next/worlds/${worldId}/arcs/${urlCampaignId}`);
    } catch (err) {
      handleError(err);
    }
  }

  async function createBeatKind(name: string): Promise<BeatKind | undefined> {
    try {
      const created = await beatKindApi.create({ name });
      setBeatKinds((k) => [...k, created]);
      toast.success(`Beat kind "${created.name}" added`);
      return created;
    } catch (err) {
      handleError(err);
      return undefined;
    }
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        {error && <p className="error">{error}</p>}
        <p className="eyebrow">Campaigns</p>
        <ul className="article-list">
          {campaigns.map((c) => (
            <li key={c.id}>
              <button
                className={c.id === urlCampaignId ? 'article-link active' : 'article-link'}
                onClick={() => navigate(`/next/worlds/${worldId}/arcs/${c.id}`)}
              >
                <TruncatedLabel label={c.name}>{c.name}</TruncatedLabel>
              </button>
            </li>
          ))}
          {campaigns.length === 0 && <li className="muted">No campaigns yet.</li>}
        </ul>

        {urlCampaignId && (
          <>
            <p className="eyebrow">Story arcs</p>
            <ul className="article-list">
              {arcs.map((a) => (
                <li key={a.id}>
                  <button
                    className={a.id === urlArcId ? 'article-link active' : 'article-link'}
                    onClick={() => navigate(`/next/worlds/${worldId}/arcs/${urlCampaignId}/${a.id}`)}
                  >
                    <TruncatedLabel label={a.title}>
                      <span className={`arc-status arc-${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>{' '}
                      {a.title}
                    </TruncatedLabel>
                  </button>
                </li>
              ))}
              {loading && (
                <li className="muted loading-row">
                  <Spinner /> Loading…
                </li>
              )}
              {!loading && arcs.length === 0 && <li className="muted">No arcs yet.</li>}
            </ul>
            <form className="editor-actions" onSubmit={addArc}>
              <Input placeholder="New arc title" value={newArcTitle} onChange={(e) => setNewArcTitle(e.target.value)} />
              <Button type="submit" size="sm" disabled={!newArcTitle}>
                Add
              </Button>
            </form>
          </>
        )}
      </aside>

      <div className="wiki-main">
        {!urlCampaignId && <p className="muted">Select a campaign to see its story arcs.</p>}
        {urlCampaignId && !arc && <p className="muted">Select an arc from the list, or create a new one.</p>}

        {arc && (
          <div className="card">
            <div className="arc-head">
              <strong>{arc.title}</strong>
              <span className={`arc-status arc-${arc.status.toLowerCase()}`}>{arc.status.toLowerCase()}</span>
              <Select value={arc.status} onValueChange={(v) => void setStatus(v as ArcStatus)}>
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
              <span className="bf-spacer" />
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    Delete arc
                  </Button>
                }
                title="Delete arc?"
                description={`This permanently deletes "${arc.title}" and its beats. This cannot be undone.`}
                onConfirm={() => void removeArc()}
              />
            </div>

            <ArcBeats
              beats={beats}
              titleById={titleById}
              statblockNameById={statblockNameById}
              encounterNameById={encounterNameById}
              sessionById={sessionById}
              kindById={kindById}
              articles={articles}
              statblocks={statblocks}
              encounters={encounters}
              sessions={sessions}
              beatKinds={beatKinds}
              beatsApiRef={beatsApiRef}
              refreshBeats={refreshBeats}
              onCreateBeatKind={createBeatKind}
              onOpenArticle={onOpenArticle}
              onError={handleError}
            />
          </div>
        )}
      </div>
    </div>
  );
}

interface ArcBeatsProps {
  beats: Beat[];
  titleById: Map<string, string>;
  statblockNameById: Map<string, string>;
  encounterNameById: Map<string, string>;
  sessionById: Map<string, Session>;
  kindById: Map<string, BeatKind>;
  articles: ArticleSummary[];
  statblocks: Statblock[];
  encounters: Encounter[];
  sessions: Session[];
  beatKinds: BeatKind[];
  beatsApiRef: ReturnType<typeof beatsApi> | null;
  refreshBeats: () => Promise<void>;
  onCreateBeatKind: (name: string) => Promise<BeatKind | undefined>;
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
}

/** Beat list + create/edit form for the open arc — ported from ArcBoard's ArcCard. */
function ArcBeats({
  beats,
  titleById,
  statblockNameById,
  encounterNameById,
  sessionById,
  kindById,
  articles,
  statblocks,
  encounters,
  sessions,
  beatKinds,
  beatsApiRef,
  refreshBeats,
  onCreateBeatKind,
  onOpenArticle,
  onError,
}: ArcBeatsProps) {
  const [beatTitle, setBeatTitle] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<BeatDraft>(EMPTY_BEAT_DRAFT);
  const [newKindOpen, setNewKindOpen] = useState(false);

  async function addBeat(e: FormEvent) {
    e.preventDefault();
    if (!beatTitle || !beatsApiRef) return;
    try {
      const created = await beatsApiRef.create({ title: beatTitle });
      setBeatTitle('');
      await refreshBeats();
      toast.success(`Beat "${created.title}" added`);
    } catch (err) {
      onError(err);
    }
  }

  async function toggle(beat: Beat) {
    if (!beatsApiRef) return;
    try {
      await beatsApiRef.update(beat.id, {
        title: beat.title,
        body: beat.body,
        done: !beat.done,
        articleIds: beat.articleIds,
        statblockIds: beat.statblockIds,
        encounterIds: beat.encounterIds,
        sessionId: beat.sessionId,
        kindId: beat.kindId,
        position: beat.position,
      });
      await refreshBeats();
    } catch (err) {
      onError(err);
    }
  }

  async function removeBeat(beat: Beat) {
    if (!beatsApiRef) return;
    try {
      await beatsApiRef.remove(beat.id);
      await refreshBeats();
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
      encounterIds: beat.encounterIds ?? [],
      sessionId: beat.sessionId ?? '',
      kindId: beat.kindId ?? '',
    });
  }

  function addDraftArticle(id: string) {
    if (id && !draft.articleIds.includes(id)) setDraft({ ...draft, articleIds: [...draft.articleIds, id] });
  }
  function addDraftStatblock(id: string) {
    if (id && !draft.statblockIds.includes(id)) setDraft({ ...draft, statblockIds: [...draft.statblockIds, id] });
  }
  function addDraftEncounter(id: string) {
    if (id && !draft.encounterIds.includes(id)) setDraft({ ...draft, encounterIds: [...draft.encounterIds, id] });
  }

  async function saveEdit(beat: Beat) {
    if (!beatsApiRef) return;
    try {
      await beatsApiRef.update(beat.id, {
        title: draft.title || beat.title,
        body: draft.body || null,
        done: beat.done,
        articleIds: draft.articleIds,
        statblockIds: draft.statblockIds,
        encounterIds: draft.encounterIds,
        sessionId: draft.sessionId || null,
        kindId: draft.kindId || null,
        position: beat.position,
      });
      setEditingId(null);
      await refreshBeats();
      toast.success('Beat saved');
    } catch (err) {
      onError(err);
    }
  }

  return (
    <div className="arc-beats">
      <ul className="beat-list">
        {beats.map((b) => (
          <li key={b.id} className="beat-item">
            <div className="beat-row">
              <label className="beat-check">
                <Checkbox checked={b.done} onCheckedChange={() => void toggle(b)} />
                {b.kindId && kindById.has(b.kindId) && (
                  <span
                    className="beat-kind-dot"
                    title={kindById.get(b.kindId)!.name}
                    style={{ backgroundColor: kindById.get(b.kindId)!.color ?? '#888888' }}
                  />
                )}
                <span className={b.done ? 'beat-done' : ''}>{b.title}</span>
              </label>
              {b.articleIds
                .filter((id) => titleById.has(id))
                .map((id) => (
                  <Button key={id} variant="link" className="beat-link" onClick={() => onOpenArticle(id)}>
                    {titleById.get(id)}
                  </Button>
                ))}
              {(() => {
                const names = b.statblockIds.filter((id) => statblockNameById.has(id)).map((id) => statblockNameById.get(id));
                return names.length > 0 ? (
                  <Badge variant="secondary" title={names.join(', ')}>
                    ⚔ {names.length} statblock{names.length === 1 ? '' : 's'}
                  </Badge>
                ) : null;
              })()}
              {(() => {
                const names = b.encounterIds.filter((id) => encounterNameById.has(id)).map((id) => encounterNameById.get(id));
                return names.length > 0 ? (
                  <Badge variant="secondary" title={names.join(', ')}>
                    ⚔ {names.length} encounter{names.length === 1 ? '' : 's'}
                  </Badge>
                ) : null;
              })()}
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
                onConfirm={() => void removeBeat(b)}
              />
            </div>

            {editingId !== b.id && b.body && (
              <div className="beat-body muted preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(b.body) }} />
            )}

            {editingId === b.id && (
              <div className="beat-editor">
                <Input
                  value={draft.title}
                  placeholder="Beat title"
                  onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                />
                <MarkdownEditor value={draft.body} onChange={(body) => setDraft({ ...draft, body })} />
                {(draft.articleIds.length > 0 || draft.statblockIds.length > 0 || draft.encounterIds.length > 0) && (
                  <div className="beat-article-chips">
                    {draft.articleIds.map((id) => (
                      <span key={id} className="beat-chip">
                        {titleById.get(id) ?? 'article'}
                        <Button
                          type="button"
                          variant="link"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setDraft({ ...draft, articleIds: draft.articleIds.filter((x) => x !== id) })}
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
                          onClick={() => setDraft({ ...draft, statblockIds: draft.statblockIds.filter((x) => x !== id) })}
                        >
                          ✕
                        </Button>
                      </span>
                    ))}
                    {draft.encounterIds.map((id) => (
                      <span key={id} className="beat-chip beat-chip-encounter">
                        ⚔ {encounterNameById.get(id) ?? 'encounter'}
                        <Button
                          type="button"
                          variant="link"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setDraft({ ...draft, encounterIds: draft.encounterIds.filter((x) => x !== id) })}
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
                  <Select value="" onValueChange={addDraftEncounter}>
                    <SelectTrigger>
                      <SelectValue placeholder="+ link encounter" />
                    </SelectTrigger>
                    <SelectContent>
                      {encounters
                        .filter((e) => !draft.encounterIds.includes(e.id))
                        .map((e) => (
                          <SelectItem key={e.id} value={e.id}>
                            {e.name}
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
                  <Select
                    value={draft.kindId || NONE_VALUE}
                    onValueChange={(v) => setDraft({ ...draft, kindId: v === NONE_VALUE ? '' : v })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Beat kind…" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE_VALUE}>— none —</SelectItem>
                      {beatKinds.map((k) => (
                        <SelectItem key={k.id} value={k.id}>
                          {k.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button type="button" variant="link" onClick={() => setNewKindOpen(true)}>
                    + New kind
                  </Button>
                </div>
                <PromptDialog
                  open={newKindOpen}
                  onOpenChange={setNewKindOpen}
                  title="New beat kind"
                  label="Name"
                  onSubmit={(value) =>
                    void onCreateBeatKind(value).then((created) => {
                      if (created) setDraft((d) => ({ ...d, kindId: created.id }));
                    })
                  }
                />
                <div className="editor-actions">
                  <Button onClick={() => void saveEdit(b)}>Save beat</Button>
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
        <Input placeholder="New beat — then Edit to add notes & links" value={beatTitle} onChange={(e) => setBeatTitle(e.target.value)} />
        <Button type="submit" disabled={!beatTitle}>
          Add
        </Button>
      </form>
    </div>
  );
}
