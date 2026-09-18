import { ReactNode, useState } from 'react';
import { Popover, PopoverContent, PopoverTrigger } from './ui/popover';
import { Button } from './ui/button';
import { ArticleLinkPicker } from './ArticleLinkPicker';

interface Props {
  trigger: ReactNode;
  /** Enables "Change article…" (needs a world to search); omit to only offer Remove. */
  worldId?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The wiki-link's current target (article title/slug). */
  target: string;
  /** Replaces the whole wiki-link with a fresh, unlabeled `[[NewTitle]]`. */
  onRepoint: (title: string) => void;
  onRemove: () => void;
}

/**
 * Edit affordance for an existing `[[wiki-link]]` (ADR-0116) — the
 * counterpart to `LinkPopover` for regular links, since `WikiLink`
 * (wikiLinkExtension.ts) has no editing UI of its own by design. Reuses
 * `ArticleLinkPicker`'s search dialog (already built for table-entry/
 * deck-card bodies) rather than a free-text field, since a wiki-link's
 * target should be a real article.
 */
export function WikiLinkPopover({ trigger, worldId, open, onOpenChange, target, onRepoint, onRemove }: Props) {
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <>
      <Popover open={open} onOpenChange={onOpenChange}>
        <PopoverTrigger asChild>{trigger}</PopoverTrigger>
        <PopoverContent className="link-popover">
          <p className="muted">
            Links to article &ldquo;{target}&rdquo;
          </p>
          <div className="link-popover-actions">
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
            {worldId && (
              <Button
                type="button"
                size="sm"
                onClick={() => {
                  onOpenChange(false);
                  setPickerOpen(true);
                }}
              >
                Change article…
              </Button>
            )}
          </div>
        </PopoverContent>
      </Popover>
      {worldId && (
        <ArticleLinkPicker
          worldId={worldId}
          open={pickerOpen}
          onPick={(title) => {
            onRepoint(title);
            setPickerOpen(false);
          }}
          onClose={() => setPickerOpen(false)}
        />
      )}
    </>
  );
}
