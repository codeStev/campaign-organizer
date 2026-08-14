import { FormEvent, MouseEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  articlesApi,
  articleRevisionsApi,
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
  ApiError,
} from '../api/client';
import { RichTextEditor } from '../components/RichTextEditor';
import { CommandPalette, Command } from '../components/CommandPalette';
import { MapsView } from './MapsView';
import { TimelinesView } from './TimelinesView';
import { CalendarsView } from './CalendarsView';
import { RelationshipsView } from './RelationshipsView';
import { CampaignsView } from './CampaignsView';
import { SheetsView } from './SheetsView';
import { WhiteboardsView } from './WhiteboardsView';

/** True when the HTML has no meaningful text content. */
function isEmptyHtml(html: string): boolean {
  return html.replace(/<[^>]*>/g, '').trim().length === 0;
}

function outlineHtml(info: ArticleTemplateInfo): string {
  return info.sections.map((s) => `<h2>${s.heading}</h2><p></p>`).join('');
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
}

const EMPTY_DRAFT: Draft = { id: null, title: '', template: 'GENERIC', body: '' };

type Tab =
  | 'articles'
  | 'maps'
  | 'timelines'
  | 'calendars'
  | 'relationships'
  | 'campaigns'
  | 'sheets'
  | 'whiteboards';

const TABS: { key: Tab; label: string }[] = [
  { key: 'articles', label: 'Articles' },
  { key: 'maps', label: 'Maps' },
  { key: 'timelines', label: 'Timelines' },
  { key: 'calendars', label: 'Calendars' },
  { key: 'relationships', label: 'Relationships' },
  { key: 'campaigns', label: 'Campaigns' },
  { key: 'sheets', label: 'Sheets' },
  { key: 'whiteboards', label: 'Whiteboards' },
];

