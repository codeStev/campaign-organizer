import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Label } from './ui/label';
import { Input } from './ui/input';
import { Button } from './ui/button';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  label: string;
  /** Re-applied whenever the dialog opens. */
  defaultValue?: string;
  /** Allows submitting a blank value; required by default. */
  optional?: boolean;
  submitLabel?: string;
  onSubmit: (value: string) => void;
}

/** A single-field text prompt behind a real dialog, replacing `window.prompt`. */
export function PromptDialog({
  open,
  onOpenChange,
  title,
  label,
  defaultValue = '',
  optional = false,
  submitLabel = 'OK',
  onSubmit,
}: Props) {
  const [value, setValue] = useState(defaultValue);

  useEffect(() => {
    if (open) setValue(defaultValue);
    // Only re-seed when the dialog opens, not on every defaultValue change
    // while it's already open (that would fight the user's typing).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  function submit() {
    const trimmed = value.trim();
    if (!optional && trimmed.length === 0) return;
    onSubmit(trimmed);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <Label htmlFor="prompt-dialog-input">{label}</Label>
        <Input
          id="prompt-dialog-input"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          autoFocus
          onKeyDown={(e) => {
            if (e.key === 'Enter') submit();
          }}
        />
        <DialogFooter>
          <DialogClose asChild>
            <Button type="button" variant="link">
              Cancel
            </Button>
          </DialogClose>
          <Button type="button" disabled={!optional && value.trim().length === 0} onClick={submit}>
            {submitLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
