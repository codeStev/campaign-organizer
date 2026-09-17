import { useState } from 'react';
import { Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { FoundryCategoryPushMode } from '../api/client';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  onConfirm: (mode: FoundryCategoryPushMode) => void;
}

/** Lets the user pick how a wiki category/world push lays out its articles in
 * Foundry (ADR-0115 addendum) — one Foundry folder per category with a
 * separate JournalEntry per article, or one JournalEntry with a page per
 * article. Neither is a default the other falls back to; the user always
 * chooses explicitly. */
export function FoundryCategoryPushDialog({ open, onOpenChange, title, description, onConfirm }: Props) {
  const [mode, setMode] = useState<FoundryCategoryPushMode>('FOLDER');

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <p className="muted">{description}</p>
        <div className="foundry-push-mode-options">
          <label className="foundry-push-mode-option">
            <input
              type="radio"
              name="foundry-push-mode"
              checked={mode === 'FOLDER'}
              onChange={() => setMode('FOLDER')}
            />
            <span>
              <strong>As separate documents</strong>
              <br />
              <span className="muted">One Foundry folder, one JournalEntry per article.</span>
            </span>
          </label>
          <label className="foundry-push-mode-option">
            <input
              type="radio"
              name="foundry-push-mode"
              checked={mode === 'SINGLE_DOCUMENT'}
              onChange={() => setMode('SINGLE_DOCUMENT')}
            />
            <span>
              <strong>As one document</strong>
              <br />
              <span className="muted">A single JournalEntry with one page per article.</span>
            </span>
          </label>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button type="button" variant="link">
              Cancel
            </Button>
          </DialogClose>
          <Button
            type="button"
            onClick={() => {
              onConfirm(mode);
              onOpenChange(false);
            }}
          >
            Push
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
