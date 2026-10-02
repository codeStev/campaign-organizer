import { PaperMargin, PaperSize } from '../PrintOptionsMenu';
import { PacketSection } from '../../lib/packetLayout';

interface Props {
  paper: PaperSize;
  margin: PaperMargin;
  sections: PacketSection[];
  sessionTitle: string;
}

const FOOTER = `@bottom-center {
    content: counter(page) ' / ' counter(pages);
    font-family: Georgia, 'Times New Roman', serif;
    font-size: 9pt;
    color: #666;
  }`;

/** Escapes text for use inside a double-quoted CSS string. */
function cssString(text: string): string {
  return `"${text.replace(/[\\"]/g, '\\$&').replace(/[\r\n]+/g, ' ')}"`;
}

/**
 * Running headers (ADR-0118): one named @page per lettered section, so each
 * page carries its section's letter and name. Generated for the current paper
 * size and margins because a named page replaces the generic `.print-doc`
 * page rule; the page-number footer is repeated in each.
 */
export function PacketPageStyles({ paper, margin, sections, sessionTitle }: Props) {
  const size = paper === 'a4' ? 'A4' : 'letter';
  const pageMargin = margin === 'narrow' ? '1.2cm' : '2cm';
  const headerFont = `font-family: Georgia, 'Times New Roman', serif; font-size: 9pt; color: #666;`;

  const page = (name: string, header?: string) => `
@page ${name} {
  size: ${size};
  margin: ${pageMargin};
  ${
    header
      ? `@top-left { content: ${cssString(header)}; ${headerFont} font-weight: 600; }
  @top-right { content: ${cssString(sessionTitle)}; ${headerFont} }`
      : ''
  }
  ${FOOTER}
}
.${name} { page: ${name}; }`;

  const css = [
    page('pk-front'),
    ...sections.map((s) => page(`pk-${s.id}`, `${s.letter} · ${s.title}`)),
  ].join('\n');

  return <style>{css}</style>;
}
