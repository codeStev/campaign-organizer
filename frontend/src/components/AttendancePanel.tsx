import { useCallback, useEffect, useMemo, useState } from 'react';
import { sessionAttendanceApi, characterSheetsApi, AttendanceEntry, CharacterSheet } from '../api/client';
import { Checkbox } from './ui/checkbox';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Spinner } from './ui/spinner';
import { toast } from 'sonner';

interface Props {
  worldId: string;
  campaignId: string;
  sessionId: string;
  onError: (err: unknown) => void;
  readOnly?: boolean;
}

// Radix Select can't use "" as an item value (reserved for "no selection").
const NONE_VALUE = '__none__';

/** FR-53: Attended | Player | Character, pre-populated from the campaign roster. */
export function AttendancePanel({ worldId, campaignId, sessionId, onError, readOnly = false }: Props) {
  const api = useMemo(
    () => sessionAttendanceApi(worldId, campaignId, sessionId),
    [worldId, campaignId, sessionId],
  );
  const sheetsApi = useMemo(() => characterSheetsApi(worldId), [worldId]);
  const [entries, setEntries] = useState<AttendanceEntry[]>([]);
  const [sheets, setSheets] = useState<CharacterSheet[]>([]);
  const [loading, setLoading] = useState(true);

  // Sheets shared across the world's campaigns, or scoped to this one (ADR-0091).
  const candidateSheets = sheets.filter((s) => s.campaignId == null || s.campaignId === campaignId);

  const refresh = useCallback(async () => {
    try {
      const [attendance, sheetList] = await Promise.all([api.get(), sheetsApi.list()]);
      setEntries(attendance);
      setSheets(sheetList);
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, sheetsApi, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function save(next: AttendanceEntry[]) {
    try {
      setEntries(
        await api.put(
          next.map((e) => ({ playerId: e.playerId, present: e.present, characterId: e.characterId ?? null })),
        ),
      );
      toast.success('Attendance updated');
    } catch (err) {
      onError(err);
    }
  }

  function togglePresent(entry: AttendanceEntry, present: boolean) {
    void save(entries.map((e) => (e.playerId === entry.playerId ? { ...e, present } : e)));
  }

  function setCharacter(entry: AttendanceEntry, characterId: string | null) {
    void save(entries.map((e) => (e.playerId === entry.playerId ? { ...e, characterId } : e)));
  }

  if (readOnly) {
    return (
      <>
        <strong className="muted">Attendance</strong>
        {loading && (
          <p className="muted loading-row">
            <Spinner /> Loading…
          </p>
        )}
        {!loading && entries.length === 0 && <p className="muted">No roster set for this campaign yet.</p>}
        {entries.length > 0 && (
          <ul className="beat-list">
            {entries.map((e) => (
              <li key={e.playerId} className="beat-item">
                <div className="beat-row">
                  <span>{e.present ? '✓' : '—'}</span>
                  <span>
                    {e.name}
                    {e.guest && <span className="muted"> (guest)</span>}
                  </span>
                  <span className="bf-spacer" />
                  <span className="muted">{e.characterName ?? '—'}</span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </>
    );
  }

  return (
    <div className="loose-threads">
      <span className="muted">Attendance</span>
      {loading && (
        <p className="muted loading-row">
          <Spinner /> Loading…
        </p>
      )}
      {!loading && entries.length === 0 && (
        <p className="muted">No roster set for this campaign yet — add players on the Roster section above.</p>
      )}
      {entries.length > 0 && (
        <div className="table-scroll">
        <table className="attendance-table">
          <thead>
            <tr>
              <th>Attended</th>
              <th>Player</th>
              <th>Character</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.playerId}>
                <td>
                  <Checkbox
                    checked={e.present}
                    onCheckedChange={(checked) => togglePresent(e, checked === true)}
                  />
                </td>
                <td>
                  {e.name}
                  {e.guest && <span className="muted"> (guest)</span>}
                </td>
                <td>
                  <Select
                    value={e.characterId ?? NONE_VALUE}
                    onValueChange={(v) => setCharacter(e, v === NONE_VALUE ? null : v)}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE_VALUE}>— none —</SelectItem>
                      {candidateSheets.map((s) => (
                        <SelectItem key={s.id} value={s.id}>
                          {s.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
      )}
    </div>
  );
}
