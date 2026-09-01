import { FieldTemplate, FieldType } from '../api/client';

export interface StatEntry {
  key: string;
  label: string;
  value: unknown;
  type?: FieldType;
}

/** Minimal shape shared by FieldTemplate and GlobalFieldTemplate (ADR-0093) for lookup. */
type TemplateLike = { id: string; sections: FieldTemplate['sections'] };

/** Resolves the one template reference set on a sheet/statblock (ADR-0093: world or global, not both meaningfully). */
export function templateIdOf(entity: {
  worldTemplateId?: string | null;
  globalTemplateId?: string | null;
}): string | null {
  return entity.worldTemplateId ?? entity.globalTemplateId ?? null;
}

/**
 * A statblock's stats as ordered, labeled entries for print (ADR-0052): when it
 * has a template — from either the world or the global catalog (ADR-0093) —
 * fields are ordered and labeled per the template's sections, followed by any
 * stray keys the template doesn't cover; without one, stats print in raw map order.
 */
export function orderedStatEntries(
  stats: Record<string, unknown> | undefined | null,
  templateId: string | undefined | null,
  templates: TemplateLike[],
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
