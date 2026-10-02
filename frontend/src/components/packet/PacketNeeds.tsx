import { NeedChip } from '../../lib/packetLayout';

/** A beat's "Needs:" line: chips for the material it links to, tagged with the section letter. */
export function PacketNeeds({ chips }: { chips: NeedChip[] }) {
  if (chips.length === 0) return null;
  return (
    <div className="print-needs">
      <span className="print-needs-label">Needs:</span>
      {chips.map((c) => (
        <a key={c.key} className="print-chip" href={`#${c.anchor}`}>
          {c.label}
          <b>{c.letter}</b>
        </a>
      ))}
    </div>
  );
}
