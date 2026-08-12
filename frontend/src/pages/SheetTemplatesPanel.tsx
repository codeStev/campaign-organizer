import { useEffect, useState } from 'react';
import {
  sheetTemplatesApi,
  builtinSheetTemplatesApi,
  SheetTemplate,
  BuiltinSheetTemplate,
} from '../api/client';

interface Props {
  worldId: string;
  templates: SheetTemplate[];
  onChanged: () => void;
  onError: (err: unknown) => void;
}

export function SheetTemplatesPanel({ worldId, templates, onChanged, onError }: Props) {
  const api = sheetTemplatesApi(worldId);
  const [builtins, setBuiltins] = useState<BuiltinSheetTemplate[]>([]);
  const [choice, setChoice] = useState('');

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

  async function addBlank() {
    const name = window.prompt('Template name?');
    if (!name) return;
    try {
      await api.create({ name, sections: [] });
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
        <button className="link-button" onClick={addBlank}>
          Blank template
        </button>
      </div>

      <ul className="article-list">
        {templates.map((t) => (
          <li key={t.id} className="rel-row">
            <span>
              <strong>{t.name}</strong>{' '}
              <small className="muted">
                {t.system ?? 'custom'} · {t.sections.length} sections
              </small>
            </span>
            <button className="link-button danger" onClick={() => remove(t)}>
              ✕
            </button>
          </li>
        ))}
        {templates.length === 0 && <li className="muted">No templates yet. Add a starter above.</li>}
      </ul>
    </div>
  );
}
