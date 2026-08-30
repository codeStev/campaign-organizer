import { TemplateField, TemplateSection } from '../api/client';
import { MarkdownEditor } from './MarkdownEditor';
import { Input } from './ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Checkbox } from './ui/checkbox';
import { Toggle } from './ui/toggle';
import { renderMarkdown } from '../lib/markdown';

// Radix Select can't use "" as an item value (reserved for "no selection"),
// and a SELECT-type field can genuinely be saved unset, so that state goes
// through this sentinel at the Select boundary.
const NONE_VALUE = '__none__';

interface Props {
  sections: TemplateSection[];
  values: Record<string, unknown>;
  onChange: (values: Record<string, unknown>) => void;
  /** Renders filled-in values (or a template's empty schema) with no editor chrome. */
  readOnly?: boolean;
}

const SPAN: Record<string, number> = { FULL: 12, HALF: 6, THIRD: 4, QUARTER: 3 };

function span(field: TemplateField): number {
  return SPAN[field.width ?? 'FULL'] ?? 12;
}

/** Renders a template's fields, laid out in a 12-col grid (ADR-0030); editable
 *  unless readOnly, in which case it shows values (or an empty schema) with
 *  no interactive widgets. */
export function TemplateForm({ sections, values, onChange, readOnly = false }: Props) {
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
                  {readOnly ? (
                    field.type === 'TEXTAREA' ? (
                      (value as string) ? (
                        <div
                          className="preview-body"
                          dangerouslySetInnerHTML={{ __html: renderMarkdown(value as string) }}
                        />
                      ) : (
                        <p className="muted">(empty)</p>
                      )
                    ) : field.type === 'BOOLEAN' ? (
                      <span className="field-read">{value ? '☑' : '☐'}</span>
                    ) : field.type === 'CIRCLES' ? (
                      <CircleTracker count={field.count ?? 3} filled={Number(value) || 0} readOnly />
                    ) : (
                      <span className="field-read">{(value as string | number) || '—'}</span>
                    )
                  ) : field.type === 'TEXTAREA' ? (
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
                    <Checkbox
                      checked={Boolean(value)}
                      onCheckedChange={(checked) => set(field.key, checked === true)}
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
  onChange?: (n: number) => void;
  readOnly?: boolean;
}

/** A row of n pips; clicking pip i fills 1..i (clicking the last filled clears it). */
function CircleTracker({ count, filled, onChange, readOnly = false }: CircleTrackerProps) {
  return (
    <div className="circle-tracker">
      {Array.from({ length: Math.max(0, count) }, (_, i) => {
        const n = i + 1;
        const on = n <= filled;
        if (readOnly) {
          return <span key={n} className={on ? 'pip on read-only' : 'pip read-only'} title={`${n}`} />;
        }
        return (
          <Toggle
            key={n}
            type="button"
            className={on ? 'pip on' : 'pip'}
            title={`${n}`}
            pressed={on}
            onPressedChange={() => onChange?.(filled === n ? n - 1 : n)}
          />
        );
      })}
    </div>
  );
}
