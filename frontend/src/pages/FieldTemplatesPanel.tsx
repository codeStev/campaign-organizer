import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fieldTemplatesApi,
  builtinFieldTemplatesApi,
  FieldTemplate,
  FieldTemplateRequest,
  BuiltinFieldTemplate,
  TemplateKind,
} from '../api/client';
import { TemplateBuilder } from '../components/TemplateBuilder';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';

interface Props {
  worldId: string;
  templates: FieldTemplate[];
  loading: boolean;
  onChanged: () => void;
  onError: (err: unknown) => void;
}

const KIND_LABEL: Record<TemplateKind, string> = {
  CHARACTER: 'Character sheet',
  STATBLOCK: 'Statblock',
};

export function FieldTemplatesPanel({ worldId, templates, loading, onChanged, onError }: Props) {
  const navigate = useNavigate();
  const { templateId: urlTemplateId } = useParams<{ templateId: string }>();
  const api = fieldTemplatesApi(worldId);
  const [builtins, setBuiltins] = useState<BuiltinFieldTemplate[]>([]);
  // What kind of template a new one (starter or built-from-scratch) will be.
  const [newKind, setNewKind] = useState<TemplateKind>('CHARACTER');
  const [choice, setChoice] = useState('');
  // Which template is open in the builder: an existing one, 'new', or null.
  const [editing, setEditing] = useState<FieldTemplate | null>(null);
  const [building, setBuilding] = useState(false);

  useEffect(() => {
    builtinFieldTemplatesApi.list().then(setBuiltins).catch(onError);
  }, [onError]);

  // The URL is the source of truth for which template is open in the builder
  // (ADR-0053); "Build new" has no id and stays purely local state.
  useEffect(() => {
    if (!urlTemplateId || urlTemplateId === editing?.id) return;
    const found = templates.find((t) => t.id === urlTemplateId);
    if (found) {
      setEditing(found);
      setBuilding(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlTemplateId, templates]);

  const buildersForKind = builtins.filter((b) => b.kind === newKind);

  async function addFromBuiltin() {
    const b = buildersForKind.find((x) => x.name === choice);
    if (!b) return;
    try {
      await api.create({ name: b.name, kind: b.kind, system: b.system, sections: b.sections });
      setChoice('');
      onChanged();
      toast.success(`Template "${b.name}" added`);
    } catch (err) {
      onError(err);
    }
  }

  async function saveTemplate(body: FieldTemplateRequest) {
    try {
      if (editing) await api.update(editing.id, body);
      else await api.create(body);
      setBuilding(false);
      setEditing(null);
      navigate(`/worlds/${worldId}/sheets/templates`);
      onChanged();
      toast.success('Template saved');
    } catch (err) {
      onError(err);
    }
  }

  async function remove(t: FieldTemplate) {
    try {
      await api.remove(t.id);
      onChanged();
    } catch (err) {
      onError(err);
    }
  }

  function deleteConsequence(t: FieldTemplate): string {
    return t.kind === 'CHARACTER'
      ? 'Character sheets using it will be removed too.'
      : 'Statblocks using it will fall back to freeform stats.';
  }

  if (building) {
    return (
      <TemplateBuilder
        initial={editing}
        kind={editing?.kind ?? newKind}
        onSave={saveTemplate}
        onCancel={() => {
          setBuilding(false);
          setEditing(null);
          navigate(`/worlds/${worldId}/sheets/templates`);
        }}
      />
    );
  }

  return (
    <div className="card">
      <h3>Field templates</h3>
      <div className="editor-actions">
        <label className="muted">
          What are you building?{' '}
          <Select
            value={newKind}
            onValueChange={(v) => {
              setNewKind(v as TemplateKind);
              setChoice('');
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="CHARACTER">Character sheet</SelectItem>
              <SelectItem value="STATBLOCK">Statblock</SelectItem>
            </SelectContent>
          </Select>
        </label>
        <Select value={choice} onValueChange={setChoice}>
          <SelectTrigger>
            <SelectValue placeholder="Starter system…" />
          </SelectTrigger>
          <SelectContent>
            {buildersForKind.map((b) => (
              <SelectItem key={b.name} value={b.name}>
                {b.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button onClick={addFromBuiltin} disabled={!choice}>
          Add starter
        </Button>
        <Button
          onClick={() => {
            setEditing(null);
            setBuilding(true);
          }}
        >
          Build new
        </Button>
      </div>

      <ul className="article-list">
        {templates.map((t) => (
          <li key={t.id} className="rel-row">
            <Button
              variant="link"
              className="template-open"
              onClick={() => navigate(`/worlds/${worldId}/sheets/templates/${t.id}`)}
            >
              <strong>{t.name}</strong>{' '}
              <small className="muted">
                {KIND_LABEL[t.kind]} · {t.system ?? 'custom'} · {t.sections.length} sections
              </small>
            </Button>
            <ConfirmDeleteDialog
              trigger={
                <Button variant="link" className="text-destructive hover:text-destructive">
                  ✕
                </Button>
              }
              title="Delete template?"
              description={`This deletes "${t.name}". ${deleteConsequence(t)}`}
              onConfirm={() => remove(t)}
            />
          </li>
        ))}
        {loading && <li className="muted">Loading…</li>}
        {!loading && templates.length === 0 && (
          <li className="muted">No templates yet. Add a starter or build one.</li>
        )}
      </ul>
    </div>
  );
}
