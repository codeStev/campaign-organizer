import { MouseEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  articlesApi,
  articleRevisionsApi,
  articleTagsApi,
  templatesApi,
  mediaApi,
  aiApi,
  ArticleSummary,
  ArticleRevision,
  ArticleTemplate,
  ArticleTemplateInfo,
  ARTICLE_TEMPLATES,
  Category,
  Usage,
  ApiError,
} from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from './ConfirmDeleteDialog';
import { MarkdownEditor } from './MarkdownEditor';
import { TagInput, TagList } from './TagInput';
import { RevisionDiff } from './RevisionDiff';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so a meaningfully persistent "none" state goes through this sentinel.
const NONE_VALUE = '__none__';

/** True when the Markdown has no meaningful text content. */
function isEmptyMarkdown(markdown: string): boolean {
  return !markdown || !markdown.trim().length;
}

function outlineMarkdown(info: ArticleTemplateInfo): string {
  return info.sections.map((s) => `## ${s.heading}\n\n`).join('');
}

/** All ids reachable from rootId through childrenByParent, any depth. */
function descendantIds(rootId: string, childrenByParent: Map<string, ArticleSummary[]>): Set<string> {
  const result = new Set<string>();
  const stack = [...(childrenByParent.get(rootId) ?? [])];
  while (stack.length) {
    const next = stack.pop()!;
    if (result.has(next.id)) continue;
    result.add(next.id);
    stack.push(...(childrenByParent.get(next.id) ?? []));
  }
  return result;
}

function groupByParent(list: ArticleSummary[]): Map<string, ArticleSummary[]> {
  const map = new Map<string, ArticleSummary[]>();
  for (const a of list) {
    if (!a.parentArticleId) continue;
    const bucket = map.get(a.parentArticleId) ?? [];
    bucket.push(a);
    map.set(a.parentArticleId, bucket);
  }
  return map;
}

function templateLabel(t: ArticleTemplate) {
  return t.charAt(0) + t.slice(1).toLowerCase();
}

const USAGE_LABELS: Record<Usage['type'], string> = {
  BEAT: 'Beat',
  MAP_PIN: 'Map',
  TIMELINE_EVENT: 'Timeline',
  RELATIONSHIP: 'Relationship',
  CHARACTER_SHEET: 'Sheet',
  STATBLOCK: 'Statblock',
  ARTICLE_LINK: 'Wiki-link',
  CHILD_ARTICLE: 'Sub-article',
};

interface Draft {
  id: string | null;
  categoryId: string | null;
  title: string;
  template: ArticleTemplate;
  body: string;
  parentArticleId: string | null;
  tags: string[];
}

const EMPTY_DRAFT: Draft = {
  id: null,
  categoryId: null,
  title: '',
  template: 'GENERIC',
  body: '',
  parentArticleId: null,
  tags: [],
};

interface Props {
  worldId: string;
  /** null = nothing open; 'new' = create mode; otherwise an existing article id. */
  articleId: string | null;
  /** Full, unfiltered article list — for the parent-article picker and breadcrumb. */
  articles: ArticleSummary[];
  categories: Category[];
  onOpenArticle: (id: string) => void;
  /** The article list (and/or categories) changed — parent should refetch. */
  onChanged: () => void;
  onAuthExpired: () => void;
}

/**
 * Full article body editor, ported from the old UI's `WorldView.tsx` into
 * `/next` (ADR-0105 Step 6) — title/template/parent/tags/body, AI draft,
 * revision history + diff, and the "Used by" usages panel. Category
 * assignment itself stays out of this form: it's handled by drag-and-drop on
 * the sidebar's `<CategoryTree>`, exactly as before this component existed.
 */
