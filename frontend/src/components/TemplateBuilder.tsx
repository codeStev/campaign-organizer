import { useState } from 'react';
import { SheetTemplate, SheetTemplateRequest, SheetFieldType } from '../api/client';

interface Props {
  initial: SheetTemplate | null;
  onSave: (body: SheetTemplateRequest) => void;
  onCancel: () => void;
}

interface FieldDraft {
  key: string;
  label: string;
  type: SheetFieldType;
  optionsText: string;
}

interface SectionDraft {
  title: string;
  fields: FieldDraft[];
}

const TYPES: { type: SheetFieldType; label: string }[] = [
  { type: 'TEXT', label: 'Text' },
  { type: 'TEXTAREA', label: 'Text area' },
  { type: 'NUMBER', label: 'Number' },
  { type: 'BOOLEAN', label: 'Checkbox' },
  { type: 'SELECT', label: 'Dropdown' },
];

function slugKey(label: string, existing: Set<string>): string {
  const base =
    label
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') || 'field';
  let key = base;
  let i = 2;
  while (existing.has(key)) key = `${base}_${i++}`;
  return key;
}

function toDrafts(t: SheetTemplate | null): SectionDraft[] {
  if (!t) return [];
  return t.sections.map((s) => ({
    title: s.title,
    fields: s.fields.map((f) => ({
      key: f.key,
      label: f.label,
      type: f.type,
      optionsText: (f.options ?? []).join(', '),
    })),
  }));
}

export function TemplateBuilder({ initial, onSave, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '');
  const [system, setSystem] = useState(initial?.system ?? '');
  const [sections, setSections] = useState<SectionDraft[]>(toDrafts(initial));

  function mutateSection(i: number, patch: Partial<SectionDraft>) {
    setSections((s) => s.map((sec, j) => (j === i ? { ...sec, ...patch } : sec)));
  }

  function mutateField(si: number, fi: number, patch: Partial<FieldDraft>) {
    setSections((s) =>
      s.map((sec, j) =>
        j === si ? { ...sec, fields: sec.fields.map((f, k) => (k === fi ? { ...f, ...patch } : f)) } : sec,
      ),
    );
  }

  function addSection() {
    setSections((s) => [...s, { title: 'New section', fields: [] }]);
  }

  function addField(si: number) {
    setSections((s) =>
      s.map((sec, j) => {
        if (j !== si) return sec;
        const used = new Set(sec.fields.map((f) => f.key));
        return {
          ...sec,
          fields: [...sec.fields, { key: slugKey('field', used), label: 'New field', type: 'TEXT', optionsText: '' }],
        };
      }),
    );
  }

  function moveField(si: number, fi: number, dir: -1 | 1) {
    setSections((s) =>
      s.map((sec, j) => {
        if (j !== si) return sec;
        const fields = [...sec.fields];
        const to = fi + dir;
        if (to < 0 || to >= fields.length) return sec;
        [fields[fi], fields[to]] = [fields[to], fields[fi]];
        return { ...sec, fields };
      }),
    );
  }

  function save() {
    // Auto-derive a key from the label when the user left it blank.
    const body: SheetTemplateRequest = {
      name,
      system: system || null,
      sections: sections.map((sec) => {
        const used = new Set<string>();
        return {
          title: sec.title,
          fields: sec.fields.map((f) => {
            const key = f.key.trim() || slugKey(f.label, used);
            used.add(key);
            return {
              key,
              label: f.label,
              type: f.type,
              options: f.type === 'SELECT'
                ? f.optionsText.split(',').map((o) => o.trim()).filter(Boolean)
                : null,
            };
          }),
        };
      }),
    };
    onSave(body);
  }

  return (
    <div className="card template-builder">
      <div className="builder-head">
        <input className="title-input" placeholder="Template name" value={name} onChange={(e) => setName(e.target.value)} />
        <input placeholder="system (e.g. homebrew)" value={system} onChange={(e) => setSystem(e.target.value)} />
      </div>

      {sections.map((sec, si) => (
        <fieldset key={si} className="builder-section">
          <legend>
            <input value={sec.title} onChange={(e) => mutateSection(si, { title: e.target.value })} />
            <button
              type="button"
              className="link-button danger"
              onClick={() => setSections((s) => s.filter((_, j) => j !== si))}
            >
              ✕ section
            </button>
          </legend>

          {sec.fields.map((f, fi) => (
            <div key={fi} className="builder-field">
              <input
                className="field-label-in"
                placeholder="Label"
                value={f.label}
                onChange={(e) => mutateField(si, fi, { label: e.target.value })}
              />
              <select value={f.type} onChange={(e) => mutateField(si, fi, { type: e.target.value as SheetFieldType })}>
                {TYPES.map((t) => (
                  <option key={t.type} value={t.type}>
                    {t.label}
                  </option>
                ))}
              </select>
              {f.type === 'SELECT' && (
                <input
                  placeholder="options, comma separated"
                  value={f.optionsText}
                  onChange={(e) => mutateField(si, fi, { optionsText: e.target.value })}
                />
              )}
              <button type="button" className="link-button" onClick={() => moveField(si, fi, -1)} title="Move up">
                ↑
              </button>
              <button type="button" className="link-button" onClick={() => moveField(si, fi, 1)} title="Move down">
                ↓
              </button>
              <button
                type="button"
                className="link-button danger"
                onClick={() => mutateSection(si, { fields: sec.fields.filter((_, k) => k !== fi) })}
              >
                ✕
              </button>
            </div>
          ))}
          <button type="button" className="link-button" onClick={() => addField(si)}>
            + Field
          </button>
        </fieldset>
      ))}

      <div className="editor-actions">
        <button type="button" className="link-button" onClick={addSection}>
          + Section
        </button>
        <span style={{ flex: 1 }} />
        <button onClick={save} disabled={!name}>
          Save template
        </button>
        <button type="button" className="link-button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </div>
  );
}
