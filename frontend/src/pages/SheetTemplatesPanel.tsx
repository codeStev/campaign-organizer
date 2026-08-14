import { useEffect, useState } from 'react';
import {
  sheetTemplatesApi,
  builtinSheetTemplatesApi,
  SheetTemplate,
  SheetTemplateRequest,
  BuiltinSheetTemplate,
} from '../api/client';
import { TemplateBuilder } from '../components/TemplateBuilder';

interface Props {
  worldId: string;
  templates: SheetTemplate[];
  loading: boolean;
  onChanged: () => void;
  onError: (err: unknown) => void;
}

export function SheetTemplatesPanel({ worldId, templates, loading, onChanged, onError }: Props) {
  const api = sheetTemplatesApi(worldId);
  const [builtins, setBuiltins] = useState<BuiltinSheetTemplate[]>([]);
  const [choice, setChoice] = useState('');
  // Which template is open in the builder: an existing one, 'new', or null.
  const [editing, setEditing] = useState<SheetTemplate | null>(null);
  const [building, setBuilding] = useState(false);

  useEffect(() => {
    builtinSheetTemplatesApi.list().then(setBuiltins).catch(onError);
  }, [onError]);

  async function addFromBuiltin() {
    const b = builtins.find((x) => x.name === choice);
    if (!b) return;
    try {
      await api.create({ name: b.name, system: b.system, sections: b.sections });
      setChoice('');
      onChanged();
    } catch (err) {
      onError(err);
    }
  }

  async function saveTemplate(body: SheetTemplateRequest) {
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

  async function remove(t: SheetTemplate) {
    if (!window.confirm(`Delete "${t.name}"? Character sheets using it will be removed too.`)) return;
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
      <h3>Sheet templates</h3>
      <div className="editor-actions">
        <select value={choice} onChange={(e) => setChoice(e.target.value)}>
          <option value="">Starter system…</option>
          {builtins.map((b) => (
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
                {t.system ?? 'custom'} · {t.sections.length} sections
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
