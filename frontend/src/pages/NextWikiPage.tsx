import { MouseEvent, useEffect, useMemo, useState } from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  articlesApi,
  articleTagsApi,
  categoriesApi,
  worldTagsApi,
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
import { TruncatedLabel } from '../components/TruncatedLabel';

const ROOT_KEY = '__root__';
const UNCATEGORIZED_KEY = '__none__';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
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
 * set an article's category — done by dragging an article onto a category
 * row (@dnd-kit/core), replacing an earlier Select-based picker on the
 * read pane entirely.
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
  const [worldTags, setWorldTags] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [newCategoryParentId, setNewCategoryParentId] = useState<string | null | undefined>(undefined);
  const [draggingArticle, setDraggingArticle] = useState<ArticleSummary | null>(null);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 4 } }));

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

  function openTag(tag: string) {
    navigate(`/next/worlds/${worldId}/tags/${encodeURIComponent(tag)}`);
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

  function handleDragStart(event: DragStartEvent) {
    setDraggingArticle((event.active.data.current?.article as ArticleSummary | undefined) ?? null);
  }

  function handleDragEnd(event: DragEndEvent) {
    setDraggingArticle(null);
    const dropped = event.active.data.current?.article as ArticleSummary | undefined;
    if (!dropped || !event.over) return;
    const categoryId = event.over.id === UNCATEGORIZED_KEY ? null : String(event.over.id);
    void moveArticleToCategory(dropped, categoryId);
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
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
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
          <ul className="category-tree">
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
              dropId={UNCATEGORIZED_KEY}
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
          {worldTags.length > 0 && (
            <div className="wiki-tags-section">
              <p className="eyebrow">Tags</p>
              <div className="beat-article-chips">
                {worldTags.map((t) => (
                  <button key={t} type="button" className="beat-chip tag-chip-link" onClick={() => openTag(t)}>
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
      <DragOverlay dropAnimation={null}>
        {draggingArticle && <div className="category-tree-drag-overlay">{draggingArticle.title}</div>}
      </DragOverlay>
    </DndContext>
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
  const { setNodeRef, isOver } = useDroppable({ id: category.id });

  return (
    <li>
      <div ref={setNodeRef} className={isOver ? 'category-tree-row drop-over' : 'category-tree-row'}>
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
            <CategoryTreeArticleRow key={a.id} article={a} active={a.id === activeArticleId} onOpen={onOpenArticle} />
          ))}
        </ul>
      )}
    </li>
  );
}

function CategoryLeaf({
  dropId,
  label,
  articles,
  expanded,
  onToggleExpand,
  activeArticleId,
  onOpenArticle,
}: {
  dropId: string;
  label: string;
  articles: ArticleSummary[];
  expanded: boolean;
  onToggleExpand: () => void;
  activeArticleId: string | null;
  onOpenArticle: (id: string) => void;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: dropId });
  return (
    <li>
      <div ref={setNodeRef} className={isOver ? 'category-tree-row drop-over' : 'category-tree-row'}>
        {articles.length > 0 ? (
          <button
            type="button"
            className="article-tree-toggle"
            onClick={onToggleExpand}
            title={expanded ? 'Collapse' : 'Expand'}
          >
            {expanded ? '▾' : '▸'}
          </button>
        ) : (
          <span className="article-tree-toggle-spacer" />
        )}
        <span className="category-tree-label muted">{label}</span>
        {articles.length > 0 && <span className="muted category-tree-count">{articles.length}</span>}
      </div>
      {expanded && articles.length > 0 && (
        <ul className="article-list-nested">
          {articles.map((a) => (
            <CategoryTreeArticleRow key={a.id} article={a} active={a.id === activeArticleId} onOpen={onOpenArticle} />
          ))}
        </ul>
      )}
    </li>
  );
}

function CategoryTreeArticleRow({
  article,
  active,
  onOpen,
}: {
  article: ArticleSummary;
  active: boolean;
  onOpen: (id: string) => void;
}) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: article.id,
    data: { article },
  });
  return (
    <li>
      <button
        ref={setNodeRef}
        {...listeners}
        {...attributes}
        className={active ? 'category-tree-article active' : 'category-tree-article'}
        style={isDragging ? { opacity: 0.4 } : undefined}
        onClick={() => onOpen(article.id)}
      >
        <TruncatedLabel label={article.title}>{article.title}</TruncatedLabel>
      </button>
    </li>
  );
}
