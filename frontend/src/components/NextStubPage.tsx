interface Props {
  title: string;
  note?: string;
}

/**
 * Placeholder for a /next screen not yet migrated (docs/ui-overhaul-plan.md
 * Phase 1) — nav is real and reviewable before any feature work starts.
 */
export function NextStubPage({ title, note }: Props) {
  return (
    <div className="card">
      <h1>{title}</h1>
      <p className="muted">{note ?? 'Not migrated yet — see docs/ui-overhaul-plan.md.'}</p>
    </div>
  );
}
