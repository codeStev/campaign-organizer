import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  campaignsApi,
  statblocksApi,
  fieldTemplatesApi,
  globalFieldTemplatesApi,
  encountersApi,
  ApiError,
  Campaign,
  Statblock,
  FieldTemplate,
  GlobalFieldTemplate,
  Encounter,
} from '../api/client';
import { EncounterBoard } from './EncounterBoard';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Spinner } from '../components/ui/spinner';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

/**
 * Encounters (docs/ui-overhaul-plan.md Phase 5) — the existing encounter
 * builder (ADR-0097) relocated to its own /next nav entry, with a statblock
 * reference panel beside it (mirrors StatblocksPanel's list-plus-detail
 * layout) so a GM can eyeball the world's statblocks while building.
 * EncounterBoard itself is reused unchanged — "mostly reskin/relocation"
 * per the plan. Encounters are still campaign-scoped, so this page adds a
 * campaign picker the old UI didn't need (it lived inside one campaign's
 * detail view).
 */
export function NextEncountersPage({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { campaignId } = useParams<{ campaignId: string }>();
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [statblocks, setStatblocks] = useState<Statblock[]>([]);
  const [templates, setTemplates] = useState<FieldTemplate[]>([]);
  const [globalTemplates, setGlobalTemplates] = useState<GlobalFieldTemplate[]>([]);
  const [encounters, setEncounters] = useState<Encounter[]>([]);
  const [loading, setLoading] = useState(true);

  const onError = (err: unknown) => {
    if (err instanceof ApiError && err.status === 401) onAuthExpired();
  };

  useEffect(() => {
    setLoading(true);
    Promise.all([
      campaignsApi(worldId).list(),
      statblocksApi(worldId).list(),
      fieldTemplatesApi(worldId).list(),
      globalFieldTemplatesApi.list(),
    ])
      .then(([c, s, t, g]) => {
        setCampaigns(c);
        setStatblocks(s);
        setTemplates(t);
        setGlobalTemplates(g);
      })
      .catch(onError)
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId]);

  // Land on the first campaign once the list loads, if none is selected yet.
  useEffect(() => {
    if (!campaignId && campaigns.length > 0) {
      navigate(campaigns[0].id, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [campaignId, campaigns]);

  const refreshEncounters = useCallback(() => {
    if (!campaignId) return;
    encountersApi(worldId, campaignId).list().then(setEncounters).catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId, campaignId]);

  useEffect(() => {
    refreshEncounters();
  }, [refreshEncounters]);

  if (loading) {
    return (
      <p className="muted loading-row">
        <Spinner /> Loading…
      </p>
    );
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <Select value={campaignId} onValueChange={(v) => navigate(`../${v}`, { relative: 'path' })}>
          <SelectTrigger>
            <SelectValue placeholder="Pick a campaign…" />
          </SelectTrigger>
          <SelectContent>
            {campaigns.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <h4>Statblocks</h4>
        <ul className="article-list article-list-scroll">
          {statblocks.map((s) => (
            <li key={s.id} className="muted">
              {s.name}
            </li>
          ))}
          {statblocks.length === 0 && <li className="muted">No statblocks in this world yet.</li>}
        </ul>
      </aside>

      {campaigns.length === 0 ? (
        <p className="muted">No campaigns yet — create one in the current UI.</p>
      ) : campaignId ? (
        <EncounterBoard
          worldId={worldId}
          campaignId={campaignId}
          encounters={encounters}
          onChanged={refreshEncounters}
          statblocks={statblocks}
          templates={templates}
          globalTemplates={globalTemplates}
          onError={onError}
        />
      ) : (
        <p className="muted">Pick a campaign.</p>
      )}
    </div>
  );
}
