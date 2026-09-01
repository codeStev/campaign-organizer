import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  characterSheetsApi,
  exportCharacterSheetPdf,
  gameSystemsApi,
  CharacterSheet,
  FieldTemplate,
  GlobalFieldTemplate,
  GameSystem,
  ArticleSummary,
  Campaign,
} from '../api/client';
import { TemplateForm } from '../components/TemplateForm';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Spinner } from '../components/ui/spinner';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so a meaningfully persistent "none" state goes through this sentinel.
const NONE_VALUE = '__none__';

// Encodes which catalog a picked template comes from into the Select's value,
// since the id alone doesn't say whether to set worldTemplateId or globalTemplateId (ADR-0093).
const WORLD_PREFIX = 'world:';
const GLOBAL_PREFIX = 'global:';

interface Props {
  worldId: string;
  templates: FieldTemplate[];
  globalTemplates: GlobalFieldTemplate[];
  articles: ArticleSummary[];
  campaigns: Campaign[];
  onOpenArticle: (id: string) => void;
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  name: string;
  worldTemplateId: string;
  globalTemplateId: string;
  articleId: string;
  campaignId: string;
  values: Record<string, unknown>;
}

export function CharacterSheetsPanel({
  worldId,
  templates,
  globalTemplates,
  articles,
  campaigns,
  onOpenArticle,
  onError,
}: Props) {
  const navigate = useNavigate();
  const { sheetId: urlSheetId } = useParams<{ sheetId: string }>();
  const api = useMemo(() => characterSheetsApi(worldId), [worldId]);
  const [sheets, setSheets] = useState<CharacterSheet[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft | null>(null);
  // '' = all campaigns; a campaign id = only that party's sheets.
  const [filterCampaign, setFilterCampaign] = useState('');
  // Read (rendered values) vs edit (the form) — mirrors WorldView's article mode.
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  const [systems, setSystems] = useState<GameSystem[]>([]);

  useEffect(() => {
    gameSystemsApi.list().then(setSystems).catch(() => {});
  }, []);

  function systemName(systemId: string): string {
    return systems.find((s) => s.id === systemId)?.name ?? '(unknown system)';
  }

  function systemColor(systemId: string): string | null {
    return systems.find((s) => s.id === systemId)?.color ?? null;
  }

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

  const characterTemplates = templates.filter((t) => t.kind === 'CHARACTER');
  const globalCharacterTemplates = globalTemplates.filter((t) => t.kind === 'CHARACTER');
  const template =
    characterTemplates.find((t) => t.id === draft?.worldTemplateId) ??
    globalCharacterTemplates.find((t) => t.id === draft?.globalTemplateId) ??
    null;

  function newSheet() {
    if (characterTemplates.length === 0 && globalCharacterTemplates.length === 0) {
      onError(new Error('Create a character sheet template first (Templates tab).'));
      return;
    }
    setDraft({
      id: null,
      name: '',
      worldTemplateId: characterTemplates[0]?.id ?? '',
      globalTemplateId: characterTemplates.length === 0 ? globalCharacterTemplates[0].id : '',
      articleId: '',
      campaignId: filterCampaign, // default new sheets to the active campaign
      values: {},
    });
    setMode('edit');
    navigate(`/worlds/${worldId}/sheets/characters`);
  }

  function toDraft(sheet: CharacterSheet): Draft {
    return {
      id: sheet.id,
      name: sheet.name,
      worldTemplateId: sheet.worldTemplateId ?? '',
      globalTemplateId: sheet.globalTemplateId ?? '',
      articleId: sheet.articleId ?? '',
      campaignId: sheet.campaignId ?? '',
      values: sheet.values ?? {},
    };
  }

  const open = useCallback(
    async (id: string) => {
      try {
        setDraft(toDraft(await api.get(id)));
        setMode('read');
      } catch (err) {
        onError(err);
      }
    },
    [api, onError],
  );

  // The URL is the source of truth for which sheet is open (ADR-0053).
  useEffect(() => {
    if (!urlSheetId || urlSheetId === draft?.id) return;
    void open(urlSheetId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlSheetId]);

  async function save() {
    if (!draft) return;
    const wasNew = !draft.id;
    const body = {
      name: draft.name,
      worldTemplateId: draft.worldTemplateId || null,
      globalTemplateId: draft.globalTemplateId || null,
      articleId: draft.articleId || null,
      campaignId: draft.campaignId || null,
      values: draft.values,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      setDraft(toDraft(saved));
      setMode('read');
      if (wasNew) navigate(`/worlds/${worldId}/sheets/characters/${saved.id}`);
      await refresh();
      toast.success(`Character sheet "${body.name}" saved`);
    } catch (err) {
      onError(err);
    }
  }

  async function remove() {
    if (!draft?.id) return;
    try {
      await api.remove(draft.id);
      setDraft(null);
      navigate(`/worlds/${worldId}/sheets/characters`);
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
        <Button onClick={newSheet}>+ New sheet</Button>
        {campaigns.length > 0 && (
          <Select
            value={filterCampaign || NONE_VALUE}
            onValueChange={(v) => setFilterCampaign(v === NONE_VALUE ? '' : v)}
          >
            <SelectTrigger title="Filter by campaign">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE_VALUE}>All campaigns</SelectItem>
              {campaigns.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
        <ul className="article-list">
          {sheets.map((s) => (
            <li key={s.id}>
              <button
                className={s.id === draft?.id ? 'article-link active' : 'article-link'}
                onClick={() => navigate(`/worlds/${worldId}/sheets/characters/${s.id}`)}
              >
                <TruncatedLabel label={s.name}>{s.name}</TruncatedLabel>
              </button>
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && sheets.length === 0 && (
            <li className="muted">No character sheets yet.</li>
          )}
        </ul>
      </div>

      <div className="sheet-detail">
        {!draft && <p className="muted">Select or create a character sheet.</p>}
        {draft && mode === 'edit' && (
          <>
            <div className="sheet-head">
              <Input
                className="title-input"
                placeholder="Character name"
                value={draft.name}
                onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              />
              <Select
                value={
                  draft.worldTemplateId
                    ? `${WORLD_PREFIX}${draft.worldTemplateId}`
                    : `${GLOBAL_PREFIX}${draft.globalTemplateId}`
                }
                onValueChange={(v) => {
                  if (v.startsWith(WORLD_PREFIX)) {
                    setDraft({ ...draft, worldTemplateId: v.slice(WORLD_PREFIX.length), globalTemplateId: '' });
                  } else {
                    setDraft({ ...draft, globalTemplateId: v.slice(GLOBAL_PREFIX.length), worldTemplateId: '' });
                  }
                }}
                disabled={draft.id != null}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {characterTemplates.length > 0 && (
                    <SelectGroup>
                      <SelectLabel>This world</SelectLabel>
                      {characterTemplates.map((t) => (
                        <SelectItem key={t.id} value={`${WORLD_PREFIX}${t.id}`}>
                          {t.name}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  )}
                  {globalCharacterTemplates.length > 0 && (
                    <SelectGroup>
                      <SelectLabel>Global (system) templates</SelectLabel>
                      {globalCharacterTemplates.map((t) => (
                        <SelectItem key={t.id} value={`${GLOBAL_PREFIX}${t.id}`}>
                          {systemColor(t.systemId) && (
                            <span className="system-color-dot" style={{ backgroundColor: systemColor(t.systemId)! }} />
                          )}
                          {t.name} · {systemName(t.systemId)}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  )}
                </SelectContent>
              </Select>
            </div>

            <label className="sheet-article">
              <span className="muted">Linked article</span>
              <Select
                value={draft.articleId || NONE_VALUE}
                onValueChange={(v) => setDraft({ ...draft, articleId: v === NONE_VALUE ? '' : v })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NONE_VALUE}>— none —</SelectItem>
                  {articles.map((a) => (
                    <SelectItem key={a.id} value={a.id}>
                      {a.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {draft.articleId && (
                <Button variant="link" onClick={() => onOpenArticle(draft.articleId)}>
                  Open
                </Button>
              )}
            </label>

            {campaigns.length > 0 && (
              <label className="sheet-article">
                <span className="muted">Campaign</span>
                <Select
                  value={draft.campaignId || NONE_VALUE}
                  onValueChange={(v) => setDraft({ ...draft, campaignId: v === NONE_VALUE ? '' : v })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NONE_VALUE}>— shared (no campaign) —</SelectItem>
                    {campaigns.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
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
              <Button onClick={save} disabled={!draft.name}>
                {draft.id ? 'Save sheet' : 'Create sheet'}
              </Button>
              {draft.id && (
                <Button
                  type="button"
                  variant="link"
                  onClick={() => {
                    const saved = sheets.find((s) => s.id === draft.id);
                    if (saved) setDraft(toDraft(saved));
                    setMode('read');
                  }}
                >
                  Cancel
                </Button>
              )}
              {draft.id && template && (
                <Button variant="link" onClick={exportPdf} title="Download a filled fillable PDF">
                  ⭳ Export PDF
                </Button>
              )}
              {draft.id && (
                <ConfirmDeleteDialog
                  trigger={
                    <Button variant="link" className="text-destructive hover:text-destructive">
                      Delete
                    </Button>
                  }
                  title="Delete character sheet?"
                  description={`This permanently deletes "${draft.name}" and cannot be undone.`}
                  onConfirm={remove}
                />
              )}
            </div>
          </>
        )}
        {draft && mode === 'read' && (
          <article className="card article-read">
            <div className="article-read-head">
              <h2>{draft.name}</h2>
              <div className="editor-actions">
                <Button type="button" onClick={() => setMode('edit')}>
                  Edit
                </Button>
                {template && (
                  <Button variant="link" onClick={exportPdf} title="Download a filled fillable PDF">
                    ⭳ Export PDF
                  </Button>
                )}
              </div>
            </div>

            {draft.articleId && (
              <p className="muted">
                Linked article:{' '}
                <Button variant="link" onClick={() => onOpenArticle(draft.articleId)}>
                  {articles.find((a) => a.id === draft.articleId)?.title ?? draft.articleId}
                </Button>
              </p>
            )}
            {draft.campaignId && campaigns.length > 0 && (
              <p className="muted">
                Campaign: {campaigns.find((c) => c.id === draft.campaignId)?.name ?? '—'}
              </p>
            )}

            {template && (
              <TemplateForm sections={template.sections} values={draft.values} onChange={() => {}} readOnly />
            )}
          </article>
        )}
      </div>
    </div>
  );
}
