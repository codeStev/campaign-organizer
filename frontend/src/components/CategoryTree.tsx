import { ReactNode, useEffect, useMemo, useState } from 'react';
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
import { Input } from './ui/input';
import { Button } from './ui/button';
import { Spinner } from './ui/spinner';
import { PromptDialog } from './PromptDialog';
import { ConfirmDeleteDialog } from './ConfirmDeleteDialog';
import { TruncatedLabel } from './TruncatedLabel';

const ROOT_KEY = '__root__';
const UNCATEGORIZED_KEY = '__none__';

interface CategoryLike {
  id: string;
  parentId?: string | null;
  name: string;
}

interface CategoryTreeProps<TEntity, TCategory extends CategoryLike> {
  categories: TCategory[];
  entities: TEntity[];
  entityId: (entity: TEntity) => string;
  entityLabel: (entity: TEntity) => string;
  entityCategoryId: (entity: TEntity) => string | null;
  activeEntityId: string | null;
  onOpenEntity: (id: string) => void;
  onMoveEntity: (entity: TEntity, categoryId: string | null) => void;
  onCreateCategory: (name: string, parentId: string | null) => void;
  onRemoveCategory: (category: TCategory) => void;
  loading?: boolean;
  searchPlaceholder?: string;
  uncategorizedLabel?: string;
  emptyLabel?: string;
  /** Custom row content (e.g. a kind icon) — defaults to a plain truncated label. */
  renderEntityRow?: (entity: TEntity) => ReactNode;
}

/**
 * Generic category tree sidebar (ADR-0104's original Wiki-only build,
 * extracted so Atlas/Handouts/Tables & Decks/Sheets all get the exact same
 * compact tree, drag-and-drop, search, and deep-link auto-expand behavior —
 * one shared component and CSS class set instead of four hand-rolled
 * copies. Search is the only filter built in here; a caller with extra
 * filters (Wiki's additive tag filter) pre-filters `entities` before
 * passing them in.
 */
