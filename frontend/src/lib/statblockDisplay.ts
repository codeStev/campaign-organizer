import { FieldTemplate, FieldType } from '../api/client';

export interface StatEntry {
  key: string;
  label: string;
  value: unknown;
  type?: FieldType;
}

/**
 * A statblock's stats as ordered, labeled entries for print (ADR-0052): when it
 * has a template, fields are ordered and labeled per the template's sections,
 * followed by any stray keys the template doesn't cover; without one, stats
 * print in their raw map order.
 */
export function orderedStatEntries(
  stats: Record<string, unknown> | undefined | null,
  templateId: string | undefined | null,
  templates: FieldTemplate[],
): StatEntry[] {
  const values = stats ?? {};
  const template = templateId ? templates.find((t) => t.id === templateId) : undefined;
  if (!template) {
    return Object.entries(values).map(([key, value]) => ({ key, label: key, value }));
  }

  const seen = new Set<string>();
  const entries: StatEntry[] = [];
  for (const section of template.sections) {
    for (const field of section.fields) {
      if (Object.prototype.hasOwnProperty.call(values, field.key)) {
        entries.push({ key: field.key, label: field.label, value: values[field.key], type: field.type });
        seen.add(field.key);
      }
    }
  }
  for (const [key, value] of Object.entries(values)) {
    if (!seen.has(key)) entries.push({ key, label: key, value });
  }
  return entries;
}
