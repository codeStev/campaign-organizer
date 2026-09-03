import { MouseEvent, useEffect, useMemo, useState } from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';
import {
  articlesApi,
  articleTagsApi,
  categoriesApi,
  worldTagsApi,
  tagBrowseApi,
  Article,
  ArticleSummary,
  Category,
  ApiError,
} from '../api/client';
import { Button } from '../components/ui/button';
import { TagList } from '../components/TagInput';
import { CategoryTree } from '../components/CategoryTree';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

/**
 * Wiki (docs/ui-overhaul-plan.md Phase 2, restructured in the Phase 5c
 * mockup-fidelity pass): a Category tree — matching the mockup's "BY
 * CATEGORY" sidebar — instead of ADR-0080's parentArticleId tree. Categories
 * had full CRUD in the backend/OpenAPI contract already but were never
 * wired into any UI (old or new); this is the first screen to use them, and
 * the tree/drag-and-drop machinery built here was extracted into
 * `<CategoryTree>` (see that file) so Atlas/Handouts/Tables & Decks/Sheets
 * can reuse the exact same component (ADR-0105).
 * Article body editing is ported into `/next` via `<ArticleEditor>`
 * (see that component) — no more "Edit in current UI" link-out.
 */
export function NextWikiPage({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { articleId } = useParams<{ articleId: string }>();
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [article, setArticle] = useState<Article | null>(null);
  const [tags, setTags] = useState<string[]>([]);
  const [worldTags, setWorldTags] = useState<string[]>([]);
  const [selectedTags, setSelectedTags] = useState<Set<string>>(new Set());
  const [tagMatchIds, setTagMatchIds] = useState<Set<string> | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onError = (err: unknown) => {
    if (err instanceof ApiError && err.status === 401) return onAuthExpired();
    setError(err instanceof Error ? err.message : 'Something went wrong');
  };

  function refresh() {
    setLoading(true);
    return Promise.all([articlesApi(worldId).list(), categoriesApi(worldId).list()])
      .then(([a, c]) => {
        setArticles(a);
        setCategories(c);
      })
      .catch(onError)
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    void refresh();
    worldTagsApi(worldId).list().then(setWorldTags).catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId]);

  // Additive (OR) tag filter: an article matches if it carries ANY selected
  // tag. Resolved via tagBrowseApi per tag (union of results) rather than
  // fetching every article's own tags — cheaper for the common case of a
  // handful of tags selected against a much larger article list.
  useEffect(() => {
    if (selectedTags.size === 0) {
      setTagMatchIds(null);
      return;
    }
    let cancelled = false;
    Promise.all([...selectedTags].map((t) => tagBrowseApi(worldId).entities(t)))
      .then((results) => {
        if (cancelled) return;
        const ids = new Set<string>();
        for (const r of results) for (const a of r.articles) ids.add(a.id);
        setTagMatchIds(ids);
      })
      .catch(onError);
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId, selectedTags]);

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

  // Absolute path, not a bare relative navigate(id): with flat sibling
  // routes ("wiki" and "wiki/:articleId"), React Router's default
  // route-tree-aware relative resolution breaks once already on
  // wiki/:articleId — same class of bug fixed elsewhere in this app.
  function openArticle(id: string) {
    navigate(`/next/worlds/${worldId}/wiki/${id}`);
  }

  function toggleTag(tag: string) {
    setSelectedTags((prev) => {
      const next = new Set(prev);
      if (next.has(tag)) next.delete(tag);
      else next.add(tag);
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

  async function createCategory(name: string, parentId: string | null) {
    try {
      await categoriesApi(worldId).create({ name, parentId });
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function removeCategory(category: Category) {
    try {
      await categoriesApi(worldId).remove(category.id);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  // Fetches full article detail first (the tree only holds ArticleSummary,
  // which has no body) so the update never clobbers body with an omitted
  // field — ArticleRequest.body isn't defaulted server-side to the
  // existing value when left out.
  async function moveArticleToCategory(articleToMove: ArticleSummary, categoryId: string | null) {
    if (articleToMove.categoryId === categoryId) return;
    try {
      const full =
        article && articleToMove.id === article.id ? article : await articlesApi(worldId).get(articleToMove.id);
      const updated = await articlesApi(worldId).update(full.id, {
        title: full.title,
        slug: full.slug,
        template: full.template,
        categoryId,
        parentArticleId: full.parentArticleId,
        body: full.body ?? undefined,
      });
      if (article?.id === updated.id) setArticle(updated);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  const categoryById = useMemo(() => new Map(categories.map((c) => [c.id, c])), [categories]);

  const tagFilteredArticles = tagMatchIds ? articles.filter((a) => tagMatchIds.has(a.id)) : articles;

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        {error && <p className="error">{error}</p>}
        <CategoryTree
          categories={categories}
          entities={tagFilteredArticles}
          entityId={(a) => a.id}
          entityLabel={(a) => a.title}
          entityCategoryId={(a) => a.categoryId ?? null}
          activeEntityId={articleId ?? null}
          onOpenEntity={openArticle}
          onMoveEntity={(a, categoryId) => void moveArticleToCategory(a, categoryId)}
          onCreateCategory={(name, parentId) => void createCategory(name, parentId)}
          onRemoveCategory={(c) => void removeCategory(c)}
          loading={loading}
          searchPlaceholder="Search articles…"
          emptyLabel="No articles found."
        />
        {worldTags.length > 0 && (
          <div className="wiki-tags-section">
            <p className="eyebrow">Tags</p>
            <div className="beat-article-chips">
              {worldTags.map((t) => (
                <button
                  key={t}
                  type="button"
                  className={
                    selectedTags.has(t) ? 'beat-chip tag-chip-link tag-chip-selected' : 'beat-chip tag-chip-link'
                  }
                  onClick={() => toggleTag(t)}
                >
                  {t}
                </button>
              ))}
            </div>
          </div>
        )}
      </aside>
      {article ? (
        <article className="card article-read">
          <div className="article-read-head">
            <div>
              <h3>{article.title}</h3>
              <small className="muted">
                {article.template.toLowerCase()}
                {article.categoryId && ` · ${categoryById.get(article.categoryId)?.name ?? ''}`}
              </small>
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
