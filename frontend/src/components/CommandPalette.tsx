import { useEffect, useMemo, useRef, useState } from 'react';
import { Input } from './ui/input';

export interface Command {
  id: string;
  label: string;
  group: string;
  /** Extra text matched by search but not shown. */
  keywords?: string;
  run: () => void;
}

interface Props {
  open: boolean;
  commands: Command[];
  onClose: () => void;
}

const MAX_RESULTS = 50;

/** Ctrl/Cmd-K "jump to anything" palette: fuzzy-ish substring match + keyboard nav. */
export function CommandPalette({ open, commands, onClose }: Props) {
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  // Reset and focus each time the palette opens.
  useEffect(() => {
    if (open) {
      setQuery('');
      setSelected(0);
      // Focus after the element is actually mounted/painted.
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  }, [open]);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    const matches = q
      ? commands.filter(
          (c) =>
            c.label.toLowerCase().includes(q) ||
            (c.keywords ?? '').toLowerCase().includes(q),
        )
      : commands;
    return matches.slice(0, MAX_RESULTS);
  }, [commands, query]);

  // Keep the selection in range as the result set changes.
  useEffect(() => {
    setSelected((s) => Math.min(s, Math.max(results.length - 1, 0)));
  }, [results.length]);

  // Scroll the active row into view.
  useEffect(() => {
    const el = listRef.current?.children[selected] as HTMLElement | undefined;
    el?.scrollIntoView({ block: 'nearest' });
  }, [selected]);

  if (!open) return null;

  function runAt(index: number) {
    const cmd = results[index];
    if (cmd) {
      cmd.run();
      onClose();
    }
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelected((s) => (results.length ? (s + 1) % results.length : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelected((s) => (results.length ? (s - 1 + results.length) % results.length : 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      runAt(selected);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  }

  return (
    <div className="palette-overlay" onMouseDown={onClose}>
      <div className="palette" onMouseDown={(e) => e.stopPropagation()}>
        <Input
          ref={inputRef}
          className="palette-input"
          type="text"
          placeholder="Jump to an article or view…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={onKeyDown}
        />
        <ul className="palette-list" ref={listRef}>
          {results.map((c, i) => {
            const prev = results[i - 1];
            const showGroup = !prev || prev.group !== c.group;
            return (
              <li
                key={c.id}
                className={i === selected ? 'palette-item active' : 'palette-item'}
                onMouseMove={() => setSelected(i)}
                onClick={() => runAt(i)}
              >
                {showGroup && <span className="palette-group">{c.group}</span>}
                <span className="palette-label">{c.label}</span>
              </li>
            );
          })}
          {results.length === 0 && <li className="palette-empty muted">No matches.</li>}
        </ul>
      </div>
    </div>
  );
}
