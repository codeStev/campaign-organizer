import { MouseEvent, useEffect, useMemo, useState } from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';
import {
  articlesApi,
  articleTagsApi,
  categoriesApi,
  Article,
  ArticleSummary,
  Category,
  ApiError,
} from '../api/client';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Spinner } from '../components/ui/spinner';
import { TagList } from '../components/TagInput';
import { PromptDialog } from '../components/PromptDialog';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { TruncatedLabel } from '../components/TruncatedLabel';

const NONE_VALUE = '__none__';
const ROOT_KEY = '__root__';
const UNCATEGORIZED_KEY = '__none__';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

/** "Characters / Reckoners" — the nearest thing to a breadcrumb for a Select option. */
function categoryPath(category: Category, byId: Map<string, Category>): string {
  const parts: string[] = [category.name];
  let cur = category;
  while (cur.parentId) {
    const parent = byId.get(cur.parentId);
    if (!parent) break;
    parts.unshift(parent.name);
    cur = parent;
  }
  return parts.join(' / ');
}

/**
 * Wiki (docs/ui-overhaul-plan.md Phase 2, restructured in the Phase 5c
 * mockup-fidelity pass): a Category tree — matching the mockup's "BY
 * CATEGORY" sidebar — instead of ADR-0080's parentArticleId tree. Categories
 * had full CRUD in the backend/OpenAPI contract already but were never
 * wired into any UI (old or new); this is the first screen to use them.
 * Article body editing still stays on the old UI's richer editor (draft
 * handling, revisions, GM-only block, AI draft); category assignment is a
 * narrow, additive exception since there was previously no way at all to
 * set an article's category.
 */
