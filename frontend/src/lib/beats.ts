import { beatsApi, Arc, Beat } from '../api/client';

/** All story beats across a campaign's arcs, unordered. Beats live per arc
 * (there's no single list-all endpoint), so this fans out one request per
 * arc and flattens the results — shared by the campaign-wide recap
 * (RecapView) and the per-session recap (SessionLog). Takes the arc list
 * rather than fetching it itself since callers already need it too. */
export async function fetchCampaignBeats(worldId: string, campaignId: string, arcs: Arc[]): Promise<Beat[]> {
  const perArc = await Promise.all(arcs.map((a) => beatsApi(worldId, campaignId, a.id).list()));
  return perArc.flat();
}
