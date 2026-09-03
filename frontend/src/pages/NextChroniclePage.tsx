import { Navigate, NavLink, Route, Routes, useLocation } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { TimelinesView } from './TimelinesView';
import { CalendarsView } from './CalendarsView';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

/**
 * Chronicle (docs/ui-overhaul-plan.md Phase 2) — merges Timelines and
 * Calendars as sub-tabs of one screen, matching the reviewed mockup. Both
 * underlying views are reused as-is; no new backend.
 *
 * Real nested routes ("timelines"/"timelines/:timelineId",
 * "calendars"/"calendars/:calendarId"), not a local-state tab switch: both
 * views read their selected item from the URL (`useParams`, ADR-0053 — the
 * URL is the source of truth), so a local tab toggle would leave clicking a
 * specific timeline/calendar with nowhere valid to navigate to.
 */
export function NextChroniclePage({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const location = useLocation();
  const onCalendars = location.pathname.includes('/calendars');
  return (
    <div className="next-chronicle">
      <div className="next-chronicle-tabs">
        <Button variant={onCalendars ? 'outline' : 'default'} size="sm" asChild>
          <NavLink to="timelines">Timeline</NavLink>
        </Button>
        <Button variant={onCalendars ? 'default' : 'outline'} size="sm" asChild>
          <NavLink to="calendars">Calendar</NavLink>
        </Button>
      </div>
      <Routes>
        <Route index element={<Navigate to="timelines" replace />} />
        <Route
          path="timelines"
          element={<TimelinesView worldId={worldId} onOpenArticle={onOpenArticle} onAuthExpired={onAuthExpired} />}
        />
        <Route
          path="timelines/:timelineId"
          element={<TimelinesView worldId={worldId} onOpenArticle={onOpenArticle} onAuthExpired={onAuthExpired} />}
        />
        <Route path="calendars" element={<CalendarsView worldId={worldId} onAuthExpired={onAuthExpired} />} />
        <Route
          path="calendars/:calendarId"
          element={<CalendarsView worldId={worldId} onAuthExpired={onAuthExpired} />}
        />
        <Route path="*" element={<Navigate to="timelines" replace />} />
      </Routes>
    </div>
  );
}
