import { ReactNode, useEffect, useState } from 'react';
import { Popover, PopoverContent, PopoverTrigger } from './ui/popover';
import { Label } from './ui/label';
import { Input } from './ui/input';
import { Button } from './ui/button';

interface Props {
  trigger: ReactNode;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Prefilled with the link's current URL when the cursor is already
   * inside a link; empty when adding a new one. */
  href: string;
  onSave: (href: string) => void;
  /** Only offered when editing an existing link (`href` was non-empty on open). */
  onRemove?: () => void;
}

/** URL-entry popover for the editor's Link toolbar button - both "add a
 * link" and "edit/remove this link" (opens pre-filled when the cursor is
 * already inside one), the same two things a link-editing tooltip plugin
 * would give, built on the app's own Popover instead. */
export function LinkPopover({ trigger, open, onOpenChange, href, onSave, onRemove }: Props) {
  const [value, setValue] = useState(href);

  useEffect(() => {
    if (open) setValue(href);
    // Only re-seed when the popover opens, matching PromptDialog's rule.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function save() {
    const trimmed = value.trim();
    if (!trimmed) return;
    onSave(trimmed);
    onOpenChange(false);
  }

  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>{trigger}</PopoverTrigger>
      <PopoverContent className="link-popover">
        <Label htmlFor="md-link-href">Link URL</Label>
        <Input
          id="md-link-href"
          placeholder="https://…"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          autoFocus
          onKeyDown={(e) => {
            if (e.key === 'Enter') save();
          }}
        />
        <div className="link-popover-actions">
          {onRemove && (
            <Button
              type="button"
              variant="link"
              className="text-destructive hover:text-destructive"
              onClick={() => {
                onRemove();
                onOpenChange(false);
              }}
            >
              Remove
            </Button>
          )}
          <Button type="button" size="sm" disabled={!value.trim()} onClick={save}>
            Save
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
