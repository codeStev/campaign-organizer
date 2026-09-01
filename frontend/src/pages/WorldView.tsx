import { FormEvent, MouseEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import {
  articlesApi,
  articleRevisionsApi,
  articleTagsApi,
  worldTagsApi,
  campaignsApi,
  exportWorld,
  ArticleSummary,
  ArticleRevision,
  ArticleTemplate,
  ArticleTemplateInfo,
  ARTICLE_TEMPLATES,
  Campaign,
  Usage,
  templatesApi,
  mediaApi,
  aiApi,
  ApiError,
} from '../api/client';
import { Button } from '../components/ui/button';
import { Toggle } from '../components/ui/toggle';
import { toast } from 'sonner';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { Tabs, TabsList, TabsTrigger } from '../components/ui/tabs';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { TagInput, TagList } from '../components/TagInput';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { CommandPalette, Command } from '../components/CommandPalette';
import { RevisionDiff } from '../components/RevisionDiff';
import { PrintView } from './PrintView';
import { MapsView } from './MapsView';
import { TimelinesView } from './TimelinesView';
import { CalendarsView } from './CalendarsView';
import { RelationshipsView } from './RelationshipsView';
import { CampaignsView } from './CampaignsView';
import { PlayersPanel } from './PlayersPanel';
import { SheetsView } from './SheetsView';
import { WhiteboardsView } from './WhiteboardsView';
import { TablesView } from './TablesView';
import { HandoutsView } from './HandoutsView';
import { ConsistencyView } from './ConsistencyView';
import { TagBrowseView } from './TagBrowseView';

/** True when the Markdown has no meaningful text content. */
// Radix Select can't use "" as an item value (reserved for "no selection"),
// so a meaningfully persistent "none" state goes through this sentinel.
const NONE_VALUE = '__none__';

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

/**
 * Sidebar tree, collapsed by default: articles with no parent (or whose
 * parent didn't survive the current filter/search) render at the root so
 * filtering never makes an article unreachable.
 */
function ArticleTreeItem({
  article,
  childrenByParent,
  expandedIds,
  onToggleExpand,
  activeId,
  onOpen,
}: {
  article: ArticleSummary;
  childrenByParent: Map<string, ArticleSummary[]>;
  expandedIds: Set<string>;
  onToggleExpand: (id: string) => void;
  activeId: string | null;
  onOpen: (id: string) => void;
}) {
  const children = childrenByParent.get(article.id) ?? [];
  const expanded = expandedIds.has(article.id);
  return (
    <li>
      <div className="article-tree-row">
        {children.length > 0 ? (
          <button
            type="button"
            className="article-tree-toggle"
            onClick={() => onToggleExpand(article.id)}
            title={expanded ? 'Collapse' : 'Expand'}
          >
            {expanded ? '▾' : '▸'}
          </button>
        ) : (
          <span className="article-tree-toggle-spacer" />
        )}
        <button
          className={article.id === activeId ? 'article-link active' : 'article-link'}
          onClick={() => onOpen(article.id)}
        >
          <TruncatedLabel label={article.title}>{article.title}</TruncatedLabel>
          <small className="muted">{article.template.toLowerCase()}</small>
        </button>
      </div>
      {expanded && children.length > 0 && (
        <ul className="article-list article-list-nested">
          {children.map((c) => (
            <ArticleTreeItem
              key={c.id}
              article={c}
              childrenByParent={childrenByParent}
              expandedIds={expandedIds}
              onToggleExpand={onToggleExpand}
              activeId={activeId}
              onOpen={onOpen}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

interface Props {
  worldId: string;
  worldName: string;
  onBack: () => void;
  onAuthExpired: () => void;
}

interface Draft {
  id: string | null;
  title: string;
  template: ArticleTemplate;
  body: string;
  parentArticleId: string | null;
  tags: string[];
}

const EMPTY_DRAFT: Draft = {
  id: null,
  title: '',
  template: 'GENERIC',
  body: '',
  parentArticleId: null,
  tags: [],
};

type Tab =
  | 'articles'
  | 'maps'
  | 'timelines'
  | 'calendars'
  | 'relationships'
  | 'campaigns'
  | 'players'
  | 'sheets'
  | 'whiteboards'
  | 'tables'
  | 'handouts'
  | 'tags'
  | 'consistency';

/** Route path segments, in nav order. */
const TABS: { key: Tab; label: string }[] = [
  { key: 'articles', label: 'Articles' },
  { key: 'maps', label: 'Maps' },
  { key: 'timelines', label: 'Timelines' },
  { key: 'calendars', label: 'Calendars' },
  { key: 'relationships', label: 'Relationships' },
  { key: 'campaigns', label: 'Campaigns' },
  { key: 'players', label: 'Players' },
  { key: 'sheets', label: 'Sheets' },
  { key: 'whiteboards', label: 'Whiteboards' },
  { key: 'tables', label: 'Tables & Decks' },
  { key: 'handouts', label: 'Handouts' },
  { key: 'tags', label: 'Tags' },
  { key: 'consistency', label: 'Consistency' },
];

export function WorldView({ worldId, worldName, onBack, onAuthExpired }: Props) {
  const api = articlesApi(worldId);
  const media = mediaApi(worldId);
  const ai = aiApi(worldId);
  const navigate = useNavigate();
  // WorldView declares its own nested <Routes> below, so it's an ancestor of
  // wherever "articles/:articleId" matches — useParams() can't see that param
  // from here (only a genuine route-element descendant can). Read it from the
  // URL directly instead; useLocation() works from any depth.
  const location = useLocation();
  const articleIdMatch = location.pathname.match(/\/articles\/([^/]+)$/);
  const articleId = articleIdMatch ? articleIdMatch[1] : undefined;
  // Which top-level tab is active, from the URL rather than local state, so a
  // direct link/reload lands on the right tab too.
  const activeTabKey = location.pathname.replace(`/worlds/${worldId}`, '').split('/').filter(Boolean)[0];
  const activeTab: Tab = TABS.some((t) => t.key === activeTabKey) ? (activeTabKey as Tab) : 'articles';
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [query, setQuery] = useState('');
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [previewHtml, setPreviewHtml] = useState('');
  const [templates, setTemplates] = useState<ArticleTemplateInfo[]>([]);
  // Article panel mode: read (rendered, clickable links) vs edit (TipTap form).
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  // Active article-type filters; empty set means no filtering (show all).
  const [typeFilter, setTypeFilter] = useState<Set<ArticleTemplate>>(new Set());
  // Revision history for the open article (null = panel hidden).
  const [revisions, setRevisions] = useState<ArticleRevision[] | null>(null);
  // Up to two versions selected for diffing ('current' or a revision id).
  const [diffPick, setDiffPick] = useState<string[]>([]);
  // Campaigns in this world, for the "used in campaign" filter and usage tags.
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  // Narrow the article list to entries referenced by this campaign ('' = all).
  const [campaignFilter, setCampaignFilter] = useState('');
  // Narrow the article list to entries carrying this tag ('' = all, ADR-0083).
  const [tagFilter, setTagFilter] = useState('');
  const [worldTags, setWorldTags] = useState<string[]>([]);
  const [articlesLoading, setArticlesLoading] = useState(true);
  // "Used by" panel for the open article (null = hidden).
  const [usages, setUsages] = useState<Usage[] | null>(null);
  // Ctrl/Cmd-K command palette; its article list is unfiltered by the sidebar.
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [paletteArticles, setPaletteArticles] = useState<ArticleSummary[]>([]);
  // Full-screen print/PDF view.
  const [printOpen, setPrintOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Sidebar tree: which parent articles are expanded; collapsed by default.
  const [expandedArticleIds, setExpandedArticleIds] = useState<Set<string>>(new Set());
  // Breadcrumb title for the open article's parent, when the parent isn't in
  // the currently-loaded (possibly search-filtered) articles list.
  const [parentArticleTitle, setParentArticleTitle] = useState<string | null>(null);

  const visibleArticles = articles.filter(
    (a) => typeFilter.size === 0 || typeFilter.has(a.template),
  );

  // Sidebar tree, built from the currently-visible (filtered/searched) set.
  const visibleIds = new Set(visibleArticles.map((a) => a.id));
  const sidebarChildrenByParent = new Map<string, ArticleSummary[]>();
  const rootArticles: ArticleSummary[] = [];
  for (const a of visibleArticles) {
    if (a.parentArticleId && visibleIds.has(a.parentArticleId)) {
      const bucket = sidebarChildrenByParent.get(a.parentArticleId) ?? [];
      bucket.push(a);
      sidebarChildrenByParent.set(a.parentArticleId, bucket);
    } else {
      rootArticles.push(a);
    }
  }

  // Parent-article picker candidates: every article except the one being
  // edited and its own descendants (built from the full, unfiltered list -
  // a candidate hidden by the sidebar's type filter is still a valid parent).
  const allChildrenByParent = groupByParent(articles);
  const excludedParentIds = draft.id
    ? new Set([draft.id, ...descendantIds(draft.id, allChildrenByParent)])
    : new Set<string>();
  const parentCandidates = articles.filter((a) => !excludedParentIds.has(a.id));

  function toggleExpandedArticle(id: string) {
    setExpandedArticleIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleType(t: ArticleTemplate) {
    setTypeFilter((prev) => {
      const next = new Set(prev);
      if (next.has(t)) next.delete(t);
      else next.add(t);
      return next;
    });
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

  const refresh = useCallback(
    async (q: string, campaignId: string, tag: string) => {
      try {
        const params: { q?: string; campaignId?: string; tag?: string } = {};
        if (q) params.q = q;
        if (campaignId) params.campaignId = campaignId;
        if (tag) params.tag = tag;
        setArticles(await api.list(Object.keys(params).length ? params : undefined));
      } catch (err) {
        handleError(err);
      } finally {
        setArticlesLoading(false);
      }
    },
    [api, handleError],
  );

  useEffect(() => {
    // Debounce so typing a query doesn't fire a request per keystroke.
    const handle = setTimeout(() => void refresh(query, campaignFilter, tagFilter), 150);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, campaignFilter, tagFilter]);

  const loadWorldTags = useCallback(() => {
    worldTagsApi(worldId).list().then(setWorldTags).catch(handleError);
  }, [worldId, handleError]);

  useEffect(() => {
    templatesApi.list().then(setTemplates).catch(handleError);
    campaignsApi(worldId).list().then(setCampaigns).catch(handleError);
    loadWorldTags();
  }, [handleError, worldId, loadWorldTags]);

  // Changing the template on an empty draft seeds an outline (ADR-0015).
  function selectTemplate(template: ArticleTemplate) {
    setDraft((current) => {
      const next = { ...current, template };
      if (isEmptyMarkdown(current.body)) {
        const info = templates.find((t) => t.template === template);
        if (info) next.body = outlineMarkdown(info);
      }
      return next;
    });
  }

  const loadArticle = useCallback(
    async (id: string) => {
      try {
        const [article, articleTags] = await Promise.all([api.get(id), articleTagsApi(worldId, id).get()]);
        setDraft({
          id: article.id,
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

  // The URL is the source of truth for which article is open (ADR-0053): clicking an
  // article navigates, and this effect does the actual load — including on a direct
  // deep link, a page reload, or browser back/forward.
  useEffect(() => {
    if (!articleId || articleId === draft.id) return;
    void loadArticle(articleId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [articleId]);

  function openArticle(id: string) {
    navigate(`/worlds/${worldId}/articles/${id}`);
  }

  // Breadcrumb title for the open article's parent: reuse the already-loaded
  // list when possible, otherwise a single lightweight fetch for just the title.
  useEffect(() => {
    const parentId = draft.parentArticleId;
    if (!parentId) {
      setParentArticleTitle(null);
      return;
    }
    const local = articles.find((a) => a.id === parentId);
    if (local) {
      setParentArticleTitle(local.title);
      return;
    }
    let active = true;
    api
      .get(parentId)
      .then((a) => {
        if (active) setParentArticleTitle(a.title);
      })
      .catch(() => {
        if (active) setParentArticleTitle(null);
      });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft.parentArticleId, articles]);

  async function toggleUsages() {
    if (usages !== null) {
      setUsages(null);
      return;
    }
    if (!draft.id) return;
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
    if (!draft.id) return;
    try {
      setRevisions(await articleRevisionsApi(worldId, draft.id).list());
    } catch (err) {
      handleError(err);
    }
  }

  /** Resolve a diff selection ('current' or a revision id) to a comparable version. */
  function versionFor(key: string | null) {
    if (key === 'current') {
      return { label: 'Current', title: draft.title, body: draft.body };
    }
    const r = revisions?.find((rev) => rev.id === key);
    return r
      ? { label: new Date(r.createdAt).toLocaleString(), title: r.title, body: r.body ?? '' }
      : null;
  }

  /** Recency of a version key; 'current' is newest. Used to order the diff. */
  function versionTime(key: string): number {
    if (key === 'current') return Infinity;
    const r = revisions?.find((rev) => rev.id === key);
    return r ? new Date(r.createdAt).getTime() : 0;
  }

  // Toggle a version into the (max two) diff selection.
  function toggleDiffPick(id: string) {
    setDiffPick((prev) => {
      if (prev.includes(id)) return prev.filter((x) => x !== id);
      if (prev.length >= 2) return [prev[1], id]; // keep most-recent pick + new one
      return [...prev, id];
    });
  }

  // The two picks ordered older→newer, so newer always renders as "+".
  const diffPair =
    diffPick.length === 2
      ? [...diffPick].sort((x, y) => versionTime(x) - versionTime(y))
      : null;

  async function restoreRevision(revisionId: string) {
    if (!draft.id) return;
    try {
      const restored = await articleRevisionsApi(worldId, draft.id).restore(revisionId);
      setDraft((d) => ({
        id: restored.id,
        title: restored.title,
        template: restored.template,
        body: restored.body ?? '',
        parentArticleId: restored.parentArticleId ?? null,
        tags: d.tags,
      }));
      setPreviewHtml(restored.bodyHtml ?? '');
      setRevisions(null);
      await refresh(query, campaignFilter, tagFilter);
    } catch (err) {
      handleError(err);
    }
  }

  function openFromMap(id: string) {
    openArticle(id);
  }

  const openPalette = useCallback(async () => {
    setPaletteOpen(true);
    try {
      // Always the full, unfiltered list so the palette can reach any article.
      setPaletteArticles(await api.list());
    } catch (err) {
      handleError(err);
    }
  }, [api, handleError]);

  // Global Ctrl/Cmd-K opens the palette.
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        void openPalette();
      }
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [openPalette]);

  const commands = useMemo<Command[]>(() => {
    const nav: Command[] = TABS.map((t) => ({
      id: `tab:${t.key}`,
      label: `Go to ${t.label}`,
      group: 'Navigate',
      keywords: t.key,
      run: () => navigate(`/worlds/${worldId}/${t.key}`),
    }));
    const articleCmds: Command[] = paletteArticles.map((a) => ({
      id: `article:${a.id}`,
      label: a.title,
      group: 'Articles',
      keywords: a.template,
      run: () => openFromMap(a.id),
    }));
    return [...nav, ...articleCmds];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paletteArticles]);

  function handlePreviewClick(event: MouseEvent<HTMLDivElement>) {
    const link = (event.target as HTMLElement).closest('.wiki-link');
    if (link) {
      event.preventDefault();
      const id = link.getAttribute('data-article-id');
      if (id) void openArticle(id);
    }
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    setError(null);
    const wasNew = !draft.id;
    const payload = {
      title: draft.title,
      template: draft.template,
      body: draft.body,
      parentArticleId: draft.parentArticleId,
    };
    try {
      const saved = draft.id
        ? await api.update(draft.id, payload)
        : await api.create(payload);
      const savedTags = await articleTagsApi(worldId, saved.id).set(draft.tags);
      setDraft({
        id: saved.id,
        title: saved.title,
        template: saved.template,
        body: saved.body ?? '',
        parentArticleId: saved.parentArticleId ?? null,
        tags: savedTags.tags,
      });
      setPreviewHtml(saved.bodyHtml ?? '');
      setMode('read');
      if (wasNew) navigate(`/worlds/${worldId}/articles/${saved.id}`);
      await refresh(query, campaignFilter, tagFilter);
      loadWorldTags();
      toast.success(`Article "${saved.title}" saved`);
    } catch (err) {
      handleError(err);
    }
  }

  async function handleDelete() {
    if (!draft.id) return;
    try {
      await api.remove(draft.id);
      setDraft(EMPTY_DRAFT);
      setPreviewHtml('');
      navigate(`/worlds/${worldId}/articles`);
      await refresh(query, campaignFilter, tagFilter);
    } catch (err) {
      handleError(err);
    }
  }

  async function handleExport() {
    try {
      await exportWorld(worldId);
    } catch (err) {
      handleError(err);
    }
  }

  const articlesPane = (
      <div className="wiki-layout">
        <aside className="wiki-sidebar">
          <Input
            type="search"
            placeholder="Search articles…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <Button
            onClick={() => {
              setDraft(EMPTY_DRAFT);
              setPreviewHtml('');
              setMode('edit');
              navigate(`/worlds/${worldId}/articles`);
            }}
          >
            + New article
          </Button>

          <div className="type-filters">
            {ARTICLE_TEMPLATES.map((t) => (
              <Toggle
                key={t}
                type="button"
                className={typeFilter.has(t) ? 'type-chip active' : 'type-chip'}
                pressed={typeFilter.has(t)}
                onPressedChange={() => toggleType(t)}
                title={`Filter by ${templateLabel(t)}`}
              >
                {templateLabel(t)}
              </Toggle>
            ))}
          </div>

          {campaigns.length > 0 && (
            <Select
              value={campaignFilter || NONE_VALUE}
              onValueChange={(v) => setCampaignFilter(v === NONE_VALUE ? '' : v)}
            >
              <SelectTrigger className="campaign-filter" title="Show only articles used in a campaign">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE_VALUE}>All campaigns</SelectItem>
                {campaigns.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    Used in {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}

          {worldTags.length > 0 && (
            <Select value={tagFilter || NONE_VALUE} onValueChange={(v) => setTagFilter(v === NONE_VALUE ? '' : v)}>
              <SelectTrigger className="tag-filter" title="Show only articles carrying a tag">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE_VALUE}>All tags</SelectItem>
                {worldTags.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}

          <div className="article-list-scroll">
            <ul className="article-list">
              {rootArticles.map((a) => (
                <ArticleTreeItem
                  key={a.id}
                  article={a}
                  childrenByParent={sidebarChildrenByParent}
                  expandedIds={expandedArticleIds}
                  onToggleExpand={toggleExpandedArticle}
                  activeId={draft.id}
                  onOpen={openArticle}
                />
              ))}
              {articlesLoading && (
                <li className="muted loading-row">
                  <Spinner /> Loading…
                </li>
              )}
              {!articlesLoading && visibleArticles.length === 0 && (
                <li className="muted">
                  {articles.length === 0 ? 'No articles yet.' : 'No articles match the filter.'}
                </li>
              )}
            </ul>
          </div>
        </aside>

        <div className="wiki-main">
          {mode === 'edit' ? (
            <>
              <form className="wiki-editor card" onSubmit={handleSave}>
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
                    // Radix's hidden native-<select> fallback fires a spurious
                    // onValueChange('') on (re)mount - not a real user pick, and
                    // our own items never carry an empty-string value, so ignore it.
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
                  <Button type="submit" disabled={draft.title.length === 0}>
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
                        <Button
                          type="button"
                          variant="link"
                          className="text-destructive hover:text-destructive"
                        >
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
                  Tip: link to another article with <code>[[Title]]</code> or{' '}
                  <code>[[Title|label]]</code>.
                </p>
              </form>

              {previewHtml && (
                <div className="card preview">
                  <h3 className="muted">Preview</h3>
                  {/* eslint-disable-next-line react/no-danger */}
                  <div
                    className="preview-body"
                    onClick={handlePreviewClick}
                    dangerouslySetInnerHTML={{ __html: previewHtml }}
                  />
                </div>
              )}
            </>
          ) : draft.id ? (
            <article className="card article-read">
              <div className="article-read-head">
                <div>
                  {draft.parentArticleId && parentArticleTitle && (
                    <p className="muted breadcrumb">
                      Part of{' '}
                      <button
                        type="button"
                        className="breadcrumb-link"
                        onClick={() => openArticle(draft.parentArticleId!)}
                      >
                        {parentArticleTitle}
                      </button>
                    </p>
                  )}
                  <h2>{draft.title}</h2>
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
                  {usages.length === 0 && (
                    <p className="muted">Not referenced anywhere yet.</p>
                  )}
                  <ul className="usage-list">
                    {usages.map((u, i) => {
                      const clickable =
                        (u.type === 'ARTICLE_LINK' || u.type === 'RELATIONSHIP' || u.type === 'CHILD_ARTICLE')
                        && u.targetId;
                      return (
                        <li key={i} className="usage-item">
                          <span className="usage-type">{USAGE_LABELS[u.type]}</span>
                          {clickable ? (
                            <Button
                              variant="link"
                              className="usage-label"
                              onClick={() => openArticle(u.targetId!)}
                            >
                              {u.label}
                            </Button>
                          ) : (
                            <span className="usage-label">{u.label}</span>
                          )}
                          {u.campaignName && (
                            <span className="usage-campaign">{u.campaignName}</span>
                          )}
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
                      <p className="muted hint">
                        Tick two versions to compare — the newer one is always the “+” side.
                      </p>
                      <ul className="revision-list">
                        {[{ id: 'current', label: 'Current version' }, ...revisions.map((r) => ({
                          id: r.id,
                          label: new Date(r.createdAt).toLocaleString(),
                        }))].map((v) => (
                          <li key={v.id} className="revision-item">
                            <label className="diff-pick" title="Select for comparison">
                              <Checkbox
                                checked={diffPick.includes(v.id)}
                                onCheckedChange={() => toggleDiffPick(v.id)}
                              />
                            </label>
                            <span className="muted">{v.label}</span>
                            {v.id !== 'current' && (
                              <Button variant="link" onClick={() => restoreRevision(v.id)}>
                                Restore
                              </Button>
                            )}
                          </li>
                        ))}
                      </ul>
                      {diffPair && versionFor(diffPair[0]) && versionFor(diffPair[1]) && (
                        <RevisionDiff a={versionFor(diffPair[0])!} b={versionFor(diffPair[1])!} />
                      )}
                    </>
                  )}
                </div>
              )}

              {/* eslint-disable-next-line react/no-danger */}
              <div
                className="preview-body"
                onClick={handlePreviewClick}
                dangerouslySetInnerHTML={{ __html: previewHtml || '<p class="muted">(empty)</p>' }}
              />
            </article>
          ) : (
            <p className="muted">Select an article from the list, or create a new one.</p>
          )}
        </div>
      </div>
  );

  const mapsPane = <MapsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />;
  const timelinesPane = (
    <TimelinesView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />
  );
  const calendarsPane = <CalendarsView worldId={worldId} onAuthExpired={onAuthExpired} />;
  const campaignsPane = (
    <CampaignsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />
  );
  const playersPane = <PlayersPanel worldId={worldId} onAuthExpired={onAuthExpired} />;
  const whiteboardsPane = <WhiteboardsView worldId={worldId} onAuthExpired={onAuthExpired} />;
  const tablesPane = <TablesView worldId={worldId} onAuthExpired={onAuthExpired} />;
  const consistencyPane = (
    <ConsistencyView
      worldId={worldId}
      worldName={worldName}
      onOpenArticle={openFromMap}
      onAuthExpired={onAuthExpired}
    />
  );
  const tagsPane = (
    <TagBrowseView
      worldId={worldId}
      onOpenArticle={openFromMap}
      onOpenStatblock={(id) => navigate(`/worlds/${worldId}/sheets/statblocks/${id}`)}
      onAuthExpired={onAuthExpired}
    />
  );

  return (
    <section className="world-view">
      <CommandPalette
        open={paletteOpen}
        commands={commands}
        onClose={() => setPaletteOpen(false)}
      />
      {printOpen && (
        <PrintView
          worldId={worldId}
          worldName={worldName}
          campaigns={campaigns}
          onClose={() => setPrintOpen(false)}
          onError={handleError}
        />
      )}
      <div className="world-view-bar">
        <Button variant="link" onClick={onBack}>
          ← Worlds
        </Button>
        <h2>{worldName}</h2>
        <Button
          variant="link"
          className="palette-btn"
          onClick={() => void openPalette()}
          title="Jump to anything (Ctrl/⌘-K)"
        >
          ⌘K Jump…
        </Button>
        <Button
          variant="link"
          className="print-btn"
          onClick={() => setPrintOpen(true)}
          title="Print or save as PDF"
        >
          🖨 Print
        </Button>
        <Button variant="link" className="export-btn" onClick={handleExport} title="Download world as JSON">
          ⭳ Export
        </Button>
        <Tabs value={activeTab} className="tabs">
          <TabsList variant="line">
            {TABS.map((t) => (
              <TabsTrigger key={t.key} value={t.key} asChild>
                <NavLink to={t.key === 'articles' && articleId ? `articles/${articleId}` : t.key}>
                  {t.label}
                </NavLink>
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
      </div>

      {error && <p className="error">{error}</p>}

      <Routes>
        <Route index element={<Navigate to="articles" replace />} />
        <Route path="articles" element={articlesPane} />
        <Route path="articles/:articleId" element={articlesPane} />
        <Route path="maps" element={mapsPane} />
        <Route path="maps/:mapId" element={mapsPane} />
        <Route path="timelines" element={timelinesPane} />
        <Route path="timelines/:timelineId" element={timelinesPane} />
        <Route path="calendars" element={calendarsPane} />
        <Route path="calendars/:calendarId" element={calendarsPane} />
        <Route
          path="relationships"
          element={<RelationshipsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />}
        />
        <Route path="campaigns" element={campaignsPane} />
        <Route path="campaigns/:campaignId" element={campaignsPane} />
        <Route path="players" element={playersPane} />
        <Route
          path="sheets/*"
          element={<SheetsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />}
        />
        <Route path="whiteboards" element={whiteboardsPane} />
        <Route path="whiteboards/:whiteboardId" element={whiteboardsPane} />
        <Route path="tables" element={tablesPane} />
        <Route path="tables/:kind" element={tablesPane} />
        <Route path="tables/:kind/:entityId" element={tablesPane} />
        <Route
          path="handouts"
          element={<HandoutsView worldId={worldId} onAuthExpired={onAuthExpired} />}
        />
        <Route
          path="handouts/:handoutId"
          element={<HandoutsView worldId={worldId} onAuthExpired={onAuthExpired} />}
        />
        <Route path="tags" element={tagsPane} />
        <Route path="tags/:tagName" element={tagsPane} />
        <Route path="consistency" element={consistencyPane} />
        <Route path="*" element={<Navigate to="articles" replace />} />
      </Routes>
    </section>
  );
}