export function NextWikiPage({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { articleId } = useParams<{ articleId: string }>();
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [article, setArticle] = useState<Article | null>(null);
  const [tags, setTags] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [newCategoryParentId, setNewCategoryParentId] = useState<string | null | undefined>(undefined);

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId]);

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

  async function createCategory(name: string) {
    try {
      await categoriesApi(worldId).create({ name, parentId: newCategoryParentId ?? null });
      await refresh();
    } catch (err) {
      onError(err);
    } finally {
      setNewCategoryParentId(undefined);
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

  async function setArticleCategory(categoryId: string | null) {
    if (!article) return;
    try {
      const updated = await articlesApi(worldId).update(article.id, {
        title: article.title,
        slug: article.slug,
        template: article.template,
        categoryId,
        parentArticleId: article.parentArticleId,
        body: article.body ?? undefined,
      });
      setArticle(updated);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  const categoryById = useMemo(() => new Map(categories.map((c) => [c.id, c])), [categories]);

  const childrenByCategory = useMemo(() => {
    const map = new Map<string, Category[]>();
    for (const c of categories) {
      const key = c.parentId ?? ROOT_KEY;
      const bucket = map.get(key) ?? [];
      bucket.push(c);
      map.set(key, bucket);
    }
    return map;
  }, [categories]);

  const queryLc = query.trim().toLowerCase();
  const matchingArticles = queryLc ? articles.filter((a) => a.title.toLowerCase().includes(queryLc)) : articles;

  const articlesByCategory = useMemo(() => {
    const map = new Map<string, ArticleSummary[]>();
    for (const a of matchingArticles) {
      const key = a.categoryId ?? UNCATEGORIZED_KEY;
      const bucket = map.get(key) ?? [];
      bucket.push(a);
      map.set(key, bucket);
    }
    return map;
  }, [matchingArticles]);

  // Search filters the tree in place rather than replacing it with a flat
  // list: a category shows only if it (or a descendant) has a match, and
  // every visible category force-expands so results aren't hidden behind a
  // collapsed toggle.
  const categoryHasMatch = useMemo(() => {
    if (!queryLc) return null;
    const has = new Set<string>();
    function walk(id: string): boolean {
      let found = (articlesByCategory.get(id) ?? []).length > 0;
      for (const child of childrenByCategory.get(id) ?? []) {
        if (walk(child.id)) found = true;
      }
      if (found) has.add(id);
      return found;
    }
    for (const c of categories) walk(c.id);
    return has;
  }, [queryLc, categories, childrenByCategory, articlesByCategory]);

  const rootCategories = (childrenByCategory.get(ROOT_KEY) ?? []).filter(
    (c) => !categoryHasMatch || categoryHasMatch.has(c.id),
  );
  const uncategorized = articlesByCategory.get(UNCATEGORIZED_KEY) ?? [];
  const searching = categoryHasMatch !== null;

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <Input placeholder="Search articles…" value={query} onChange={(e) => setQuery(e.target.value)} />
        {error && <p className="error">{error}</p>}
        <Button variant="outline" size="sm" onClick={() => setNewCategoryParentId(null)}>
          + New category
        </Button>
        <PromptDialog
          open={newCategoryParentId !== undefined}
          onOpenChange={(open) => !open && setNewCategoryParentId(undefined)}
          title={newCategoryParentId ? 'New sub-category' : 'New category'}
          label="Category name"
          onSubmit={(name) => void createCategory(name)}
        />
        <ul className="article-list article-list-scroll category-tree">
          {rootCategories.map((c) => (
            <CategoryTreeNode
              key={c.id}
              category={c}
              childrenByCategory={childrenByCategory}
              articlesByCategory={articlesByCategory}
              expandedIds={expandedIds}
              onToggleExpand={toggleExpanded}
              activeArticleId={articleId ?? null}
              onOpenArticle={openArticle}
              onAddSubcategory={(parentId) => setNewCategoryParentId(parentId)}
              onRemoveCategory={removeCategory}
              forceExpand={searching}
            />
          ))}
          <CategoryLeaf
            label="Uncategorised"
            articles={uncategorized}
            expanded={searching || expandedIds.has(UNCATEGORIZED_KEY)}
            onToggleExpand={() => toggleExpanded(UNCATEGORIZED_KEY)}
            activeArticleId={articleId ?? null}
            onOpenArticle={openArticle}
          />
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && rootCategories.length === 0 && uncategorized.length === 0 && (
            <li className="muted">No articles found.</li>
          )}
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
          <label className="wiki-category-picker">
            Category
            <Select
              value={article.categoryId ?? NONE_VALUE}
              onValueChange={(v) => void setArticleCategory(v === NONE_VALUE ? null : v)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE_VALUE}>— uncategorised —</SelectItem>
                {categories.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {categoryPath(c, categoryById)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </label>
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

interface CategoryTreeNodeProps {
  category: Category;
  childrenByCategory: Map<string, Category[]>;
  articlesByCategory: Map<string, ArticleSummary[]>;
  expandedIds: Set<string>;
  onToggleExpand: (id: string) => void;
  activeArticleId: string | null;
  onOpenArticle: (id: string) => void;
  onAddSubcategory: (parentId: string) => void;
  onRemoveCategory: (category: Category) => void;
  forceExpand: boolean;
}

function CategoryTreeNode({
  category,
  childrenByCategory,
  articlesByCategory,
  expandedIds,
  onToggleExpand,
  activeArticleId,
  onOpenArticle,
  onAddSubcategory,
  onRemoveCategory,
  forceExpand,
}: CategoryTreeNodeProps) {
  const subCategories = childrenByCategory.get(category.id) ?? [];
  const directArticles = articlesByCategory.get(category.id) ?? [];
  const hasContent = subCategories.length > 0 || directArticles.length > 0;
  const expanded = forceExpand || expandedIds.has(category.id);

  return (
    <li>
      <div className="category-tree-row">
        {hasContent ? (
          <button
            type="button"
            className="article-tree-toggle"
            onClick={() => onToggleExpand(category.id)}
            title={expanded ? 'Collapse' : 'Expand'}
          >
            {expanded ? '▾' : '▸'}
          </button>
        ) : (
          <span className="article-tree-toggle-spacer" />
        )}
        <span className="category-tree-label">
          <TruncatedLabel label={category.name}>{category.name}</TruncatedLabel>
        </span>
        {directArticles.length > 0 && <span className="muted category-tree-count">{directArticles.length}</span>}
        <button
          type="button"
          className="category-tree-action"
          title="Add sub-category"
          onClick={() => onAddSubcategory(category.id)}
        >
          +
        </button>
        <ConfirmDeleteDialog
          trigger={
            <button
              type="button"
              className="category-tree-action category-tree-action-destructive"
              title="Delete category"
            >
              ✕
            </button>
          }
          title="Delete category?"
          description={`This deletes "${category.name}". Its articles are kept, just uncategorised.`}
          onConfirm={() => onRemoveCategory(category)}
        />
      </div>
      {expanded && hasContent && (
        <ul className="article-list-nested">
          {subCategories.map((c) => (
            <CategoryTreeNode
              key={c.id}
              category={c}
              childrenByCategory={childrenByCategory}
              articlesByCategory={articlesByCategory}
              expandedIds={expandedIds}
              onToggleExpand={onToggleExpand}
              activeArticleId={activeArticleId}
              onOpenArticle={onOpenArticle}
              onAddSubcategory={onAddSubcategory}
              onRemoveCategory={onRemoveCategory}
              forceExpand={forceExpand}
            />
          ))}
          {directArticles.map((a) => (
            <li key={a.id}>
              <button
                className={a.id === activeArticleId ? 'article-link active' : 'article-link'}
                onClick={() => onOpenArticle(a.id)}
              >
                <TruncatedLabel label={a.title}>{a.title}</TruncatedLabel>
                <small className="muted">{a.template.toLowerCase()}</small>
              </button>
            </li>
          ))}
        </ul>
      )}
    </li>
  );
}

function CategoryLeaf({
  label,
  articles,
  expanded,
  onToggleExpand,
  activeArticleId,
  onOpenArticle,
}: {
  label: string;
  articles: ArticleSummary[];
  expanded: boolean;
  onToggleExpand: () => void;
  activeArticleId: string | null;
  onOpenArticle: (id: string) => void;
}) {
  if (articles.length === 0) return null;
  return (
    <li>
      <div className="category-tree-row">
        <button
          type="button"
          className="article-tree-toggle"
          onClick={onToggleExpand}
          title={expanded ? 'Collapse' : 'Expand'}
        >
          {expanded ? '▾' : '▸'}
        </button>
        <span className="category-tree-label muted">{label}</span>
        <span className="muted category-tree-count">{articles.length}</span>
      </div>
      {expanded && (
        <ul className="article-list-nested">
          {articles.map((a) => (
            <li key={a.id}>
              <button
                className={a.id === activeArticleId ? 'article-link active' : 'article-link'}
                onClick={() => onOpenArticle(a.id)}
              >
                <TruncatedLabel label={a.title}>{a.title}</TruncatedLabel>
                <small className="muted">{a.template.toLowerCase()}</small>
              </button>
            </li>
          ))}
        </ul>
      )}
    </li>
  );
}
