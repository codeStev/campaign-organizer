import { useMemo } from 'react';
import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from './ui/command';

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

/** Ctrl/Cmd-K "jump to anything" palette, built on cmdk (via shadcn's Command/Dialog). */
export function CommandPalette({ open, commands, onClose }: Props) {
  const groups = useMemo(() => {
    const byGroup = new Map<string, Command[]>();
    for (const c of commands) {
      const list = byGroup.get(c.group) ?? [];
      list.push(c);
      byGroup.set(c.group, list);
    }
    return [...byGroup.entries()];
  }, [commands]);

  return (
    <CommandDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
      title="Jump to anything"
      description="Search articles and views"
    >
      <Command>
        <CommandInput placeholder="Jump to an article or view…" />
        <CommandList>
          <CommandEmpty>No matches.</CommandEmpty>
          {groups.map(([group, cmds]) => (
            <CommandGroup key={group} heading={group}>
              {cmds.map((c) => (
                <CommandItem
                  key={c.id}
                  value={`${c.label} ${c.keywords ?? ''}`}
                  onSelect={() => {
                    c.run();
                    onClose();
                  }}
                >
                  {c.label}
                </CommandItem>
              ))}
            </CommandGroup>
          ))}
        </CommandList>
      </Command>
    </CommandDialog>
  );
}
