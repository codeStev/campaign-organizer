import { ReactNode } from 'react';
import { NextWorldSwitcher } from './NextWorldSwitcher';

interface Props {
  currentWorldId?: string;
  currentWorldName?: string;
  rightSlot?: ReactNode;
}

/**
 * /next's persistent header row, matching the reviewed mockup's proportions
 * (fixed ~46px height, world switcher pinned left) rather than living
 * inside the sidebar/content grid like the old UI's per-page bars.
 */
export function NextTopBar({ currentWorldId, currentWorldName, rightSlot }: Props) {
  return (
    <div className="next-top-bar">
      <NextWorldSwitcher currentWorldId={currentWorldId} currentWorldName={currentWorldName} />
      <div className="next-top-bar-spacer" />
      {rightSlot}
    </div>
  );
}
