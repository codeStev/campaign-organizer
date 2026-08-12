import { useCallback, useEffect, useMemo, useState } from 'react';
import { statblocksApi, Statblock } from '../api/client';

interface Props {
  worldId: string;
  onError: (err: unknown) => void;
}

interface StatRow {
  key: string;
  value: string;
}

interface Draft {
  id: string | null;
  name: string;
  notes: string;
  rows: StatRow[];
}

const EMPTY: Draft = { id: null, name: '', notes: '', rows: [{ key: '', value: '' }] };

function toRows(stats: Record<string, unknown>): StatRow[] {
  const rows = Object.entries(stats).map(([key, value]) => ({ key, value: String(value) }));
  return rows.length ? rows : [{ key: '', value: '' }];
}

export function StatblocksPanel({ worldId, onError }: Props) {
  const api = useMemo(() => statblocksApi(worldId), [worldId]);
  const [list, setList] = useState<Statblock[]>([]);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const refresh = useCallback(async () => {
    try {
      setList(await api.list());
    } catch (err) {
      onError(err);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  function edit(sb: Statblock) {
    setDraft({ id: sb.id, name: sb.name, notes: sb.notes ?? '', rows: toRows(sb.stats) });
  }

  async function save() {
    const stats: Record<string, unknown> = {};
    for (const r of draft.rows) {
      if (r.key.trim()) {
        const num = Number(r.value);
        stats[r.key.trim()] = r.value !== '' && !Number.isNaN(num) ? num : r.value;
      }
    }
    const body = { name: draft.name, stats, notes: draft.notes || null };
    try {
      if (draft.id) await api.update(draft.id, body);
      else await api.create(body);
      setDraft(EMPTY);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function remove(sb: Statblock) {
    try {
      await api.remove(sb.id);
      if (draft.id === sb.id) setDraft(EMPTY);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  function setRow(i: number, patch: Partial<StatRow>) {
    setDraft((d) => ({ ...d, rows: d.rows.map((r, j) => (j === i ? { ...r, ...patch } : r)) }));
  }

  return (
    <div className="sheets-panel">
      <div className="sheets-list-col">
        <button onClick={() => setDraft(EMPTY)}>+ New statblock</button>
        <ul className="article-list">
          {list.map((sb) => (
            <li key={sb.id}>
              <button
                className={sb.id === draft.id ? 'article-link active' : 'article-link'}
                onClick={() => edit(sb)}
              >
                <span>{sb.name}</span>
              </button>
            </li>
          ))}
          {list.length === 0 && <li className="muted">No statblocks yet.</li>}
        </ul>
      </div>

      <div className="sheet-detail card">
        <input
          className="title-input"
          placeholder="Statblock name (e.g. Goblin)"
          value={draft.name}
          onChange={(e) => setDraft({ ...draft, name: e.target.value })}
        />
        <strong className="muted">Stats</strong>
        {draft.rows.map((row, i) => (
          <div key={i} className="month-row">
            <input
              placeholder="stat (AC)"
              value={row.key}
              onChange={(e) => setRow(i, { key: e.target.value })}
            />
            <input
              placeholder="value (15)"
              value={row.value}
              onChange={(e) => setRow(i, { value: e.target.value })}
            />
            <button
              type="button"
              className="link-button danger"
              onClick={() => setDraft((d) => ({ ...d, rows: d.rows.filter((_, j) => j !== i) }))}
            >
              ✕
            </button>
          </div>
        ))}
        <button
          type="button"
          className="link-button"
          onClick={() => setDraft((d) => ({ ...d, rows: [...d.rows, { key: '', value: '' }] }))}
        >
          + Add stat
        </button>
        <textarea
          placeholder="Notes (abilities, tactics…)"
          value={draft.notes}
          onChange={(e) => setDraft({ ...draft, notes: e.target.value })}
        />
        <div className="editor-actions">
          <button onClick={save} disabled={!draft.name}>
            {draft.id ? 'Save statblock' : 'Create statblock'}
          </button>
          {draft.id && (
            <button className="link-button danger" onClick={() => remove(list.find((s) => s.id === draft.id)!)}>
              Delete
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
