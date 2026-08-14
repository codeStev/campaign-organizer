import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { sessionsApi, Session } from '../api/client';

interface Props {
  worldId: string;
  campaignId: string;
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  title: string;
  sessionNumber: string;
  date: string;
  summary: string;
}

const EMPTY: Draft = { id: null, title: '', sessionNumber: '', date: '', summary: '' };

export function SessionLog({ worldId, campaignId, onError }: Props) {
  const api = useMemo(() => sessionsApi(worldId, campaignId), [worldId, campaignId]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const refresh = useCallback(async () => {
    try {
      setSessions(await api.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function save(e: FormEvent) {
    e.preventDefault();
    const body = {
      title: draft.title,
      sessionNumber: draft.sessionNumber ? Number(draft.sessionNumber) : null,
      date: draft.date || null,
      summary: draft.summary || null,
    };
    try {
      if (draft.id) await api.update(draft.id, body);
      else await api.create(body);
      setDraft(EMPTY);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function remove(id: string) {
    try {
      await api.remove(id);
      if (draft.id === id) setDraft(EMPTY);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  function edit(s: Session) {
    setDraft({
      id: s.id,
      title: s.title,
      sessionNumber: s.sessionNumber != null ? String(s.sessionNumber) : '',
      date: s.date ?? '',
      summary: s.summary ?? '',
    });
  }

  return (
    <section className="card">
      <h3>Sessions</h3>
      <form className="session-form" onSubmit={save}>
        <div className="session-form-row">
          <input
            type="number"
            className="num-input"
            placeholder="#"
            value={draft.sessionNumber}
            onChange={(e) => setDraft({ ...draft, sessionNumber: e.target.value })}
          />
          <input
            type="date"
            value={draft.date}
            onChange={(e) => setDraft({ ...draft, date: e.target.value })}
          />
          <input
            placeholder="Session title"
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
            required
          />
        </div>
        <textarea
          placeholder="Summary (optional)"
          value={draft.summary}
          onChange={(e) => setDraft({ ...draft, summary: e.target.value })}
        />
        <div className="editor-actions">
          <button type="submit" disabled={!draft.title}>
            {draft.id ? 'Save session' : 'Add session'}
          </button>
          {draft.id && (
            <button type="button" className="link-button" onClick={() => setDraft(EMPTY)}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <ol className="session-list">
        {sessions.map((s) => (
          <li key={s.id} className="session-item">
            <div className="session-meta">
              {s.sessionNumber != null && <span className="session-num">#{s.sessionNumber}</span>}
              {s.date && <span className="muted">{s.date}</span>}
            </div>
            <div className="session-body">
              <strong>{s.title}</strong>
              {s.summary && <p className="muted">{s.summary}</p>}
              <div className="editor-actions">
                <button className="link-button" onClick={() => edit(s)}>
                  Edit
                </button>
                <button className="link-button danger" onClick={() => remove(s.id)}>
                  Delete
                </button>
              </div>
            </div>
          </li>
        ))}
        {loading && <li className="muted">Loading…</li>}
        {!loading && sessions.length === 0 && (
          <li className="muted">No sessions logged yet.</li>
        )}
      </ol>
    </section>
  );
}
