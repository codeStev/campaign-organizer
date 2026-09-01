import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  campaignsApi,
  articlesApi,
  statblocksApi,
  Campaign,
  CampaignStatus,
  CAMPAIGN_STATUSES,
  ArticleSummary,
  Statblock,
  ApiError,
} from '../api/client';
import { SessionLog } from './SessionLog';
import { ArcBoard } from './ArcBoard';
import { ClockBoard } from './ClockBoard';
import { RosterPanel } from '../components/RosterPanel';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { toast } from 'sonner';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { PromptDialog } from '../components/PromptDialog';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

export function CampaignsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { campaignId: urlCampaignId } = useParams<{ campaignId: string }>();
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
  const [namePromptOpen, setNamePromptOpen] = useState(false);

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

  // The URL is the source of truth for which campaign is open (ADR-0053).
  useEffect(() => {
    if (!urlCampaignId || urlCampaignId === selected?.id) return;
    api.get(urlCampaignId).then(select).catch(handleError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlCampaignId]);

  async function createCampaign(name: string) {
    try {
      const created = await api.create({ name });
      await refresh();
      select(created);
      navigate(`/worlds/${worldId}/campaigns/${created.id}`);
      toast.success(`Campaign "${created.name}" created`);
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
      toast.success('GM notes saved');
    } catch (err) {
      handleError(err);
    }
  }

  async function setStatus(campaign: Campaign, status: CampaignStatus) {
    try {
      const updated = await api.update(campaign.id, {
        name: campaign.name,
        description: campaign.description,
        notes: campaign.notes,
        status,
      });
      setSelected(updated);
      await refresh();
      toast.success(`"${campaign.name}" marked ${status.toLowerCase().replace('_', ' ')}`);
    } catch (err) {
      handleError(err);
    }
  }

  async function removeCampaign(campaign: Campaign) {
    try {
      await api.remove(campaign.id);
      if (selected?.id === campaign.id) {
        setSelected(null);
        navigate(`/worlds/${worldId}/campaigns`);
      }
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <Button onClick={() => setNamePromptOpen(true)}>+ New campaign</Button>
        <PromptDialog
          open={namePromptOpen}
          onOpenChange={setNamePromptOpen}
          title="New campaign"
          label="Campaign name"
          onSubmit={(name) => void createCampaign(name)}
        />
        <ul className="article-list">
          {list.map((c) => (
            <li key={c.id}>
              <button
                className={c.id === selected?.id ? 'article-link active' : 'article-link'}
                onClick={() => navigate(`/worlds/${worldId}/campaigns/${c.id}`)}
              >
                <TruncatedLabel label={c.name}>{c.name}</TruncatedLabel>
              </button>
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
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
              <span className={`campaign-status campaign-${selected.status.toLowerCase().replace('_', '-')}`}>
                {selected.status.toLowerCase().replace('_', ' ')}
              </span>
              <Select
                value={selected.status}
                onValueChange={(v) => void setStatus(selected, v as CampaignStatus)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CAMPAIGN_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {s.toLowerCase().replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <ConfirmDeleteDialog
                trigger={
                  <Button variant="link" className="text-destructive hover:text-destructive">
                    Delete campaign
                  </Button>
                }
                title="Delete campaign?"
                description={`This permanently deletes "${selected.name}" — its sessions, arcs, and beats. This cannot be undone.`}
                onConfirm={() => removeCampaign(selected)}
              />
            </div>
            {selected.description && <p className="muted">{selected.description}</p>}

            <section className="card">
              <h3>GM notes</h3>
              <div className="gm-notes">
                <MarkdownEditor
                  value={notes}
                  onChange={(value) => {
                    setNotes(value);
                    setNotesDirty(true);
                  }}
                />
              </div>
              <div className="editor-actions">
                <Button onClick={saveNotes} disabled={!notesDirty}>
                  Save notes
                </Button>
              </div>
            </section>

            <RosterPanel worldId={worldId} campaignId={selected.id} onError={handleError} />

            <SessionLog
              worldId={worldId}
              campaignId={selected.id}
              campaignName={selected.name}
              onError={handleError}
            />
            <ArcBoard
              worldId={worldId}
              campaignId={selected.id}
              articles={articles}
              statblocks={statblocks}
              onOpenArticle={onOpenArticle}
              onError={handleError}
            />
            <ClockBoard worldId={worldId} campaignId={selected.id} onError={handleError} />
          </>
        )}
      </div>
    </div>
  );
}