export function ArticleEditor({ worldId, articleId, articles, categories, onOpenArticle, onChanged, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const api = useMemo(() => articlesApi(worldId), [worldId]);
  const media = useMemo(() => mediaApi(worldId), [worldId]);
  const ai = useMemo(() => aiApi(worldId), [worldId]);

  const [draft, setDraft] = useState<Draft | null>(null);
  const [previewHtml, setPreviewHtml] = useState('');
  const [templates, setTemplates] = useState<ArticleTemplateInfo[]>([]);
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  const [revisions, setRevisions] = useState<ArticleRevision[] | null>(null);
  const [diffPick, setDiffPick] = useState<string[]>([]);
  const [usages, setUsages] = useState<Usage[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [parentTitle, setParentTitle] = useState<string | null>(null);

  const categoryById = useMemo(() => new Map(categories.map((c) => [c.id, c])), [categories]);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) {
        onAuthExpired();
        return;
      }
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  useEffect(() => {
    templatesApi.list().then(setTemplates).catch(handleError);
  }, [handleError]);

  const allChildrenByParent = useMemo(() => groupByParent(articles), [articles]);
  const excludedParentIds = draft?.id
    ? new Set([draft.id, ...descendantIds(draft.id, allChildrenByParent)])
    : new Set<string>();
  const parentCandidates = articles.filter((a) => !excludedParentIds.has(a.id));

  function newDraft() {
    // "+ New article" from a category's tree menu passes ?category=<id> so
    // the article is pre-assigned instead of landing Uncategorised.
    setDraft({ ...EMPTY_DRAFT, categoryId: searchParams.get('category') || null });
    setPreviewHtml('');
    setMode('edit');
    setRevisions(null);
    setDiffPick([]);
    setUsages(null);
  }

  const loadArticle = useCallback(
    async (id: string) => {
      try {
        const [article, articleTags] = await Promise.all([api.get(id), articleTagsApi(worldId, id).get()]);
        setDraft({
          id: article.id,
          categoryId: article.categoryId ?? null,
          title: article.title,
          template: article.template,
          body: article.body ?? '',
          parentArticleId: article.parentArticleId ?? null,
          tags: articleTags.tags,
        });
        setPreviewHtml(article.bodyHtml ?? '');
        setMode('read');
        setRevisions(null);
        setDiffPick([]);
        setUsages(null);
      } catch (err) {
        handleError(err);
      }
    },
    [api, handleError, worldId],
  );

  useEffect(() => {
    if (!articleId) {
      setDraft(null);
      return;
    }
    if (articleId === 'new') {
      newDraft();
      return;
    }
    if (articleId === draft?.id) return;
    void loadArticle(articleId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [articleId]);

  // Breadcrumb title for the open article's parent: reuse the already-loaded
  // list when possible, otherwise a single lightweight fetch for just the title.
  useEffect(() => {
    const parentId = draft?.parentArticleId;
    if (!parentId) {
      setParentTitle(null);
      return;
    }
    const local = articles.find((a) => a.id === parentId);
    if (local) {
      setParentTitle(local.title);
      return;
    }
    let active = true;
    api
      .get(parentId)
      .then((a) => {
        if (active) setParentTitle(a.title);
      })
      .catch(() => {
        if (active) setParentTitle(null);
      });
    return () => {
      active = false;
    };
  }, [draft?.parentArticleId, articles, api]);

  // Changing the template on an empty draft seeds an outline (ADR-0015).
  function selectTemplate(template: ArticleTemplate) {
    setDraft((current) => {
      if (!current) return current;
      const next = { ...current, template };
      if (isEmptyMarkdown(current.body)) {
        const info = templates.find((t) => t.template === template);
        if (info) next.body = outlineMarkdown(info);
      }
      return next;
    });
  }

  async function toggleUsages() {
    if (usages !== null) {
      setUsages(null);
      return;
    }
    if (!draft?.id) return;
    try {
      const res = await api.usages(draft.id);
      setUsages(res.usages);
    } catch (err) {
      handleError(err);
    }
  }

  async function toggleHistory() {
    if (revisions !== null) {
      setRevisions(null);
      setDiffPick([]);
      return;
    }
    if (!draft?.id) return;
    try {
      setRevisions(await articleRevisionsApi(worldId, draft.id).list());
    } catch (err) {
      handleError(err);
    }
  }

  function versionFor(key: string | null) {
    if (!draft) return null;
    if (key === 'current') {
      return { label: 'Current', title: draft.title, body: draft.body };
    }
    const r = revisions?.find((rev) => rev.id === key);
    return r ? { label: new Date(r.createdAt).toLocaleString(), title: r.title, body: r.body ?? '' } : null;
  }

  function versionTime(key: string): number {
    if (key === 'current') return Infinity;
    const r = revisions?.find((rev) => rev.id === key);
    return r ? new Date(r.createdAt).getTime() : 0;
  }

  function toggleDiffPick(id: string) {
    setDiffPick((prev) => {
      if (prev.includes(id)) return prev.filter((x) => x !== id);
      if (prev.length >= 2) return [prev[1], id];
      return [...prev, id];
    });
  }

  const diffPair = diffPick.length === 2 ? [...diffPick].sort((x, y) => versionTime(x) - versionTime(y)) : null;

  async function restoreRevision(revisionId: string) {
    if (!draft?.id) return;
    try {
      const restored = await articleRevisionsApi(worldId, draft.id).restore(revisionId);
      setDraft((d) =>
        d
          ? {
              ...d,
              id: restored.id,
              title: restored.title,
              template: restored.template,
              body: restored.body ?? '',
              parentArticleId: restored.parentArticleId ?? null,
            }
          : d,
      );
      setPreviewHtml(restored.bodyHtml ?? '');
      setRevisions(null);
      onChanged();
    } catch (err) {
      handleError(err);
    }
  }

  function handlePreviewClick(event: MouseEvent<HTMLDivElement>) {
    const link = (event.target as HTMLElement).closest('.wiki-link');
    if (link) {
      event.preventDefault();
      const id = link.getAttribute('data-article-id');
      if (id) onOpenArticle(id);
    }
  }

  async function handleSave() {
    if (!draft) return;
    setError(null);
    const wasNew = !draft.id;
    const payload = {
      title: draft.title,
      template: draft.template,
      body: draft.body,
      parentArticleId: draft.parentArticleId,
      categoryId: draft.categoryId,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, payload) : await api.create(payload);
      const savedTags = await articleTagsApi(worldId, saved.id).set(draft.tags);
      setDraft({
        id: saved.id,
        categoryId: saved.categoryId ?? null,
        title: saved.title,
        template: saved.template,
        body: saved.body ?? '',
        parentArticleId: saved.parentArticleId ?? null,
        tags: savedTags.tags,
      });
      setPreviewHtml(saved.bodyHtml ?? '');
      setMode('read');
      onChanged();
      if (wasNew) navigate(`/next/worlds/${worldId}/wiki/${saved.id}`);
      toast.success(`Article "${saved.title}" saved`);
    } catch (err) {
      handleError(err);
    }
  }

  async function handleDelete() {
    if (!draft?.id) return;
    try {
      await api.remove(draft.id);
      setDraft(null);
      setPreviewHtml('');
      onChanged();
      navigate(`/next/worlds/${worldId}/wiki`);
    } catch (err) {
      handleError(err);
    }
  }

  if (!draft) {
    return <p className="muted">Select an article from the list, or create a new one.</p>;
  }

  if (mode === 'edit') {
    return (
      <>
        {error && <p className="error">{error}</p>}
        <div className="wiki-editor card">
          <Input
            className="title-input"
            placeholder="Article title"
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
            required
          />
          <Select value={draft.template} onValueChange={(v) => selectTemplate(v as ArticleTemplate)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {ARTICLE_TEMPLATES.map((t) => (
                <SelectItem key={t} value={t}>
                  {templateLabel(t)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={draft.parentArticleId ?? NONE_VALUE}
            onValueChange={(v) => {
              if (v === '') return;
              setDraft({ ...draft, parentArticleId: v === NONE_VALUE ? null : v });
            }}
          >
            <SelectTrigger title="Nest this article under a parent (structure, independent of type)">
              <SelectValue placeholder="No parent article" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE_VALUE}>No parent article</SelectItem>
              {parentCandidates.map((a) => (
                <SelectItem key={a.id} value={a.id}>
                  {a.title}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <TagInput worldId={worldId} value={draft.tags} onChange={(tags) => setDraft({ ...draft, tags })} />
          <MarkdownEditor
            value={draft.body}
            onChange={(body) => setDraft({ ...draft, body })}
            onUploadImage={async (file) => (await media.upload(file)).url}
            articleTemplate={draft.template}
            onArticleTemplateChange={selectTemplate}
            onAiDraft={async (instructions, existingContent, level, template) =>
              (await ai.draftArticleText(instructions, existingContent, level, template)).text
            }
          />
          <div className="editor-actions">
            <Button type="button" onClick={handleSave} disabled={draft.title.length === 0}>
              {draft.id ? 'Save changes' : 'Create article'}
            </Button>
            {draft.id && (
              <Button type="button" variant="link" onClick={() => void loadArticle(draft.id!)}>
                Cancel
              </Button>
            )}
            {draft.id && (
              <ConfirmDeleteDialog
                trigger={
                  <Button type="button" variant="link" className="text-destructive hover:text-destructive">
                    Delete
                  </Button>
                }
                title="Delete article?"
                description={`This permanently deletes "${draft.title}" and cannot be undone.`}
                onConfirm={handleDelete}
              />
            )}
          </div>
          <p className="muted hint">
            Tip: link to another article with <code>[[Title]]</code> or <code>[[Title|label]]</code>.
          </p>
        </div>

        {previewHtml && (
          <div className="card preview">
            <h3 className="muted">Preview</h3>
            {/* eslint-disable-next-line react/no-danger */}
            <div className="preview-body" onClick={handlePreviewClick} dangerouslySetInnerHTML={{ __html: previewHtml }} />
          </div>
        )}
      </>
    );
  }

  return (
    <article className="card article-read">
      {error && <p className="error">{error}</p>}
      <div className="article-read-head">
        <div>
          {draft.parentArticleId && parentTitle && (
            <p className="muted breadcrumb">
              Part of{' '}
              <button type="button" className="breadcrumb-link" onClick={() => onOpenArticle(draft.parentArticleId!)}>
                {parentTitle}
              </button>
            </p>
          )}
          <h2>{draft.title}</h2>
          <small className="muted">
            {templateLabel(draft.template)}
            {draft.categoryId && ` · ${categoryById.get(draft.categoryId)?.name ?? ''}`}
          </small>
        </div>
        <div className="editor-actions">
          <Button type="button" onClick={() => setMode('edit')}>
            Edit
          </Button>
          <Button type="button" variant="link" onClick={toggleUsages}>
            Used by
          </Button>
          <Button type="button" variant="link" onClick={toggleHistory}>
            History
          </Button>
          <ConfirmDeleteDialog
            trigger={
              <Button type="button" variant="link" className="text-destructive hover:text-destructive">
                Delete
              </Button>
            }
            title="Delete article?"
            description={`This permanently deletes "${draft.title}" and cannot be undone.`}
            onConfirm={handleDelete}
          />
        </div>
      </div>
      <TagList worldId={worldId} tags={draft.tags} />

      {usages !== null && (
        <div className="usages card">
          <strong className="muted">Used by</strong>
          {usages.length === 0 && <p className="muted">Not referenced anywhere yet.</p>}
          <ul className="usage-list">
            {usages.map((u, i) => {
              const clickable = (u.type === 'ARTICLE_LINK' || u.type === 'RELATIONSHIP' || u.type === 'CHILD_ARTICLE') && u.targetId;
              return (
                <li key={i} className="usage-item">
                  <span className="usage-type">{USAGE_LABELS[u.type]}</span>
                  {clickable ? (
                    <Button variant="link" className="usage-label" onClick={() => onOpenArticle(u.targetId!)}>
                      {u.label}
                    </Button>
                  ) : (
                    <span className="usage-label">{u.label}</span>
                  )}
                  {u.campaignName && <span className="usage-campaign">{u.campaignName}</span>}
                </li>
              );
            })}
          </ul>
        </div>
      )}

      {revisions !== null && (
        <div className="revisions card">
          <strong className="muted">Revision history</strong>
          {revisions.length === 0 && <p className="muted">No prior versions yet.</p>}
          {revisions.length > 0 && (
            <>
              <p className="muted hint">Tick two versions to compare — the newer one is always the "+" side.</p>
              <ul className="revision-list">
                {[{ id: 'current', label: 'Current version' }, ...revisions.map((r) => ({ id: r.id, label: new Date(r.createdAt).toLocaleString() }))].map(
                  (v) => (
                    <li key={v.id} className="revision-item">
                      <label className="diff-pick" title="Select for comparison">
                        <Checkbox checked={diffPick.includes(v.id)} onCheckedChange={() => toggleDiffPick(v.id)} />
                      </label>
                      <span className="muted">{v.label}</span>
                      {v.id !== 'current' && (
                        <Button variant="link" onClick={() => restoreRevision(v.id)}>
                          Restore
                        </Button>
                      )}
                    </li>
                  ),
                )}
              </ul>
              {diffPair && versionFor(diffPair[0]) && versionFor(diffPair[1]) && (
                <RevisionDiff a={versionFor(diffPair[0])!} b={versionFor(diffPair[1])!} />
              )}
            </>
          )}
        </div>
      )}

      {/* eslint-disable-next-line react/no-danger */}
      <div className="preview-body" onClick={handlePreviewClick} dangerouslySetInnerHTML={{ __html: previewHtml || '<p class="muted">(empty)</p>' }} />
    </article>
  );
}
