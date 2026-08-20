import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  characterSheetsApi,
  exportCharacterSheetPdf,
  CharacterSheet,
  FieldTemplate,
  ArticleSummary,
  Campaign,
} from '../api/client';
import { TemplateForm } from '../components/TemplateForm';

interface Props {
  worldId: string;
  templates: FieldTemplate[];
  articles: ArticleSummary[];
  campaigns: Campaign[];
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  name: string;
  templateId: string;
  articleId: string;
  campaignId: string;
  values: Record<string, unknown>;
}

export function CharacterSheetsPanel({
  worldId,
  templates,
  articles,
  campaigns,
  onOpenArticle,
  onError,
}: Props) {
  const api = useMemo(() => characterSheetsApi(worldId), [worldId]);
  const [sheets, setSheets] = useState<CharacterSheet[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft | null>(null);
  // '' = all campaigns; a campaign id = only that party's sheets.
  const [filterCampaign, setFilterCampaign] = useState('');

  const refresh = useCallback(async () => {
    try {
      setSheets(await api.list(filterCampaign || undefined));
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, filterCampaign, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const template = templates.find((t) => t.id === draft?.templateId) ?? null;

  function newSheet() {
    if (templates.length === 0) {
      onError(new Error('Create a template first (Templates tab).'));
      return;
    }
    setDraft({
      id: null,
      name: '',
      templateId: templates[0].id,
      articleId: '',
      campaignId: filterCampaign, // default new sheets to the active campaign
      values: {},
    });
  }

  async function open(id: string) {
    try {
      const sheet = await api.get(id);
      setDraft({
        id: sheet.id,
        name: sheet.name,
        templateId: sheet.templateId,
        articleId: sheet.articleId ?? '',
        campaignId: sheet.campaignId ?? '',
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
      campaignId: draft.campaignId || null,
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
        {campaigns.length > 0 && (
          <select
            value={filterCampaign}
            onChange={(e) => setFilterCampaign(e.target.value)}
            title="Filter by campaign"
          >
            <option value="">All campaigns</option>
            {campaigns.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        )}
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
          {loading && <li className="muted">Loading…</li>}
          {!loading && sheets.length === 0 && (
            <li className="muted">No character sheets yet.</li>
          )}
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

            {campaigns.length > 0 && (
              <label className="sheet-article">
                <span className="muted">Campaign</span>
                <select
                  value={draft.campaignId}
                  onChange={(e) => setDraft({ ...draft, campaignId: e.target.value })}
                >
                  <option value="">— shared (no campaign) —</option>
                  {campaigns.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {template && (
              <TemplateForm
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
