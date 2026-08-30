import { CSSProperties, useState } from 'react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { useNewWindowContainer } from './NewWindowPortal';

export type PaperSize = 'letter' | 'a4';
export type PaperMargin = 'normal' | 'narrow';

export interface PrintOptions {
  paper: PaperSize;
  margin: PaperMargin;
  fontScale: number;
}

const DEFAULT_OPTIONS: PrintOptions = { paper: 'letter', margin: 'normal', fontScale: 1 };

const FONT_SCALES: { value: string; label: string }[] = [
  { value: '0.85', label: 'Small text' },
  { value: '1', label: 'Normal text' },
  { value: '1.15', label: 'Large text' },
];

/**
 * Shared paper size / margin / text size state for a print view. Paper and
 * margin drive named `@page` rules (index.css); font scale drives `zoom` on
 * `.print-doc` directly, since rem-based font sizes throughout the print CSS
 * would otherwise need converting to em to respond to a scale variable.
 */
export function usePrintOptions() {
  const [opts, setOpts] = useState<PrintOptions>(DEFAULT_OPTIONS);
  const docProps: { 'data-paper': PaperSize; 'data-margin': PaperMargin; style: CSSProperties } = {
    'data-paper': opts.paper,
    'data-margin': opts.margin,
    style: { zoom: opts.fontScale },
  };
  return { opts, setOpts, docProps };
}

interface Props {
  opts: PrintOptions;
  onChange: (opts: PrintOptions) => void;
}

/**
 * Paper size / margin / text size controls for a print-view toolbar. A
 * separate component (not inlined in the view) so useNewWindowContainer()
 * runs as a descendant of NewWindowPortal's provider — see the identical
 * note in PrintView.tsx/MapPrintView.tsx.
 */
export function PrintOptionsMenu({ opts, onChange }: Props) {
  const container = useNewWindowContainer();
  return (
    <>
      <Select value={opts.paper} onValueChange={(v) => onChange({ ...opts, paper: v as PaperSize })}>
        <SelectTrigger title="Paper size">
          <SelectValue />
        </SelectTrigger>
        <SelectContent container={container}>
          <SelectItem value="letter">Letter</SelectItem>
          <SelectItem value="a4">A4</SelectItem>
        </SelectContent>
      </Select>
      <Select value={opts.margin} onValueChange={(v) => onChange({ ...opts, margin: v as PaperMargin })}>
        <SelectTrigger title="Margins">
          <SelectValue />
        </SelectTrigger>
        <SelectContent container={container}>
          <SelectItem value="normal">Normal margins</SelectItem>
          <SelectItem value="narrow">Narrow margins</SelectItem>
        </SelectContent>
      </Select>
      <Select
        value={String(opts.fontScale)}
        onValueChange={(v) => onChange({ ...opts, fontScale: Number(v) })}
      >
        <SelectTrigger title="Text size">
          <SelectValue />
        </SelectTrigger>
        <SelectContent container={container}>
          {FONT_SCALES.map((f) => (
            <SelectItem key={f.value} value={f.value}>
              {f.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </>
  );
}
