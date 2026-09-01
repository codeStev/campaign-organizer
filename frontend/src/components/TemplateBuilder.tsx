import { useRef, useState } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import {
  FieldTemplate,
  FieldTemplateRequest,
  FieldType,
  FieldWidth,
  TemplateKind,
} from '../api/client';

interface Props {
  initial: FieldTemplate | null;
  kind: TemplateKind;
  onSave: (body: FieldTemplateRequest) => void;
  onCancel: () => void;
}

interface FieldDraft {
  key: string;
  label: string;
  type: FieldType;
  optionsText: string;
  width: FieldWidth;
  count: number;
}

interface SectionDraft {
  title: string;
  fields: FieldDraft[];
}

const PALETTE: { type: FieldType; label: string }[] = [
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

const SPAN: Record<FieldWidth, number> = { FULL: 12, HALF: 6, THIRD: 4, QUARTER: 3 };

type Drag = { kind: 'new'; type: FieldType } | { kind: 'move'; si: number; fi: number };
type DropTarget = { si: number; fi: number | null };

// Find the section/field under a pointer position. Used instead of native HTML5
// drag-and-drop (onDragOver/onDrop), which iOS Safari and most mobile browsers
// don't support, so field placement was previously mouse-only.
function dropTargetAt(x: number, y: number): DropTarget | null {
  const el = document.elementFromPoint(x, y);
  const field = el?.closest<HTMLElement>('[data-drop-field]');
  if (field) return { si: Number(field.dataset.si), fi: Number(field.dataset.fi) };
  const section = el?.closest<HTMLElement>('[data-drop-section]');
  if (section) return { si: Number(section.dataset.si), fi: null };
  return null;
}

function defaultLabel(type: FieldType): string {
  return PALETTE.find((p) => p.type === type)?.label ?? 'Field';
}

function newField(type: FieldType): FieldDraft {
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

function toDrafts(t: FieldTemplate | null): SectionDraft[] {
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

export function TemplateBuilder({ initial, kind, onSave, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '');
  const [system, setSystem] = useState(initial?.system ?? '');
  const [sections, setSections] = useState<SectionDraft[]>(toDrafts(initial));
  const [dropHover, setDropHover] = useState<DropTarget | null>(null);
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

  function beginDrag(e: React.PointerEvent, d: Drag) {
    e.preventDefault();
    drag.current = d;
    e.currentTarget.setPointerCapture(e.pointerId);
  }

  function onBuilderPointerMove(e: React.PointerEvent) {
    if (!drag.current) return;
    setDropHover(dropTargetAt(e.clientX, e.clientY));
  }

  function endDrag(e: React.PointerEvent) {
    const d = drag.current;
    drag.current = null;
    const target = dropTargetAt(e.clientX, e.clientY);
    setDropHover(null);
    if (!d || !target) return;
    if (d.kind === 'new') insertField(target.si, target.fi, newField(d.type));
    else moveField({ si: d.si, fi: d.fi }, target);
  }

  function isHover(si: number, fi: number | null) {
    return dropHover?.si === si && dropHover?.fi === fi;
  }

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
    const body: FieldTemplateRequest = {
      name,
      kind,
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
    <div className="card template-builder" onPointerMove={onBuilderPointerMove} onPointerUp={endDrag}>
      <div className="builder-head">
        <Input className="title-input" placeholder="Template name" value={name} onChange={(e) => setName(e.target.value)} />
        <Input placeholder="system (e.g. homebrew)" value={system} onChange={(e) => setSystem(e.target.value)} />
        <small className="muted">
          {kind === 'CHARACTER' ? 'Character sheet' : kind === 'STATBLOCK' ? 'Statblock' : 'Document'} template
        </small>
      </div>

      <div className="field-palette">
        <span className="muted">Drag a component into a section:</span>
        {PALETTE.map((p) => (
          <div
            key={p.type}
            className="palette-chip"
            onPointerDown={(e) => beginDrag(e, { kind: 'new', type: p.type })}
          >
            + {p.label}
          </div>
        ))}
      </div>

      {sections.map((sec, si) => (
        <fieldset
          key={si}
          className={`builder-section${isHover(si, null) ? ' drop-hover' : ''}`}
          data-drop-section
          data-si={si}
        >
          <legend>
            <Input value={sec.title} onChange={(e) => mutateSection(si, { title: e.target.value })} />
            <Button
              type="button"
              variant="link"
              className="text-destructive hover:text-destructive"
              onClick={() => setSections((s) => s.filter((_, j) => j !== si))}
            >
              ✕ section
            </Button>
          </legend>

          <div className="builder-grid">
            {sec.fields.map((f, fi) => (
              <div
                key={fi}
                className={`builder-field${isHover(si, fi) ? ' drop-hover' : ''}`}
                style={{ gridColumn: `span ${SPAN[f.width]}` }}
                data-drop-field
                data-si={si}
                data-fi={fi}
              >
                <div className="bf-row">
                  <span
                    className="drag-handle"
                    onPointerDown={(e) => beginDrag(e, { kind: 'move', si, fi })}
                    title="Drag to move"
                  >
                    ⠿
                  </span>
                  <Input
                    className="field-label-in"
                    placeholder="Label"
                    value={f.label}
                    onChange={(e) => mutateField(si, fi, { label: e.target.value })}
                  />
                  <Button
                    type="button"
                    variant="link"
                    className="text-destructive hover:text-destructive"
                    onClick={() => mutateSection(si, { fields: sec.fields.filter((_, k) => k !== fi) })}
                  >
                    ✕
                  </Button>
                </div>
                <div className="bf-row">
                  <span className="muted field-type">{defaultLabel(f.type)}</span>
                  <Select
                    value={f.width}
                    onValueChange={(v) => mutateField(si, fi, { width: v as FieldWidth })}
                  >
                    <SelectTrigger title="Width — set two to ½ to place them side by side">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {WIDTHS.map((w) => (
                        <SelectItem key={w.w} value={w.w}>
                          {w.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {f.type === 'CIRCLES' && (
                    <Input
                      type="number"
                      min={1}
                      max={20}
                      className="circle-count"
                      title="Number of circles"
                      value={f.count}
                      onChange={(e) => mutateField(si, fi, { count: Number(e.target.value) })}
                    />
                  )}
                  <span className="bf-spacer" />
                  <Button type="button" variant="link" onClick={() => moveArrow(si, fi, -1)} title="Move earlier">
                    ↑
                  </Button>
                  <Button type="button" variant="link" onClick={() => moveArrow(si, fi, 1)} title="Move later">
                    ↓
                  </Button>
                </div>
                {f.type === 'SELECT' && (
                  <Input
                    className="bf-options"
                    placeholder="options, comma separated"
                    value={f.optionsText}
                    onChange={(e) => mutateField(si, fi, { optionsText: e.target.value })}
                  />
                )}
              </div>
            ))}
          </div>
          <div className="drop-hint muted">Drop components here</div>
        </fieldset>
      ))}

      <div className="editor-actions">
        <Button type="button" variant="link" onClick={() => setSections((s) => [...s, { title: 'Section', fields: [] }])}>
          + Section
        </Button>
        <span style={{ flex: 1 }} />
        <Button onClick={save} disabled={!name}>
          Save template
        </Button>
        <Button type="button" variant="link" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </div>
  );
}
