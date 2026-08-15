import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  campaignsApi,
  articlesApi,
  statblocksApi,
  Campaign,
  ArticleSummary,
  Statblock,
  ApiError,
} from '../api/client';
import { SessionLog } from './SessionLog';
import { ArcBoard } from './ArcBoard';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

export function CampaignsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const api = useMemo(() => campaignsApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);
  const statblockApi = useMemo(() => statblocksApi(worldId), [worldId]);
  const [list, setList] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Campaign | null>(null);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [statblocks, setStatblocks] = useState<Statblock[]>([]);
  const [notes, setNotes] = useState('');
  const [notesDirty, setNotesDirty] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(async () => {
    try {
      setList(await api.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refresh();
    articleApi.list().then(setArticles).catch(handleError);
    statblockApi.list().then(setStatblocks).catch(handleError);
  }, [refresh, articleApi, statblockApi, handleError]);

  function select(campaign: Campaign) {
    setSelected(campaign);
    setNotes(campaign.notes ?? '');
    setNotesDirty(false);
  }

  async function createCampaign() {
    const name = window.prompt('Campaign name?');
    if (!name) return;
    try {
      const created = await api.create({ name });
      await refresh();
      select(created);
    } catch (err) {
      handleError(err);
    }
  }

  async function saveNotes() {
    if (!selected) return;
    try {
      const updated = await api.update(selected.id, {
        name: selected.name,
        description: selected.description,
        notes,
      });
      setSelected(updated);
      setNotesDirty(false);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  async function removeCampaign(campaign: Campaign) {
    try {
      await api.remove(campaign.id);
      if (selected?.id === campaign.id) setSelected(null);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <button onClick={createCampaign}>+ New campaign</button>
        <ul className="article-list">
          {list.map((c) => (
            <li key={c.id}>
              <button
                className={c.id === selected?.id ? 'article-link active' : 'article-link'}
                onClick={() => select(c)}
              >
                <span>{c.name}</span>
              </button>
            </li>
          ))}
          {loading && <li className="muted">Loading…</li>}
          {!loading && list.length === 0 && <li className="muted">No campaigns yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        {!selected && <p className="muted">Select or create a campaign.</p>}
        {selected && (
          <>
            <div className="map-bar">
              <h2>{selected.name}</h2>
              <button className="link-button danger" onClick={() => removeCampaign(selected)}>
                Delete campaign
              </button>
            </div>
            {selected.description && <p className="muted">{selected.description}</p>}

            <section className="card">
              <h3>GM notes</h3>
              <textarea
                className="gm-notes"
                value={notes}
                onChange={(e) => {
                  setNotes(e.target.value);
                  setNotesDirty(true);
                }}
                placeholder="Private notes for this campaign…"
              />
              <div className="editor-actions">
                <button onClick={saveNotes} disabled={!notesDirty}>
                  Save notes
                </button>
              </div>
            </section>

            <SessionLog worldId={worldId} campaignId={selected.id} onError={handleError} />
            <ArcBoard
              worldId={worldId}
              campaignId={selected.id}
              articles={articles}
              statblocks={statblocks}
              onOpenArticle={onOpenArticle}
              onError={handleError}
            />
          </>
        )}
      </div>
    </div>
  );
}
