import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  characterSheetsApi,
  exportCharacterSheetPdf,
  CharacterSheet,
  SheetTemplate,
  ArticleSummary,
} from '../api/client';
import { SheetForm } from '../components/SheetForm';

interface Props {
  worldId: string;
  templates: SheetTemplate[];
  articles: ArticleSummary[];
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  name: string;
  templateId: string;
  articleId: string;
  values: Record<string, unknown>;
}

export function CharacterSheetsPanel({ worldId, templates, articles, onOpenArticle, onError }: Props) {
  const api = useMemo(() => characterSheetsApi(worldId), [worldId]);
  const [sheets, setSheets] = useState<CharacterSheet[]>([]);
  const [draft, setDraft] = useState<Draft | null>(null);

  const refresh = useCallback(async () => {
    try {
      setSheets(await api.list());
    } catch (err) {
      onError(err);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const template = templates.find((t) => t.id === draft?.templateId) ?? null;

  function newSheet() {
    if (templates.length === 0) {
      onError(new Error('Create a template first (Templates tab).'));
      return;
    }
    setDraft({ id: null, name: '', templateId: templates[0].id, articleId: '', values: {} });
  }

  async function open(id: string) {
    try {
      const sheet = await api.get(id);
      setDraft({
        id: sheet.id,
        name: sheet.name,
        templateId: sheet.templateId,
        articleId: sheet.articleId ?? '',
        values: sheet.values ?? {},
      });
    } catch (err) {
      onError(err);
    }
  }

  async function save() {
    if (!draft) return;
    const body = {
      name: draft.name,
      templateId: draft.templateId,
      articleId: draft.articleId || null,
      values: draft.values,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      setDraft({ ...draft, id: saved.id });
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function remove() {
    if (!draft?.id) return;
    try {
      await api.remove(draft.id);
      setDraft(null);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function exportPdf() {
    if (!draft?.id) return;
    try {
      await exportCharacterSheetPdf(worldId, draft.id);
    } catch (err) {
      onError(err);
    }
  }

  return (
    <div className="sheets-panel">
      <div className="sheets-list-col">
        <button onClick={newSheet}>+ New sheet</button>
        <ul className="article-list">
          {sheets.map((s) => (
            <li key={s.id}>
              <button
                className={s.id === draft?.id ? 'article-link active' : 'article-link'}
                onClick={() => open(s.id)}
              >
                <span>{s.name}</span>
              </button>
            </li>
          ))}
          {sheets.length === 0 && <li className="muted">No character sheets yet.</li>}
        </ul>
      </div>

      <div className="sheet-detail">
        {!draft && <p className="muted">Select or create a character sheet.</p>}
        {draft && (
          <>
            <div className="sheet-head">
              <input
                className="title-input"
                placeholder="Character name"
                value={draft.name}
                onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              />
              <select
                value={draft.templateId}
                onChange={(e) => setDraft({ ...draft, templateId: e.target.value })}
                disabled={draft.id != null}
              >
                {templates.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>

            <label className="sheet-article">
              <span className="muted">Linked article</span>
              <select
                value={draft.articleId}
                onChange={(e) => setDraft({ ...draft, articleId: e.target.value })}
              >
                <option value="">— none —</option>
                {articles.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.title}
                  </option>
                ))}
              </select>
              {draft.articleId && (
                <button className="link-button" onClick={() => onOpenArticle(draft.articleId)}>
                  Open
                </button>
              )}
            </label>

            {template && (
              <SheetForm
                sections={template.sections}
                values={draft.values}
                onChange={(values) => setDraft({ ...draft, values })}
              />
            )}

            <div className="editor-actions">
              <button onClick={save} disabled={!draft.name}>
                {draft.id ? 'Save sheet' : 'Create sheet'}
              </button>
              {draft.id && template && (
                <button className="link-button" onClick={exportPdf} title="Download a filled fillable PDF">
                  ⭳ Export PDF
                </button>
              )}
              {draft.id && (
                <button className="link-button danger" onClick={remove}>
                  Delete
                </button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
