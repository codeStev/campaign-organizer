import { SheetSection } from '../api/client';

interface Props {
  sections: SheetSection[];
  values: Record<string, unknown>;
  onChange: (values: Record<string, unknown>) => void;
}

/** Renders an editable form purely from a template's field definitions (ADR-0024). */
export function SheetForm({ sections, values, onChange }: Props) {
  function set(key: string, value: unknown) {
    onChange({ ...values, [key]: value });
  }

  return (
    <div className="sheet-form">
      {sections.map((section) => (
        <fieldset key={section.title} className="sheet-section">
          <legend>{section.title}</legend>
          <div className="sheet-fields">
            {section.fields.map((field) => {
              const value = values[field.key];
              const common = { id: field.key };
              return (
                <label key={field.key} className={`sheet-field field-${field.type.toLowerCase()}`}>
                  <span className="field-label">{field.label}</span>
                  {field.type === 'TEXTAREA' ? (
                    <textarea
                      {...common}
                      value={(value as string) ?? ''}
                      onChange={(e) => set(field.key, e.target.value)}
                    />
                  ) : field.type === 'NUMBER' ? (
                    <input
                      {...common}
                      type="number"
                      value={(value as number | string) ?? ''}
                      onChange={(e) => set(field.key, e.target.value === '' ? null : Number(e.target.value))}
                    />
                  ) : field.type === 'BOOLEAN' ? (
                    <input
                      {...common}
                      type="checkbox"
                      checked={Boolean(value)}
                      onChange={(e) => set(field.key, e.target.checked)}
                    />
                  ) : field.type === 'SELECT' ? (
                    <select
                      {...common}
                      value={(value as string) ?? ''}
                      onChange={(e) => set(field.key, e.target.value)}
                    >
                      <option value="">—</option>
                      {(field.options ?? []).map((opt) => (
                        <option key={opt} value={opt}>
                          {opt}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      {...common}
                      value={(value as string) ?? ''}
                      onChange={(e) => set(field.key, e.target.value)}
                    />
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
