import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  arcsApi,
  beatsApi,
  Arc,
  ArcStatus,
  ARC_STATUSES,
  Beat,
  ArticleSummary,
} from '../api/client';

interface Props {
  worldId: string;
  campaignId: string;
  articles: ArticleSummary[];
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
}

export function ArcBoard({ worldId, campaignId, articles, onOpenArticle, onError }: Props) {
  const api = useMemo(() => arcsApi(worldId, campaignId), [worldId, campaignId]);
  const [arcs, setArcs] = useState<Arc[]>([]);
  const [newTitle, setNewTitle] = useState('');

  const refresh = useCallback(async () => {
    try {
      setArcs(await api.list());
    } catch (err) {
      onError(err);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

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
            onOpenArticle={onOpenArticle}
            onError={onError}
            onStatus={(s) => setStatus(arc, s)}
            onRemove={() => removeArc(arc)}
          />
        ))}
        {arcs.length === 0 && <p className="muted">No arcs yet.</p>}
      </div>
    </section>
  );
}

interface ArcCardProps {
  worldId: string;
  campaignId: string;
  arc: Arc;
  articles: ArticleSummary[];
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
  onStatus: (status: ArcStatus) => void;
  onRemove: () => void;
}

function ArcCard({
  worldId,
  campaignId,
  arc,
  articles,
  onOpenArticle,
  onError,
  onStatus,
  onRemove,
}: ArcCardProps) {
  const api = useMemo(() => beatsApi(worldId, campaignId, arc.id), [worldId, campaignId, arc.id]);
  const [beats, setBeats] = useState<Beat[]>([]);
  const [open, setOpen] = useState(false);
  const [beatTitle, setBeatTitle] = useState('');
  const [beatArticle, setBeatArticle] = useState('');
  const titleById = useMemo(() => new Map(articles.map((a) => [a.id, a.title])), [articles]);

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
      await api.create({ title: beatTitle, articleId: beatArticle || null });
      setBeatTitle('');
      setBeatArticle('');
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
        articleId: beat.articleId,
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
                <label className="beat-check">
                  <input type="checkbox" checked={b.done} onChange={() => toggle(b)} />
                  <span className={b.done ? 'beat-done' : ''}>{b.title}</span>
                </label>
                {b.articleId && titleById.has(b.articleId) && (
                  <button className="link-button beat-link" onClick={() => onOpenArticle(b.articleId!)}>
                    {titleById.get(b.articleId)}
                  </button>
                )}
                <button className="link-button danger" onClick={() => removeBeat(b)}>
                  ✕
                </button>
              </li>
            ))}
            {beats.length === 0 && <li className="muted">No beats yet.</li>}
          </ul>
          <form className="beat-form" onSubmit={addBeat}>
            <input
              placeholder="New beat"
              value={beatTitle}
              onChange={(e) => setBeatTitle(e.target.value)}
            />
            <select value={beatArticle} onChange={(e) => setBeatArticle(e.target.value)}>
              <option value="">link article…</option>
              {articles.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.title}
                </option>
              ))}
            </select>
            <button type="submit" disabled={!beatTitle}>
              Add
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
