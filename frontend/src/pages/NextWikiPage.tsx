import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  articlesApi,
  categoriesApi,
  worldTagsApi,
  tagBrowseApi,
  ArticleSummary,
  Category,
  ApiError,
} from '../api/client';
import { CategoryTree } from '../components/CategoryTree';
import { ArticleEditor } from '../components/ArticleEditor';

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
      const full = await articlesApi(worldId).get(articleToMove.id);
      await articlesApi(worldId).update(full.id, {
        title: full.title,
        slug: full.slug,
        template: full.template,
        categoryId,
        parentArticleId: full.parentArticleId,
        body: full.body ?? undefined,
      });
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  // Context-menu "Delete" on a tree row — the article may not be open, so
  // this doesn't go through ArticleEditor's own delete flow at all.
  async function deleteArticle(article: ArticleSummary) {
    try {
      await articlesApi(worldId).remove(article.id);
      if (articleId === article.id) navigate(`/next/worlds/${worldId}/wiki`);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

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
          onDeleteEntity={(a) => void deleteArticle(a)}
          newEntityActions={[
            {
              label: 'New article',
              onCreate: (categoryId) =>
                navigate(`/next/worlds/${worldId}/wiki/new${categoryId ? `?category=${categoryId}` : ''}`),
            },
          ]}
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
      <div className="wiki-main">
        <ArticleEditor
          worldId={worldId}
          articleId={articleId ?? null}
          articles={articles}
          categories={categories}
          onOpenArticle={openArticle}
          onChanged={() => void refresh()}
          onAuthExpired={onAuthExpired}
        />
      </div>
    </div>
  );
}
