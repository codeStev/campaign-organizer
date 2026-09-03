import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { documentsApi, exportDocumentPdf, Document, FieldTemplate, Campaign } from '../api/client';
import { TemplateForm } from '../components/TemplateForm';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Spinner } from '../components/ui/spinner';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so a meaningfully persistent "none" state goes through this sentinel.
const NONE_VALUE = '__none__';

interface Props {
  worldId: string;
  templates: FieldTemplate[];
  campaigns: Campaign[];
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  name: string;
  templateId: string;
  campaignId: string;
  values: Record<string, unknown>;
}

export function DocumentsPanel({ worldId, templates, campaigns, onError }: Props) {
  const navigate = useNavigate();
  const { documentId: urlDocumentId } = useParams<{ documentId: string }>();
  const api = useMemo(() => documentsApi(worldId), [worldId]);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft | null>(null);
  // '' = all campaigns; a campaign id = only that campaign's documents.
  const [filterCampaign, setFilterCampaign] = useState('');
  // Read (rendered values) vs edit (the form) — mirrors WorldView's article mode.
  const [mode, setMode] = useState<'read' | 'edit'>('read');

  const refresh = useCallback(async () => {
    try {
      setDocuments(await api.list({ campaignId: filterCampaign || undefined }));
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, filterCampaign, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const documentTemplates = templates.filter((t) => t.kind === 'DOCUMENT');
  const template = documentTemplates.find((t) => t.id === draft?.templateId) ?? null;

  function newDocument() {
    if (documentTemplates.length === 0) {
      onError(new Error('Create a document template first (Templates tab).'));
      return;
    }
    setDraft({
      id: null,
      name: '',
      templateId: documentTemplates[0].id,
      campaignId: filterCampaign, // default new documents to the active campaign
      values: {},
    });
    setMode('edit');
    navigate(urlDocumentId ? '..' : '.', { relative: 'path' });
  }

  function toDraft(doc: Document): Draft {
    return {
      id: doc.id,
      name: doc.name,
      templateId: doc.templateId,
      campaignId: doc.campaignId ?? '',
      values: doc.values ?? {},
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

  // The URL is the source of truth for which document is open (ADR-0053).
  useEffect(() => {
    if (!urlDocumentId || urlDocumentId === draft?.id) return;
    void open(urlDocumentId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlDocumentId]);

  async function save() {
    if (!draft) return;
    const wasNew = !draft.id;
    const body = {
      name: draft.name,
      templateId: draft.templateId,
      campaignId: draft.campaignId || null,
      values: draft.values,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      setDraft(toDraft(saved));
      setMode('read');
      if (wasNew) navigate(saved.id);
      await refresh();
      toast.success(`Document "${body.name}" saved`);
    } catch (err) {
      onError(err);
    }
  }

  async function remove() {
    if (!draft?.id) return;
    try {
      await api.remove(draft.id);
      setDraft(null);
      navigate('..', { relative: 'path' });
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function exportPdf() {
    if (!draft?.id) return;
    try {
      await exportDocumentPdf(worldId, draft.id);
    } catch (err) {
      onError(err);
    }
  }

  return (
    <div className="sheets-panel">
      <div className="sheets-list-col">
        <Button onClick={newDocument}>+ New document</Button>
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
          {documents.map((d) => (
            <li key={d.id}>
              <button
                className={d.id === draft?.id ? 'article-link active' : 'article-link'}
                onClick={() => navigate(urlDocumentId ? `../${d.id}` : d.id, { relative: 'path' })}
              >
                <TruncatedLabel label={d.name}>{d.name}</TruncatedLabel>
              </button>
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && documents.length === 0 && <li className="muted">No documents yet.</li>}
        </ul>
      </div>

      <div className="sheet-detail">
        {!draft && <p className="muted">Select or create a document.</p>}
        {draft && mode === 'edit' && (
          <>
            <div className="sheet-head">
              <Input
                className="title-input"
                placeholder="Document name"
                value={draft.name}
                onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              />
              <Select
                value={draft.templateId}
                onValueChange={(v) => setDraft({ ...draft, templateId: v })}
                disabled={draft.id != null}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {documentTemplates.map((t) => (
                    <SelectItem key={t.id} value={t.id}>
                      {t.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

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
                {draft.id ? 'Save document' : 'Create document'}
              </Button>
              {draft.id && (
                <Button
                  type="button"
                  variant="link"
                  onClick={() => {
                    const saved = documents.find((d) => d.id === draft.id);
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
                  title="Delete document?"
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
