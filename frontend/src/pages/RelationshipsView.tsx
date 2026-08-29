import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  relationshipsApi,
  articlesApi,
  Relationship,
  ArticleSummary,
  ApiError,
} from '../api/client';
import { RelationshipGraph } from '../components/RelationshipGraph';
import { Button } from '../components/ui/button';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';

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
          <Select value={from} onValueChange={setFrom}>
            <SelectTrigger>
              <SelectValue placeholder="From…" />
            </SelectTrigger>
            <SelectContent>
              {articles.map((a) => (
                <SelectItem key={a.id} value={a.id}>
                  {a.title}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            placeholder="label, e.g. parent of"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
          />
          <Select value={to} onValueChange={setTo}>
            <SelectTrigger>
              <SelectValue placeholder="To…" />
            </SelectTrigger>
            <SelectContent>
              {articles.map((a) => (
                <SelectItem key={a.id} value={a.id}>
                  {a.title}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <label className="layer-toggle">
            <Checkbox checked={directed} onCheckedChange={(checked) => setDirected(checked === true)} />
            Directed (arrow)
          </label>
          <Button type="submit" disabled={!from || !to}>
            Add relationship
          </Button>
        </form>

        <ul className="article-list">
          {relationships.map((r) => (
            <li key={r.id} className="rel-row">
              <span>
                {labels.get(r.fromArticleId) ?? '?'}{' '}
                <em className="muted">{r.label || (r.directed ? '→' : '—')}</em>{' '}
                {labels.get(r.toArticleId) ?? '?'}
              </span>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    ✕
                  </Button>
                }
                title="Delete relationship?"
                description={`This removes the link between "${labels.get(r.fromArticleId) ?? '?'}" and "${labels.get(r.toArticleId) ?? '?'}". This cannot be undone.`}
                onConfirm={() => removeRelationship(r.id)}
              />
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
