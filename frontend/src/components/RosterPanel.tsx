import { useCallback, useEffect, useMemo, useState } from 'react';
import { playersApi, campaignRosterApi, Player, RosterEntry } from '../api/client';
import { Checkbox } from './ui/checkbox';
import { Spinner } from './ui/spinner';
import { toast } from 'sonner';

interface Props {
  worldId: string;
  campaignId: string;
  onError: (err: unknown) => void;
}

/** FR-53: which of the world's players are on this campaign's roster, and who's a guest. */
export function RosterPanel({ worldId, campaignId, onError }: Props) {
  const playerApi = useMemo(() => playersApi(worldId), [worldId]);
  const rosterApi = useMemo(() => campaignRosterApi(worldId, campaignId), [worldId, campaignId]);
  const [players, setPlayers] = useState<Player[]>([]);
  const [roster, setRoster] = useState<RosterEntry[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const [playerList, rosterList] = await Promise.all([playerApi.list(), rosterApi.get()]);
      setPlayers(playerList);
      setRoster(rosterList);
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [playerApi, rosterApi, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function save(entries: RosterEntry[]) {
    try {
      setRoster(await rosterApi.put(entries.map((e) => ({ playerId: e.playerId, guest: e.guest }))));
      toast.success('Roster updated');
    } catch (err) {
      onError(err);
    }
  }

  function toggleMember(player: Player, onRoster: boolean) {
    if (onRoster) {
      void save([...roster, { playerId: player.id, name: player.name, guest: false }]);
    } else {
      void save(roster.filter((e) => e.playerId !== player.id));
    }
  }

  function toggleGuest(entry: RosterEntry, guest: boolean) {
    void save(roster.map((e) => (e.playerId === entry.playerId ? { ...e, guest } : e)));
  }

  return (
    <section className="card">
      <h3>Roster</h3>
      {loading && (
        <p className="muted loading-row">
          <Spinner /> Loading…
        </p>
      )}
      {!loading && players.length === 0 && (
        <p className="muted">No players yet — add some on the Players tab first.</p>
      )}
      {!loading && players.length > 0 && (
        <ul className="beat-list">
          {players.map((p) => {
            const entry = roster.find((e) => e.playerId === p.id);
            const onRoster = entry != null;
            return (
              <li key={p.id} className="beat-item">
                <div className="beat-row">
                  <label className="diff-pick">
                    <Checkbox
                      checked={onRoster}
                      onCheckedChange={(checked) => toggleMember(p, checked === true)}
                    />
                  </label>
                  <span>{p.name}</span>
                  <span className="bf-spacer" />
                  {onRoster && (
                    <label className="diff-pick" title="Guest player">
                      <Checkbox
                        checked={entry.guest}
                        onCheckedChange={(checked) => toggleGuest(entry, checked === true)}
                      />
                      <span className="muted">Guest</span>
                    </label>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
