import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  relationshipsApi,
  articlesApi,
  Relationship,
  ArticleSummary,
  ApiError,
} from '../api/client';
import { RelationshipGraph } from '../components/RelationshipGraph';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

export function RelationshipsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const api = useMemo(() => relationshipsApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);

  const [relationships, setRelationships] = useState<Relationship[]>([]);
  const [loading, setLoading] = useState(true);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [label, setLabel] = useState('');
  const [directed, setDirected] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(async () => {
    try {
      setRelationships(await api.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refresh();
    articleApi.list().then(setArticles).catch(handleError);
  }, [refresh, articleApi, handleError]);

  const labels = useMemo(
    () => new Map(articles.map((a) => [a.id, a.title])),
    [articles],
  );

  async function addRelationship(e: FormEvent) {
    e.preventDefault();
    if (!from || !to) return;
    setError(null);
    try {
      await api.create({ fromArticleId: from, toArticleId: to, label: label || null, directed });
      setLabel('');
      setTo('');
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  async function removeRelationship(id: string) {
    try {
      await api.remove(id);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <form className="card" onSubmit={addRelationship}>
          <strong>New relationship</strong>
          <select value={from} onChange={(e) => setFrom(e.target.value)} required>
            <option value="">From…</option>
            {articles.map((a) => (
              <option key={a.id} value={a.id}>
                {a.title}
              </option>
            ))}
          </select>
          <input
            placeholder="label, e.g. parent of"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
          />
          <select value={to} onChange={(e) => setTo(e.target.value)} required>
            <option value="">To…</option>
            {articles.map((a) => (
              <option key={a.id} value={a.id}>
                {a.title}
              </option>
            ))}
          </select>
          <label className="layer-toggle">
            <input type="checkbox" checked={directed} onChange={(e) => setDirected(e.target.checked)} />
            Directed (arrow)
          </label>
          <button type="submit" disabled={!from || !to}>
            Add relationship
          </button>
        </form>

        <ul className="article-list">
          {relationships.map((r) => (
            <li key={r.id} className="rel-row">
              <span>
                {labels.get(r.fromArticleId) ?? '?'}{' '}
                <em className="muted">{r.label || (r.directed ? '→' : '—')}</em>{' '}
                {labels.get(r.toArticleId) ?? '?'}
              </span>
              <button className="link-button danger" onClick={() => removeRelationship(r.id)}>
                ✕
              </button>
            </li>
          ))}
          {loading && <li className="muted">Loading…</li>}
          {!loading && relationships.length === 0 && (
            <li className="muted">No relationships yet.</li>
          )}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        <div className="card">
          <RelationshipGraph
            nodeLabels={labels}
            relationships={relationships}
            onSelectNode={onOpenArticle}
          />
          <p className="muted hint">Click a node to open its article.</p>
        </div>
      </div>
    </div>
  );
}
