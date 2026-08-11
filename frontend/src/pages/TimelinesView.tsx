import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  timelinesApi,
  eventsApi,
  articlesApi,
  Timeline,
  TimelineEvent,
  ArticleSummary,
  ApiError,
} from '../api/client';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

interface EventDraft {
  id: string | null;
  title: string;
  year: string;
  month: string;
  day: string;
  articleId: string;
  description: string;
}

const EMPTY_EVENT: EventDraft = {
  id: null,
  title: '',
  year: '',
  month: '',
  day: '',
  articleId: '',
  description: '',
};

function formatDate(e: TimelineEvent): string {
  return [e.year, e.month, e.day].filter((v) => v !== null && v !== undefined).join('.');
}

export function TimelinesView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const api = useMemo(() => timelinesApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);

  const [list, setList] = useState<Timeline[]>([]);
  const [selected, setSelected] = useState<Timeline | null>(null);
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [draft, setDraft] = useState<EventDraft>(EMPTY_EVENT);
  const [error, setError] = useState<string | null>(null);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const loadEvents = useCallback(
    async (timelineId: string) => {
      try {
        setEvents(await eventsApi(worldId, timelineId).list());
      } catch (err) {
        handleError(err);
      }
    },
    [worldId, handleError],
  );

  useEffect(() => {
    api.list().then(setList).catch(handleError);
    articleApi.list().then(setArticles).catch(handleError);
  }, [api, articleApi, handleError]);

  async function selectTimeline(timeline: Timeline) {
    setSelected(timeline);
    setDraft(EMPTY_EVENT);
    await loadEvents(timeline.id);
  }

  async function createTimeline() {
    const name = window.prompt('Timeline name?');
    if (!name) return;
    try {
      const created = await api.create({ name });
      setList(await api.list());
      await selectTimeline(created);
    } catch (err) {
      handleError(err);
    }
  }

  async function deleteTimeline(timeline: Timeline) {
    try {
      await api.remove(timeline.id);
      if (selected?.id === timeline.id) {
        setSelected(null);
        setEvents([]);
      }
      setList(await api.list());
    } catch (err) {
      handleError(err);
    }
  }

  async function saveEvent(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    if (draft.year.trim() === '' || Number.isNaN(Number(draft.year))) {
      setError('Year is required and must be a number');
      return;
    }
    setError(null);
    const body = {
      title: draft.title,
      year: Number(draft.year),
      month: draft.month ? Number(draft.month) : null,
      day: draft.day ? Number(draft.day) : null,
      articleId: draft.articleId || null,
      description: draft.description || undefined,
    };
    try {
      const events = eventsApi(worldId, selected.id);
      if (draft.id) await events.update(draft.id, body);
      else await events.create(body);
      setDraft(EMPTY_EVENT);
      await loadEvents(selected.id);
    } catch (err) {
      handleError(err);
    }
  }

  async function deleteEvent(event: TimelineEvent) {
    if (!selected) return;
    try {
      await eventsApi(worldId, selected.id).remove(event.id);
      if (draft.id === event.id) setDraft(EMPTY_EVENT);
      await loadEvents(selected.id);
    } catch (err) {
      handleError(err);
    }
  }

  function editEvent(event: TimelineEvent) {
    setDraft({
      id: event.id,
      title: event.title,
      year: String(event.year),
      month: event.month != null ? String(event.month) : '',
      day: event.day != null ? String(event.day) : '',
      articleId: event.articleId ?? '',
      description: event.description ?? '',
    });
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <button onClick={createTimeline}>+ New timeline</button>
        <ul className="article-list">
          {list.map((t) => (
            <li key={t.id}>
              <button
                className={t.id === selected?.id ? 'article-link active' : 'article-link'}
                onClick={() => selectTimeline(t)}
              >
                <span>{t.name}</span>
              </button>
            </li>
          ))}
          {list.length === 0 && <li className="muted">No timelines yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        {!selected && <p className="muted">Select or create a timeline.</p>}
        {selected && (
          <>
            <div className="map-bar">
              <strong>{selected.name}</strong>
              <button className="link-button danger" onClick={() => deleteTimeline(selected)}>
                Delete timeline
              </button>
            </div>

            <form className="card" onSubmit={saveEvent}>
              <strong>{draft.id ? 'Edit event' : 'New event'}</strong>
              <input
                placeholder="Event title"
                value={draft.title}
                onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                required
              />
              <div className="date-fields">
                <input
                  type="number"
                  placeholder="Year"
                  value={draft.year}
                  onChange={(e) => setDraft({ ...draft, year: e.target.value })}
                  required
                />
                <input
                  type="number"
                  min="1"
                  placeholder="Month"
                  value={draft.month}
                  onChange={(e) => setDraft({ ...draft, month: e.target.value })}
                />
                <input
                  type="number"
                  min="1"
                  placeholder="Day"
                  value={draft.day}
                  onChange={(e) => setDraft({ ...draft, day: e.target.value })}
                />
              </div>
              <select
                value={draft.articleId}
                onChange={(e) => setDraft({ ...draft, articleId: e.target.value })}
              >
                <option value="">— link article (optional) —</option>
                {articles.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.title}
                  </option>
                ))}
              </select>
              <textarea
                placeholder="Description (optional)"
                value={draft.description}
                onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              />
              <div className="editor-actions">
                <button type="submit" disabled={draft.title.length === 0}>
                  {draft.id ? 'Save event' : 'Add event'}
                </button>
                {draft.id && (
                  <button type="button" className="link-button" onClick={() => setDraft(EMPTY_EVENT)}>
                    Cancel
                  </button>
                )}
              </div>
            </form>

            <ol className="timeline">
              {events.map((event) => (
                <li key={event.id} className="timeline-event card">
                  <div className="timeline-date">{formatDate(event)}</div>
                  <div className="timeline-body">
                    <strong>{event.title}</strong>
                    {event.description && <p className="muted">{event.description}</p>}
                    <div className="editor-actions">
                      <button className="link-button" onClick={() => editEvent(event)}>
                        Edit
                      </button>
                      {event.articleId && (
                        <button className="link-button" onClick={() => onOpenArticle(event.articleId!)}>
                          Open article
                        </button>
                      )}
                      <button className="link-button danger" onClick={() => deleteEvent(event)}>
                        Delete
                      </button>
                    </div>
                  </div>
                </li>
              ))}
              {events.length === 0 && <li className="muted">No events yet.</li>}
            </ol>
          </>
        )}
      </div>
    </div>
  );
}