export function WorldView({ worldId, worldName, onBack, onAuthExpired }: Props) {
  const api = articlesApi(worldId);
  const media = mediaApi(worldId);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [query, setQuery] = useState('');
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [previewHtml, setPreviewHtml] = useState('');
  const [templates, setTemplates] = useState<ArticleTemplateInfo[]>([]);
  const [tab, setTab] = useState<Tab>('articles');
  // Article panel mode: read (rendered, clickable links) vs edit (TipTap form).
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  // Active article-type filters; empty set means no filtering (show all).
  const [typeFilter, setTypeFilter] = useState<Set<ArticleTemplate>>(new Set());
  // Revision history for the open article (null = panel hidden).
  const [revisions, setRevisions] = useState<ArticleRevision[] | null>(null);
  // Campaigns in this world, for the "used in campaign" filter and usage tags.
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  // Narrow the article list to entries referenced by this campaign ('' = all).
  const [campaignFilter, setCampaignFilter] = useState('');
  // "Used by" panel for the open article (null = hidden).
  const [usages, setUsages] = useState<Usage[] | null>(null);
  // Ctrl/Cmd-K command palette; its article list is unfiltered by the sidebar.
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [paletteArticles, setPaletteArticles] = useState<ArticleSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  const visibleArticles = articles.filter(
    (a) => typeFilter.size === 0 || typeFilter.has(a.template),
  );

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
    async (q: string, campaignId: string) => {
      try {
        const params: { q?: string; campaignId?: string } = {};
        if (q) params.q = q;
        if (campaignId) params.campaignId = campaignId;
        setArticles(await api.list(Object.keys(params).length ? params : undefined));
      } catch (err) {
        handleError(err);
      }
    },
    [api, handleError],
  );

  useEffect(() => {
    // Debounce so typing a query doesn't fire a request per keystroke.
    const handle = setTimeout(() => void refresh(query, campaignFilter), 150);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, campaignFilter]);

  useEffect(() => {
    templatesApi.list().then(setTemplates).catch(handleError);
    campaignsApi(worldId).list().then(setCampaigns).catch(handleError);
  }, [handleError, worldId]);

  // Changing the template on an empty draft seeds an outline (ADR-0015).
  function selectTemplate(template: ArticleTemplate) {
    setDraft((current) => {
      const next = { ...current, template };
      if (isEmptyHtml(current.body)) {
        const info = templates.find((t) => t.template === template);
        if (info) next.body = outlineHtml(info);
      }
      return next;
    });
  }

  async function openArticle(id: string) {
    try {
      const article = await api.get(id);
      setDraft({
        id: article.id,
        title: article.title,
        template: article.template,
        body: article.body ?? '',
      });
      setPreviewHtml(article.bodyHtml ?? '');
      setMode('read');
      setRevisions(null);
      setUsages(null);
    } catch (err) {
      handleError(err);
    }
  }

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
      return;
    }
    if (!draft.id) return;
    try {
      setRevisions(await articleRevisionsApi(worldId, draft.id).list());
    } catch (err) {
      handleError(err);
    }
  }

  async function restoreRevision(revisionId: string) {
    if (!draft.id) return;
    try {
      const restored = await articleRevisionsApi(worldId, draft.id).restore(revisionId);
      setDraft({
        id: restored.id,
        title: restored.title,
        template: restored.template,
        body: restored.body ?? '',
      });
      setPreviewHtml(restored.bodyHtml ?? '');
      setRevisions(null);
      await refresh(query, campaignFilter);
    } catch (err) {
      handleError(err);
    }
  }

  function openFromMap(id: string) {
    setTab('articles');
    void openArticle(id);
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
      run: () => setTab(t.key),
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
    const payload = { title: draft.title, template: draft.template, body: draft.body };
    try {
      const saved = draft.id
        ? await api.update(draft.id, payload)
        : await api.create(payload);
      setDraft({ id: saved.id, title: saved.title, template: saved.template, body: saved.body ?? '' });
      setPreviewHtml(saved.bodyHtml ?? '');
      setMode('read');
      await refresh(query, campaignFilter);
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
      await refresh(query, campaignFilter);
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

  return (
    <section className="world-view">
      <CommandPalette
        open={paletteOpen}
        commands={commands}
        onClose={() => setPaletteOpen(false)}
      />
      <div className="world-view-bar">
        <button className="link-button" onClick={onBack}>
          ← Worlds
        </button>
        <h2>{worldName}</h2>
        <button
          className="link-button palette-btn"
          onClick={() => void openPalette()}
          title="Jump to anything (Ctrl/⌘-K)"
        >
          ⌘K Jump…
        </button>
        <button className="link-button export-btn" onClick={handleExport} title="Download world as JSON">
          ⭳ Export
        </button>
        <nav className="tabs">
          <button
            className={tab === 'articles' ? 'tab active' : 'tab'}
            onClick={() => setTab('articles')}
          >
            Articles
          </button>
          <button
            className={tab === 'maps' ? 'tab active' : 'tab'}
            onClick={() => setTab('maps')}
          >
            Maps
          </button>
          <button
            className={tab === 'timelines' ? 'tab active' : 'tab'}
            onClick={() => setTab('timelines')}
          >
            Timelines
          </button>
          <button
            className={tab === 'calendars' ? 'tab active' : 'tab'}
            onClick={() => setTab('calendars')}
          >
            Calendars
          </button>
          <button
            className={tab === 'relationships' ? 'tab active' : 'tab'}
            onClick={() => setTab('relationships')}
          >
            Relationships
          </button>
          <button
            className={tab === 'campaigns' ? 'tab active' : 'tab'}
            onClick={() => setTab('campaigns')}
          >
            Campaigns
          </button>
          <button
            className={tab === 'sheets' ? 'tab active' : 'tab'}
            onClick={() => setTab('sheets')}
          >
            Sheets
          </button>
          <button
            className={tab === 'whiteboards' ? 'tab active' : 'tab'}
            onClick={() => setTab('whiteboards')}
          >
            Whiteboards
          </button>
        </nav>
      </div>

      {error && <p className="error">{error}</p>}

      {tab === 'maps' ? (
        <MapsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />
      ) : tab === 'timelines' ? (
        <TimelinesView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />
      ) : tab === 'calendars' ? (
        <CalendarsView worldId={worldId} onAuthExpired={onAuthExpired} />
      ) : tab === 'relationships' ? (
        <RelationshipsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />
      ) : tab === 'campaigns' ? (
        <CampaignsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />
      ) : tab === 'sheets' ? (
        <SheetsView worldId={worldId} onOpenArticle={openFromMap} onAuthExpired={onAuthExpired} />
      ) : tab === 'whiteboards' ? (
        <WhiteboardsView worldId={worldId} onAuthExpired={onAuthExpired} />
      ) : (
      <div className="wiki-layout">
        <aside className="wiki-sidebar">
          <input
            type="search"
            placeholder="Search articles…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button
            onClick={() => {
              setDraft(EMPTY_DRAFT);
              setPreviewHtml('');
              setMode('edit');
            }}
          >
            + New article
          </button>

          <div className="type-filters">
            {ARTICLE_TEMPLATES.map((t) => (
              <button
                key={t}
                type="button"
                className={typeFilter.has(t) ? 'type-chip active' : 'type-chip'}
                onClick={() => toggleType(t)}
                title={`Filter by ${templateLabel(t)}`}
              >
                {templateLabel(t)}
              </button>
            ))}
          </div>

          {campaigns.length > 0 && (
            <select
              className="campaign-filter"
              value={campaignFilter}
              onChange={(e) => setCampaignFilter(e.target.value)}
              title="Show only articles used in a campaign"
            >
              <option value="">All campaigns</option>
              {campaigns.map((c) => (
                <option key={c.id} value={c.id}>
                  Used in {c.name}
                </option>
              ))}
            </select>
          )}

          <div className="article-list-scroll">
            <ul className="article-list">
              {visibleArticles.map((a) => (
                <li key={a.id}>
                  <button
                    className={a.id === draft.id ? 'article-link active' : 'article-link'}
                    onClick={() => openArticle(a.id)}
                  >
                    <span>{a.title}</span>
                    <small className="muted">{a.template.toLowerCase()}</small>
                  </button>
                </li>
              ))}
              {visibleArticles.length === 0 && (
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
                <input
                  className="title-input"
                  placeholder="Article title"
                  value={draft.title}
                  onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                  required
                />
                <select
                  value={draft.template}
                  onChange={(e) => selectTemplate(e.target.value as ArticleTemplate)}
                >
                  {ARTICLE_TEMPLATES.map((t) => (
                    <option key={t} value={t}>
                      {templateLabel(t)}
                    </option>
                  ))}
                </select>
                <RichTextEditor
                  value={draft.body}
                  onChange={(body) => setDraft({ ...draft, body })}
                  onUploadImage={async (file) => (await media.upload(file)).url}
                />
                <div className="editor-actions">
                  <button type="submit" disabled={draft.title.length === 0}>
                    {draft.id ? 'Save changes' : 'Create article'}
                  </button>
                  {draft.id && (
                    <button type="button" className="link-button" onClick={() => setMode('read')}>
                      Cancel
                    </button>
                  )}
                  {draft.id && (
                    <button type="button" className="link-button danger" onClick={handleDelete}>
                      Delete
                    </button>
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
                <h2>{draft.title}</h2>
                <div className="editor-actions">
                  <button type="button" onClick={() => setMode('edit')}>
                    Edit
                  </button>
                  <button type="button" className="link-button" onClick={toggleUsages}>
                    Used by
                  </button>
                  <button type="button" className="link-button" onClick={toggleHistory}>
                    History
                  </button>
                  <button type="button" className="link-button danger" onClick={handleDelete}>
                    Delete
                  </button>
                </div>
              </div>

              {usages !== null && (
                <div className="usages card">
                  <strong className="muted">Used by</strong>
                  {usages.length === 0 && (
                    <p className="muted">Not referenced anywhere yet.</p>
                  )}
                  <ul className="usage-list">
                    {usages.map((u, i) => {
                      const clickable =
                        (u.type === 'ARTICLE_LINK' || u.type === 'RELATIONSHIP') && u.targetId;
                      return (
                        <li key={i} className="usage-item">
                          <span className="usage-type">{USAGE_LABELS[u.type]}</span>
                          {clickable ? (
                            <button
                              className="link-button usage-label"
                              onClick={() => openArticle(u.targetId!)}
                            >
                              {u.label}
                            </button>
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
                  <ul className="revision-list">
                    {revisions.map((r) => (
                      <li key={r.id} className="revision-item">
                        <span className="muted">{new Date(r.createdAt).toLocaleString()}</span>
                        <button className="link-button" onClick={() => restoreRevision(r.id)}>
                          Restore
                        </button>
                      </li>
                    ))}
                  </ul>
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
      )}
    </section>
  );
}
