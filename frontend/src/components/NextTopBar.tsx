import { ReactNode } from 'react';
import { Menu } from 'lucide-react';
import { NextWorldSwitcher } from './NextWorldSwitcher';
import { Button } from './ui/button';

interface Props {
  currentWorldId?: string;
  currentWorldName?: string;
  rightSlot?: ReactNode;
  /** Opens the mobile nav drawer — omitted (no button rendered) if the
   * caller has nothing to open, e.g. a screen with no sidebar at all. */
  onMenuClick?: () => void;
}

/**
 * /next's persistent header row, matching the reviewed mockup's proportions
 * (fixed ~46px height, world switcher pinned left) rather than living
 * inside the sidebar/content grid like the old UI's per-page bars.
 */
export function NextTopBar({ currentWorldId, currentWorldName, rightSlot, onMenuClick }: Props) {
  return (
    <div className="next-top-bar">
      {onMenuClick && (
        <Button variant="ghost" size="icon-sm" className="md:hidden" onClick={onMenuClick} aria-label="Open menu">
          <Menu />
        </Button>
      )}
      <NextWorldSwitcher currentWorldId={currentWorldId} currentWorldName={currentWorldName} />
      <div className="next-top-bar-spacer" />
      {rightSlot}
    </div>
  );
}
