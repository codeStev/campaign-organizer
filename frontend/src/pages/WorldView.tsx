import { FormEvent, MouseEvent, useCallback, useEffect, useState } from 'react';
import {
  articlesApi,
  ArticleSummary,
  ArticleTemplate,
  ArticleTemplateInfo,
  ARTICLE_TEMPLATES,
  templatesApi,
  mediaApi,
  ApiError,
} from '../api/client';
import { RichTextEditor } from '../components/RichTextEditor';

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

export function WorldView({ worldId, worldName, onBack, onAuthExpired }: Props) {
  const api = articlesApi(worldId);
  const media = mediaApi(worldId);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [query, setQuery] = useState('');
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [previewHtml, setPreviewHtml] = useState('');
  const [templates, setTemplates] = useState<ArticleTemplateInfo[]>([]);
  const [error, setError] = useState<string | null>(null);

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
    async (q: string) => {
      try {
        setArticles(await api.list(q ? { q } : undefined));
      } catch (err) {
        handleError(err);
      }
    },
    [api, handleError],
  );

  useEffect(() => {
    void refresh(query);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  useEffect(() => {
    templatesApi.list().then(setTemplates).catch(handleError);
  }, [handleError]);

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
    } catch (err) {
      handleError(err);
    }
  }

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
      await refresh(query);
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
      await refresh(query);
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <section className="world-view">
      <div className="world-view-bar">
        <button className="link-button" onClick={onBack}>
          ← Worlds
        </button>
        <h2>{worldName}</h2>
      </div>

      {error && <p className="error">{error}</p>}

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
            }}
          >
            + New article
          </button>
          <ul className="article-list">
            {articles.map((a) => (
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
            {articles.length === 0 && <li className="muted">No articles yet.</li>}
          </ul>
        </aside>

        <div className="wiki-main">
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
                {t.charAt(0) + t.slice(1).toLowerCase()}
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
        </div>
      </div>
    </section>
  );
}
