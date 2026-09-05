import { useState } from 'react';
import { toast } from 'sonner';
import { campaignCalendarApi } from '../api/client';
import { Button, buttonVariants } from './ui/button';
import { Popover, PopoverContent, PopoverTrigger } from './ui/popover';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from './ui/alert-dialog';

interface Props {
  worldId: string;
  campaignId: string;
}

/**
 * .ics export UI for a campaign's sessions (ADR-0108): a one-time download,
 * plus a live subscribe link a calendar app can poll on its own — with a
 * regenerate action to revoke a leaked link.
 */
export function CampaignCalendarExport({ worldId, campaignId }: Props) {
  const api = campaignCalendarApi(worldId, campaignId);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function loadToken() {
    if (token || loading) return;
    setLoading(true);
    api.getOrCreateFeed()
      .then((r) => setToken(r.token))
      .catch(() => toast.error('Could not load the subscribe link'))
      .finally(() => setLoading(false));
  }

  const subscribeUrl = token ? `${window.location.origin}/api/calendar/${token}.ics` : null;

  function copyLink() {
    if (!subscribeUrl) return;
    navigator.clipboard
      .writeText(subscribeUrl)
      .then(() => toast.success('Subscribe link copied'))
      .catch(() => toast.error('Could not copy the link'));
  }

  async function regenerate() {
    try {
      const r = await api.regenerateFeed();
      setToken(r.token);
      toast.success('Subscribe link regenerated — the old link no longer works');
    } catch {
      toast.error('Could not regenerate the subscribe link');
    }
  }

  return (
    <Popover onOpenChange={(open) => open && loadToken()}>
      {/* PopoverTrigger renders the button itself (styled via buttonVariants)
          rather than wrapping <Button> with asChild — shadcn's Button isn't
          forwardRef-wrapped, so Radix's floating-ui positioning can't find
          an anchor element through it, and the popover renders unpositioned. */}
      <PopoverTrigger className={buttonVariants({ variant: 'outline', size: 'sm' })}>
        🗓️ Export calendar
      </PopoverTrigger>
      <PopoverContent align="end" className="calendar-export-popover">
        <Button size="sm" onClick={() => void api.downloadIcs()}>
          Download .ics
        </Button>
        <div className="calendar-export-subscribe">
          <label className="muted">Subscribe link (auto-updates in your calendar app)</label>
          <div className="calendar-export-link-row">
            <input
              readOnly
              value={subscribeUrl ?? (loading ? 'Loading…' : '')}
              onFocus={(e) => e.target.select()}
            />
            <Button variant="outline" size="sm" onClick={copyLink} disabled={!subscribeUrl}>
              Copy
            </Button>
          </div>
        </div>
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button
              variant="link"
              size="sm"
              className="text-destructive hover:text-destructive"
              disabled={!token}
            >
              Regenerate link
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Regenerate subscribe link?</AlertDialogTitle>
              <AlertDialogDescription>
                Any calendar app already subscribed to the current link stops receiving updates —
                you'll need to re-subscribe with the new one.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction variant="destructive" onClick={() => void regenerate()}>
                Regenerate
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </PopoverContent>
    </Popover>
  );
}