export function CategoryTree<TEntity, TCategory extends CategoryLike>({
  categories,
  entities,
  entityId,
  entityLabel,
  entityCategoryId,
  activeEntityId,
  onOpenEntity,
  onMoveEntity,
  onCreateCategory,
  onRemoveCategory,
  loading,
  searchPlaceholder = 'Search…',
  uncategorizedLabel = 'Uncategorised',
  emptyLabel = 'Nothing found.',
  renderEntityRow,
}: CategoryTreeProps<TEntity, TCategory>) {
  const [query, setQuery] = useState('');
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [newCategoryParentId, setNewCategoryParentId] = useState<string | null | undefined>(undefined);
  const [draggingEntity, setDraggingEntity] = useState<TEntity | null>(null);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 4 } }));

  function toggleExpanded(id: string) {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  const categoryById = useMemo(() => new Map(categories.map((c) => [c.id, c])), [categories]);

  const childrenByCategory = useMemo(() => {
    const map = new Map<string, TCategory[]>();
    for (const c of categories) {
      const key = c.parentId ?? ROOT_KEY;
      const bucket = map.get(key) ?? [];
      bucket.push(c);
      map.set(key, bucket);
    }
    return map;
  }, [categories]);

  const queryLc = query.trim().toLowerCase();
  const matchingEntities = queryLc ? entities.filter((e) => entityLabel(e).toLowerCase().includes(queryLc)) : entities;

  const entitiesByCategory = useMemo(() => {
    const map = new Map<string, TEntity[]>();
    for (const e of matchingEntities) {
      const key = entityCategoryId(e) ?? UNCATEGORIZED_KEY;
      const bucket = map.get(key) ?? [];
      bucket.push(e);
      map.set(key, bucket);
    }
    return map;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [matchingEntities]);

  // Auto-expand the active entity's category path — covers deep links
  // landing directly on a detail route with the tree otherwise collapsed,
  // and re-runs whenever the active entity's category changes (e.g.
  // dragged elsewhere).
  useEffect(() => {
    if (!activeEntityId) return;
    const active = entities.find((e) => entityId(e) === activeEntityId);
    if (!active) return;
    const activeCategoryId = entityCategoryId(active);
    if (!activeCategoryId) {
      setExpandedIds((prev) => (prev.has(UNCATEGORIZED_KEY) ? prev : new Set(prev).add(UNCATEGORIZED_KEY)));
      return;
    }
    const toExpand: string[] = [];
    let cur = categoryById.get(activeCategoryId);
    while (cur) {
      toExpand.push(cur.id);
      cur = cur.parentId ? categoryById.get(cur.parentId) : undefined;
    }
    if (toExpand.length === 0) return;
    setExpandedIds((prev) => {
      const next = new Set(prev);
      let changed = false;
      for (const id of toExpand) {
        if (!next.has(id)) {
          next.add(id);
          changed = true;
        }
      }
      return changed ? next : prev;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeEntityId, entities, categoryById]);

  // Search filters the tree in place rather than replacing it with a flat
  // list: a category shows only if it (or a descendant) has a match, and
  // every visible category force-expands so results aren't hidden behind a
  // collapsed toggle.
  const categoryHasMatch = useMemo(() => {
    if (!queryLc) return null;
    const has = new Set<string>();
    function walk(id: string): boolean {
      let found = (entitiesByCategory.get(id) ?? []).length > 0;
      for (const child of childrenByCategory.get(id) ?? []) {
        if (walk(child.id)) found = true;
      }
      if (found) has.add(id);
      return found;
    }
    for (const c of categories) walk(c.id);
    return has;
  }, [queryLc, categories, childrenByCategory, entitiesByCategory]);

  const rootCategories = (childrenByCategory.get(ROOT_KEY) ?? []).filter(
    (c) => !categoryHasMatch || categoryHasMatch.has(c.id),
  );
  const uncategorized = entitiesByCategory.get(UNCATEGORIZED_KEY) ?? [];
  const searching = categoryHasMatch !== null;

  function handleDragStart(event: DragStartEvent) {
    setDraggingEntity((event.active.data.current?.entity as TEntity | undefined) ?? null);
  }

  function handleDragEnd(event: DragEndEvent) {
    setDraggingEntity(null);
    const dropped = event.active.data.current?.entity as TEntity | undefined;
    if (!dropped || !event.over) return;
    const categoryId = event.over.id === UNCATEGORIZED_KEY ? null : String(event.over.id);
    onMoveEntity(dropped, categoryId);
  }

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <Input placeholder={searchPlaceholder} value={query} onChange={(e) => setQuery(e.target.value)} />
      <Button variant="outline" size="sm" onClick={() => setNewCategoryParentId(null)}>
        + New category
      </Button>
      <PromptDialog
        open={newCategoryParentId !== undefined}
        onOpenChange={(open) => !open && setNewCategoryParentId(undefined)}
        title={newCategoryParentId ? 'New sub-category' : 'New category'}
        label="Category name"
        onSubmit={(name) => {
          onCreateCategory(name, newCategoryParentId ?? null);
          setNewCategoryParentId(undefined);
        }}
      />
      <ul className="category-tree">
        {rootCategories.map((c) => (
          <CategoryTreeNode
            key={c.id}
            category={c}
            childrenByCategory={childrenByCategory}
            entitiesByCategory={entitiesByCategory}
            expandedIds={expandedIds}
            onToggleExpand={toggleExpanded}
            activeEntityId={activeEntityId}
            onOpenEntity={onOpenEntity}
            onAddSubcategory={(parentId) => setNewCategoryParentId(parentId)}
            onRemoveCategory={onRemoveCategory}
            forceExpand={searching}
            entityId={entityId}
            renderEntityRow={renderEntityRow ?? defaultEntityRow(entityLabel)}
          />
        ))}
        <CategoryLeaf
          dropId={UNCATEGORIZED_KEY}
          label={uncategorizedLabel}
          entities={uncategorized}
          expanded={searching || expandedIds.has(UNCATEGORIZED_KEY)}
          onToggleExpand={() => toggleExpanded(UNCATEGORIZED_KEY)}
          activeEntityId={activeEntityId}
          onOpenEntity={onOpenEntity}
          entityId={entityId}
          renderEntityRow={renderEntityRow ?? defaultEntityRow(entityLabel)}
        />
        {loading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!loading && rootCategories.length === 0 && uncategorized.length === 0 && (
          <li className="muted">{emptyLabel}</li>
        )}
      </ul>
      <DragOverlay dropAnimation={null}>
        {draggingEntity && <div className="category-tree-drag-overlay">{entityLabel(draggingEntity)}</div>}
      </DragOverlay>
    </DndContext>
  );
}

function defaultEntityRow<TEntity>(entityLabel: (e: TEntity) => string) {
  return (entity: TEntity) => {
    const label = entityLabel(entity);
    return <TruncatedLabel label={label}>{label}</TruncatedLabel>;
  };
}

interface CategoryTreeNodeProps<TEntity, TCategory extends CategoryLike> {
  category: TCategory;
  childrenByCategory: Map<string, TCategory[]>;
  entitiesByCategory: Map<string, TEntity[]>;
  expandedIds: Set<string>;
  onToggleExpand: (id: string) => void;
  activeEntityId: string | null;
  onOpenEntity: (id: string) => void;
  onAddSubcategory: (parentId: string) => void;
  onRemoveCategory: (category: TCategory) => void;
  forceExpand: boolean;
  entityId: (entity: TEntity) => string;
  renderEntityRow: (entity: TEntity) => ReactNode;
}

function CategoryTreeNode<TEntity, TCategory extends CategoryLike>({
  category,
  childrenByCategory,
  entitiesByCategory,
  expandedIds,
  onToggleExpand,
  activeEntityId,
  onOpenEntity,
  onAddSubcategory,
  onRemoveCategory,
  forceExpand,
  entityId,
  renderEntityRow,
}: CategoryTreeNodeProps<TEntity, TCategory>) {
  const subCategories = childrenByCategory.get(category.id) ?? [];
  const directEntities = entitiesByCategory.get(category.id) ?? [];
  const hasContent = subCategories.length > 0 || directEntities.length > 0;
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
        {directEntities.length > 0 && <span className="muted category-tree-count">{directEntities.length}</span>}
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
          description={`This deletes "${category.name}". Its contents are kept, just uncategorised.`}
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
              entitiesByCategory={entitiesByCategory}
              expandedIds={expandedIds}
              onToggleExpand={onToggleExpand}
              activeEntityId={activeEntityId}
              onOpenEntity={onOpenEntity}
              onAddSubcategory={onAddSubcategory}
              onRemoveCategory={onRemoveCategory}
              forceExpand={forceExpand}
              entityId={entityId}
              renderEntityRow={renderEntityRow}
            />
          ))}
          {directEntities.map((e) => (
            <CategoryTreeEntityRow
              key={entityId(e)}
              entity={e}
              active={entityId(e) === activeEntityId}
              onOpen={onOpenEntity}
              entityId={entityId}
              render={renderEntityRow}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

function CategoryLeaf<TEntity>({
  dropId,
  label,
  entities,
  expanded,
  onToggleExpand,
  activeEntityId,
  onOpenEntity,
  entityId,
  renderEntityRow,
}: {
  dropId: string;
  label: string;
  entities: TEntity[];
  expanded: boolean;
  onToggleExpand: () => void;
  activeEntityId: string | null;
  onOpenEntity: (id: string) => void;
  entityId: (entity: TEntity) => string;
  renderEntityRow: (entity: TEntity) => ReactNode;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: dropId });
  return (
    <li>
      <div ref={setNodeRef} className={isOver ? 'category-tree-row drop-over' : 'category-tree-row'}>
        {entities.length > 0 ? (
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
        {entities.length > 0 && <span className="muted category-tree-count">{entities.length}</span>}
      </div>
      {expanded && entities.length > 0 && (
        <ul className="article-list-nested">
          {entities.map((e) => (
            <CategoryTreeEntityRow
              key={entityId(e)}
              entity={e}
              active={entityId(e) === activeEntityId}
              onOpen={onOpenEntity}
              entityId={entityId}
              render={renderEntityRow}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

function CategoryTreeEntityRow<TEntity>({
  entity,
  active,
  onOpen,
  entityId,
  render,
}: {
  entity: TEntity;
  active: boolean;
  onOpen: (id: string) => void;
  entityId: (entity: TEntity) => string;
  render: (entity: TEntity) => ReactNode;
}) {
  const id = entityId(entity);
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id,
    data: { entity },
  });
  return (
    <li>
      {/* A <div role="button"> rather than a real <button> — some callers
          (e.g. Handouts' reorder/reveal controls) need real nested buttons
          in their row content, which a <button> can't legally contain. */}
      <div
        ref={setNodeRef}
        {...listeners}
        {...attributes}
        role="button"
        tabIndex={0}
        className={active ? 'category-tree-article active' : 'category-tree-article'}
        style={isDragging ? { opacity: 0.4 } : undefined}
        onClick={() => onOpen(id)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onOpen(id);
          }
        }}
      >
        {render(entity)}
      </div>
    </li>
  );
}
