import { useEffect } from 'react';

/**
 * Minimal cross-component "something changed" signal, scoped by world id.
 *
 * `CampaignNavTree` keeps its own independent copy of campaigns/sessions/
 * arcs, fetched once on mount and otherwise only patched by mutations made
 * through the tree itself (its own "+ New arc" dialog, its own rename
 * dialogs). A session/arc/campaign created, deleted, or renamed from its own
 * dedicated page (NextSessionsPage, NextArcsPage, NextCampaignsPage) has no
 * way to reach that separate copy, so the sidebar goes stale until a full
 * page reload. This is a deliberately tiny pub/sub rather than a real
 * client-side cache (React Query etc.) - one scope key (the world id, since
 * everything affected lives under one world), no per-entity-type tracking:
 * any change just tells every subscriber in that world to refetch what it
 * owns.
 */
const listeners = new Map<string, Set<() => void>>();

export function notifyDataChanged(worldId: string) {
  listeners.get(worldId)?.forEach((callback) => callback());
}

export function useDataRefreshListener(worldId: string, callback: () => void) {
  useEffect(() => {
    const set = listeners.get(worldId) ?? new Set();
    set.add(callback);
    listeners.set(worldId, set);
    return () => {
      set.delete(callback);
    };
  }, [worldId, callback]);
}
