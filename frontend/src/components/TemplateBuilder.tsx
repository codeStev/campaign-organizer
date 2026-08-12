import { DragEvent, useRef, useState } from 'react';
import {
  SheetTemplate,
  SheetTemplateRequest,
  SheetFieldType,
  FieldWidth,
} from '../api/client';

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
  width: FieldWidth;
  count: number;
}

interface SectionDraft {
  title: string;
  fields: FieldDraft[];
}

const PALETTE: { type: SheetFieldType; label: string }[] = [
  { type: 'TEXT', label: 'Text' },
  { type: 'TEXTAREA', label: 'Text area' },
  { type: 'NUMBER', label: 'Number' },
  { type: 'BOOLEAN', label: 'Checkbox' },
  { type: 'SELECT', label: 'Dropdown' },
  { type: 'CIRCLES', label: 'Circle list' },
];

const WIDTHS: { w: FieldWidth; label: string }[] = [
  { w: 'FULL', label: 'Full' },
  { w: 'HALF', label: '½' },
  { w: 'THIRD', label: '⅓' },
  { w: 'QUARTER', label: '¼' },
];

type Drag = { kind: 'new'; type: SheetFieldType } | { kind: 'move'; si: number; fi: number };

function defaultLabel(type: SheetFieldType): string {
  return PALETTE.find((p) => p.type === type)?.label ?? 'Field';
}

function newField(type: SheetFieldType): FieldDraft {
  return { key: '', label: defaultLabel(type), type, optionsText: '', width: 'FULL', count: 3 };
}

function slugKey(label: string, existing: Set<string>): string {
  const base =
    label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '') || 'field';
  let key = base;
  let i = 2;
  while (existing.has(key)) key = `${base}_${i++}`;
  return key;
}

function toDrafts(t: SheetTemplate | null): SectionDraft[] {
  if (!t) return [{ title: 'Section', fields: [] }];
  return t.sections.map((s) => ({
    title: s.title,
    fields: s.fields.map((f) => ({
      key: f.key,
      label: f.label,
      type: f.type,
      optionsText: (f.options ?? []).join(', '),
      width: f.width ?? 'FULL',
      count: f.count ?? 3,
    })),
  }));
}

export function TemplateBuilder({ initial, onSave, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '');
  const [system, setSystem] = useState(initial?.system ?? '');
  const [sections, setSections] = useState<SectionDraft[]>(toDrafts(initial));
  const drag = useRef<Drag | null>(null);

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

  // Insert a field into section `si` at `fi` (or append when fi is null).
  function insertField(si: number, fi: number | null, field: FieldDraft) {
    setSections((s) =>
      s.map((sec, j) => {
        if (j !== si) return sec;
        const fields = [...sec.fields];
        fields.splice(fi ?? fields.length, 0, field);
        return { ...sec, fields };
      }),
    );
  }

  // Move an existing field to a target position, handling cross-section moves.
  function moveField(from: { si: number; fi: number }, to: { si: number; fi: number | null }) {
    setSections((s) => {
      const copy = s.map((sec) => ({ ...sec, fields: [...sec.fields] }));
      const [moved] = copy[from.si].fields.splice(from.fi, 1);
      if (!moved) return s;
      let idx = to.fi ?? copy[to.si].fields.length;
      if (from.si === to.si && from.fi < idx) idx -= 1; // account for the removal
      copy[to.si].fields.splice(idx, 0, moved);
      return copy;
    });
  }

  function handleDrop(si: number, fi: number | null, e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    const d = drag.current;
    drag.current = null;
    if (!d) return;
    if (d.kind === 'new') insertField(si, fi, newField(d.type));
    else moveField({ si: d.si, fi: d.fi }, { si, fi });
  }

  const allowDrop = (e: DragEvent) => e.preventDefault();

  function moveArrow(si: number, fi: number, dir: -1 | 1) {
    const to = fi + dir;
    setSections((s) =>
      s.map((sec, j) => {
        if (j !== si) return sec;
        if (to < 0 || to >= sec.fields.length) return sec;
        const fields = [...sec.fields];
        [fields[fi], fields[to]] = [fields[to], fields[fi]];
        return { ...sec, fields };
      }),
    );
  }

  function save() {
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
              options:
                f.type === 'SELECT'
                  ? f.optionsText.split(',').map((o) => o.trim()).filter(Boolean)
                  : null,
              width: f.width,
              count: f.type === 'CIRCLES' ? Math.max(1, Math.min(20, f.count || 3)) : null,
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

      <div className="palette">
        <span className="muted">Drag a component into a section:</span>
        {PALETTE.map((p) => (
          <div
            key={p.type}
            className="palette-chip"
            draggable
            onDragStart={() => (drag.current = { kind: 'new', type: p.type })}
          >
            + {p.label}
          </div>
        ))}
      </div>

      {sections.map((sec, si) => (
        <fieldset key={si} className="builder-section" onDragOver={allowDrop} onDrop={(e) => handleDrop(si, null, e)}>
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
            <div
              key={fi}
              className="builder-field"
              onDragOver={allowDrop}
              onDrop={(e) => handleDrop(si, fi, e)}
            >
              <span
                className="drag-handle"
                draggable
                onDragStart={() => (drag.current = { kind: 'move', si, fi })}
                title="Drag to move"
              >
                ⠿
              </span>
              <input
                className="field-label-in"
                placeholder="Label"
                value={f.label}
                onChange={(e) => mutateField(si, fi, { label: e.target.value })}
              />
              <span className="muted field-type">{defaultLabel(f.type)}</span>
              <select
                value={f.width}
                title="Width"
                onChange={(e) => mutateField(si, fi, { width: e.target.value as FieldWidth })}
              >
                {WIDTHS.map((w) => (
                  <option key={w.w} value={w.w}>
                    {w.label}
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
              {f.type === 'CIRCLES' && (
                <input
                  type="number"
                  min={1}
                  max={20}
                  className="circle-count"
                  title="Number of circles"
                  value={f.count}
                  onChange={(e) => mutateField(si, fi, { count: Number(e.target.value) })}
                />
              )}
              <button type="button" className="link-button" onClick={() => moveArrow(si, fi, -1)} title="Up">
                ↑
              </button>
              <button type="button" className="link-button" onClick={() => moveArrow(si, fi, 1)} title="Down">
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
          <div className="drop-hint muted">Drop components here</div>
        </fieldset>
      ))}

      <div className="editor-actions">
        <button type="button" className="link-button" onClick={() => setSections((s) => [...s, { title: 'Section', fields: [] }])}>
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
