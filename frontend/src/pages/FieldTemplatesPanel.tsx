import { useEffect, useState } from 'react';
import {
  fieldTemplatesApi,
  builtinFieldTemplatesApi,
  FieldTemplate,
  FieldTemplateRequest,
  BuiltinFieldTemplate,
  TemplateKind,
} from '../api/client';
import { TemplateBuilder } from '../components/TemplateBuilder';

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

  const buildersForKind = builtins.filter((b) => b.kind === newKind);

  async function addFromBuiltin() {
    const b = buildersForKind.find((x) => x.name === choice);
    if (!b) return;
    try {
      await api.create({ name: b.name, kind: b.kind, system: b.system, sections: b.sections });
      setChoice('');
      onChanged();
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
      onChanged();
    } catch (err) {
      onError(err);
    }
  }

  async function remove(t: FieldTemplate) {
    const consequence =
      t.kind === 'CHARACTER'
        ? 'Character sheets using it will be removed too.'
        : 'Statblocks using it will fall back to freeform stats.';
    if (!window.confirm(`Delete "${t.name}"? ${consequence}`)) return;
    try {
      await api.remove(t.id);
      onChanged();
    } catch (err) {
      onError(err);
    }
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
          <select
            value={newKind}
            onChange={(e) => {
              setNewKind(e.target.value as TemplateKind);
              setChoice('');
            }}
          >
            <option value="CHARACTER">Character sheet</option>
            <option value="STATBLOCK">Statblock</option>
          </select>
        </label>
        <select value={choice} onChange={(e) => setChoice(e.target.value)}>
          <option value="">Starter system…</option>
          {buildersForKind.map((b) => (
            <option key={b.name} value={b.name}>
              {b.name}
            </option>
          ))}
        </select>
        <button onClick={addFromBuiltin} disabled={!choice}>
          Add starter
        </button>
        <button
          onClick={() => {
            setEditing(null);
            setBuilding(true);
          }}
        >
          Build new
        </button>
      </div>

      <ul className="article-list">
        {templates.map((t) => (
          <li key={t.id} className="rel-row">
            <button
              className="link-button template-open"
              onClick={() => {
                setEditing(t);
                setBuilding(true);
              }}
            >
              <strong>{t.name}</strong>{' '}
              <small className="muted">
                {KIND_LABEL[t.kind]} · {t.system ?? 'custom'} · {t.sections.length} sections
              </small>
            </button>
            <button className="link-button danger" onClick={() => remove(t)}>
              ✕
            </button>
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
