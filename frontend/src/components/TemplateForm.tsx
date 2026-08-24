import { TemplateField, TemplateSection } from '../api/client';
import { MarkdownEditor } from './MarkdownEditor';
import { Input } from './ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// and a SELECT-type field can genuinely be saved unset, so that state goes
// through this sentinel at the Select boundary.
const NONE_VALUE = '__none__';

interface Props {
  sections: TemplateSection[];
  values: Record<string, unknown>;
  onChange: (values: Record<string, unknown>) => void;
}

const SPAN: Record<string, number> = { FULL: 12, HALF: 6, THIRD: 4, QUARTER: 3 };

function span(field: TemplateField): number {
  return SPAN[field.width ?? 'FULL'] ?? 12;
}

/** Renders an editable form from a template's fields, laid out in a 12-col grid (ADR-0030). */
export function TemplateForm({ sections, values, onChange }: Props) {
  function set(key: string, value: unknown) {
    onChange({ ...values, [key]: value });
  }

  return (
    <div className="sheet-form">
      {sections.map((section) => (
        <fieldset key={section.title} className="sheet-section">
          <legend>{section.title}</legend>
          <div className="sheet-grid">
            {section.fields.map((field) => {
              const value = values[field.key];
              return (
                <label
                  key={field.key}
                  className={`sheet-field field-${field.type.toLowerCase()}`}
                  style={{ gridColumn: `span ${span(field)}` }}
                >
                  <span className="field-label">{field.label}</span>
                  {field.type === 'TEXTAREA' ? (
                    <MarkdownEditor
                      value={(value as string) ?? ''}
                      onChange={(v) => set(field.key, v)}
                    />
                  ) : field.type === 'NUMBER' ? (
                    <Input
                      type="number"
                      value={(value as number | string) ?? ''}
                      onChange={(e) => set(field.key, e.target.value === '' ? null : Number(e.target.value))}
                    />
                  ) : field.type === 'BOOLEAN' ? (
                    <input
                      type="checkbox"
                      checked={Boolean(value)}
                      onChange={(e) => set(field.key, e.target.checked)}
                    />
                  ) : field.type === 'CIRCLES' ? (
                    <CircleTracker
                      count={field.count ?? 3}
                      filled={Number(value) || 0}
                      onChange={(n) => set(field.key, n)}
                    />
                  ) : field.type === 'SELECT' ? (
                    <Select
                      value={(value as string) || NONE_VALUE}
                      onValueChange={(v) => set(field.key, v === NONE_VALUE ? '' : v)}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={NONE_VALUE}>—</SelectItem>
                        {(field.options ?? []).map((opt) => (
                          <SelectItem key={opt} value={opt}>
                            {opt}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  ) : (
                    <Input value={(value as string) ?? ''} onChange={(e) => set(field.key, e.target.value)} />
                  )}
                </label>
              );
            })}
          </div>
        </fieldset>
      ))}
    </div>
  );
}

interface CircleTrackerProps {
  count: number;
  filled: number;
  onChange: (n: number) => void;
}

/** A row of n pips; clicking pip i fills 1..i (clicking the last filled clears it). */
function CircleTracker({ count, filled, onChange }: CircleTrackerProps) {
  return (
    <div className="circle-tracker">
      {Array.from({ length: Math.max(0, count) }, (_, i) => {
        const n = i + 1;
        const on = n <= filled;
        return (
          <button
            key={n}
            type="button"
            className={on ? 'pip on' : 'pip'}
            title={`${n}`}
            onClick={() => onChange(filled === n ? n - 1 : n)}
          />
        );
      })}
    </div>
  );
}
