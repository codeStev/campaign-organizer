import { useState } from 'react';
import { Button } from '../components/ui/button';
import { TimelinesView } from './TimelinesView';
import { CalendarsView } from './CalendarsView';

type ChronicleTab = 'timeline' | 'calendar';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

/**
 * Chronicle (docs/ui-overhaul-plan.md Phase 2) — merges Timelines and
 * Calendars as sub-tabs of one screen, matching the reviewed mockup. Both
 * underlying views are reused as-is; no new backend.
 */
export function NextChroniclePage({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const [tab, setTab] = useState<ChronicleTab>('timeline');
  return (
    <div className="next-chronicle">
      <div className="next-chronicle-tabs">
        <Button variant={tab === 'timeline' ? 'default' : 'outline'} size="sm" onClick={() => setTab('timeline')}>
          Timeline
        </Button>
        <Button variant={tab === 'calendar' ? 'default' : 'outline'} size="sm" onClick={() => setTab('calendar')}>
          Calendar
        </Button>
      </div>
      {tab === 'timeline' ? (
        <TimelinesView worldId={worldId} onOpenArticle={onOpenArticle} onAuthExpired={onAuthExpired} />
      ) : (
        <CalendarsView worldId={worldId} onAuthExpired={onAuthExpired} />
      )}
    </div>
  );
}
