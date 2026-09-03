import { MouseEvent, useEffect, useState } from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';
import { articlesApi, articleTagsApi, Article, ArticleSummary, ApiError } from '../api/client';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Spinner } from '../components/ui/spinner';
import { ArticleTreeItem } from './WorldView';
import { TagList } from '../components/TagInput';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

/**
 * Wiki (docs/ui-overhaul-plan.md Phase 2) — a real nested article tree
 * (ADR-0080's parentArticleId, collapsed by default), reusing WorldView's
 * own tree-building/rendering (exported for this purpose) rather than the
 * tag-prefix scheme the plan originally guessed at before this structural
 * hierarchy was spotted. Read-only for now: editing/creating stays on the
 * old UI's richer editor (draft handling, revisions, GM-only block, AI
 * draft) until that gets its own /next pass.
 */
export function NextWikiPage({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { articleId } = useParams<{ articleId: string }>();
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [article, setArticle] = useState<Article | null>(null);
  const [tags, setTags] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  const onError = (err: unknown) => {
    if (err instanceof ApiError && err.status === 401) return onAuthExpired();
    setError(err instanceof Error ? err.message : 'Something went wrong');
  };

  useEffect(() => {
    setLoading(true);
    const timeout = setTimeout(() => {
      articlesApi(worldId)
        .list({ q: query || undefined })
        .then(setArticles)
        .catch(onError)
        .finally(() => setLoading(false));
    }, 200);
    return () => clearTimeout(timeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId, query]);

  useEffect(() => {
    if (!articleId) {
      setArticle(null);
      setTags([]);
      return;
    }
    articlesApi(worldId).get(articleId).then(setArticle).catch(onError);
    articleTagsApi(worldId, articleId)
      .get()
      .then((t) => setTags(t.tags))
      .catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId, articleId]);

  function openArticle(id: string) {
    navigate(id);
  }

  function toggleExpanded(id: string) {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function handleBodyClick(event: MouseEvent<HTMLDivElement>) {
    const link = (event.target as HTMLElement).closest('.wiki-link');
    if (link) {
      event.preventDefault();
      const id = link.getAttribute('data-article-id');
      if (id) openArticle(id);
    }
  }

  // Tree built from the currently-visible (search-filtered) set; an article
  // whose parent got filtered out falls back to the root so search never
  // makes it unreachable — same rule as the old UI's sidebar (ADR-0080).
  const visibleIds = new Set(articles.map((a) => a.id));
  const childrenByParent = new Map<string, ArticleSummary[]>();
  const rootArticles: ArticleSummary[] = [];
  for (const a of articles) {
    if (a.parentArticleId && visibleIds.has(a.parentArticleId)) {
      const bucket = childrenByParent.get(a.parentArticleId) ?? [];
      bucket.push(a);
      childrenByParent.set(a.parentArticleId, bucket);
    } else {
      rootArticles.push(a);
    }
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <Input placeholder="Search articles…" value={query} onChange={(e) => setQuery(e.target.value)} />
        {error && <p className="error">{error}</p>}
        <ul className="article-list article-list-scroll">
          {rootArticles.map((a) => (
            <ArticleTreeItem
              key={a.id}
              article={a}
              childrenByParent={childrenByParent}
              expandedIds={expandedIds}
              onToggleExpand={toggleExpanded}
              activeId={articleId ?? null}
              onOpen={openArticle}
            />
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && articles.length === 0 && <li className="muted">No articles found.</li>}
        </ul>
      </aside>
      {article ? (
        <article className="card article-read">
          <div className="article-read-head">
            <div>
              <h3>{article.title}</h3>
              <small className="muted">{article.template.toLowerCase()}</small>
              <TagList worldId={worldId} tags={tags} />
            </div>
            <Button variant="link" size="sm" asChild>
              <NavLink to={`/worlds/${worldId}/articles/${article.id}`}>Edit in current UI →</NavLink>
            </Button>
          </div>
          <div
            className="preview-body"
            onClick={handleBodyClick}
            dangerouslySetInnerHTML={{ __html: article.bodyHtml || '<p class="muted">(empty)</p>' }}
          />
        </article>
      ) : (
        <p className="muted">Select an article from the list, or create one in the current UI.</p>
      )}
    </div>
  );
}
