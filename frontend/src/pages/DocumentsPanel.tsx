import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { documentsApi, exportDocumentPdf, Document, FieldTemplate, Campaign } from '../api/client';
import { TemplateForm } from '../components/TemplateForm';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// so a meaningfully persistent "none" state goes through this sentinel.
const NONE_VALUE = '__none__';

interface Props {
  worldId: string;
  templates: FieldTemplate[];
  campaigns: Campaign[];
  onChanged: () => void;
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  categoryId: string | null;
  name: string;
  templateId: string;
  campaignId: string;
  values: Record<string, unknown>;
}

export function DocumentsPanel({ worldId, templates, campaigns, onChanged, onError }: Props) {
  const navigate = useNavigate();
  const { documentId: urlDocumentId } = useParams<{ documentId: string }>();
  const api = useMemo(() => documentsApi(worldId), [worldId]);
  const [draft, setDraft] = useState<Draft | null>(null);
  // The last-saved version of the open document, for the Cancel button to revert to.
  const [saved, setSaved] = useState<Document | null>(null);
  // Read (rendered values) vs edit (the form) — mirrors WorldView's article mode.
  const [mode, setMode] = useState<'read' | 'edit'>('read');

  const documentTemplates = templates.filter((t) => t.kind === 'DOCUMENT');
  const template = documentTemplates.find((t) => t.id === draft?.templateId) ?? null;

  function newDocument() {
    if (documentTemplates.length === 0) {
      onError(new Error('Create a document template first (Templates tab).'));
      return;
    }
    setDraft({
      id: null,
      categoryId: null,
      name: '',
      templateId: documentTemplates[0].id,
      campaignId: '',
      values: {},
    });
    setSaved(null);
    setMode('edit');
    navigate(urlDocumentId ? '..' : '.', { relative: 'path' });
  }

  function toDraft(doc: Document): Draft {
    return {
      id: doc.id,
      categoryId: doc.categoryId ?? null,
      name: doc.name,
      templateId: doc.templateId,
      campaignId: doc.campaignId ?? '',
      values: doc.values ?? {},
    };
  }

  const open = useCallback(
    async (id: string) => {
      try {
        const doc = await api.get(id);
        setSaved(doc);
        setDraft(toDraft(doc));
        setMode('read');
      } catch (err) {
        onError(err);
      }
    },
    [api, onError],
  );

  // The URL is the source of truth for which document is open (ADR-0053);
  // "new" is a sentinel the sidebar's "+ New document" button navigates to.
  useEffect(() => {
    if (!urlDocumentId || urlDocumentId === draft?.id) return;
    if (urlDocumentId === 'new') {
      newDocument();
      return;
    }
    void open(urlDocumentId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlDocumentId]);

  async function save() {
    if (!draft) return;
    const wasNew = !draft.id;
    const body = {
      categoryId: draft.categoryId,
      name: draft.name,
      templateId: draft.templateId,
      campaignId: draft.campaignId || null,
      values: draft.values,
    };
    try {
      const savedDoc = draft.id ? await api.update(draft.id, body) : await api.create(body);
      setSaved(savedDoc);
      setDraft(toDraft(savedDoc));
      setMode('read');
      if (wasNew) navigate(savedDoc.id);
      onChanged();
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
      setSaved(null);
      navigate('..', { relative: 'path' });
      onChanged();
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
  );
}
