import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  whiteboardsApi,
  Whiteboard,
  WhiteboardNode,
  WhiteboardEdge,
  ApiError,
} from '../api/client';
import { WhiteboardCanvas } from '../components/WhiteboardCanvas';
import { Button } from '../components/ui/button';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { toast } from 'sonner';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { PromptDialog } from '../components/PromptDialog';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

export function NextWhiteboardsView({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { whiteboardId: urlWhiteboardId } = useParams<{ whiteboardId: string }>();
  const api = useMemo(() => whiteboardsApi(worldId), [worldId]);
  const [list, setList] = useState<Whiteboard[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Whiteboard | null>(null);
  const [nodes, setNodes] = useState<WhiteboardNode[]>([]);
  const [edges, setEdges] = useState<WhiteboardEdge[]>([]);
  const [dirty, setDirty] = useState(false);
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
  }, [refresh]);

  function select(board: Whiteboard) {
    setSelected(board);
    setNodes(board.nodes);
    setEdges(board.edges);
    setDirty(false);
  }

  // The URL is the source of truth for which whiteboard is open (ADR-0053).
  useEffect(() => {
    if (!urlWhiteboardId || urlWhiteboardId === selected?.id) return;
    api.get(urlWhiteboardId).then(select).catch(handleError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlWhiteboardId]);

  async function createBoard(name: string) {
    try {
      const created = await api.create({ name, nodes: [], edges: [] });
      await refresh();
      select(created);
      navigate(urlWhiteboardId ? `../${created.id}` : created.id, { relative: 'path' });
      toast.success(`Whiteboard "${created.name}" created`);
    } catch (err) {
      handleError(err);
    }
  }

  async function save() {
    if (!selected) return;
    try {
      const saved = await api.update(selected.id, { name: selected.name, nodes, edges });
      setSelected(saved);
      setDirty(false);
      await refresh();
      toast.success('Whiteboard saved');
    } catch (err) {
      handleError(err);
    }
  }

  async function removeBoard(board: Whiteboard) {
    try {
      await api.remove(board.id);
      if (selected?.id === board.id) {
        setSelected(null);
        navigate('..', { relative: 'path' });
      }
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  function onCanvasChange(n: WhiteboardNode[], e: WhiteboardEdge[]) {
    setNodes(n);
    setEdges(e);
    setDirty(true);
  }

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <Button className="sidebar-new-button" size="sm" onClick={() => setNamePromptOpen(true)}>
          + New whiteboard
        </Button>
        <PromptDialog
          open={namePromptOpen}
          onOpenChange={setNamePromptOpen}
          title="New whiteboard"
          label="Whiteboard name"
          onSubmit={(name) => void createBoard(name)}
        />
        <ul className="article-list">
          {list.map((b) => (
            <li key={b.id}>
              <button
                className={b.id === selected?.id ? 'article-link active' : 'article-link'}
                onClick={() => navigate(urlWhiteboardId ? `../${b.id}` : b.id, { relative: 'path' })}
              >
                <TruncatedLabel label={b.name}>{b.name}</TruncatedLabel>
              </button>
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && list.length === 0 && <li className="muted">No whiteboards yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        {!selected && <p className="muted">Select or create a whiteboard.</p>}
        {selected && (
          <>
            <div className="map-bar">
              <strong>{selected.name}</strong>
              <div className="editor-actions">
                <Button onClick={save} disabled={!dirty}>
                  {dirty ? 'Save' : 'Saved'}
                </Button>
                <ConfirmDeleteDialog
                  trigger={
                    <Button variant="link" className="text-destructive hover:text-destructive">
                      Delete
                    </Button>
                  }
                  title="Delete whiteboard?"
                  description={`This permanently deletes "${selected.name}" and cannot be undone.`}
                  onConfirm={() => removeBoard(selected)}
                />
              </div>
            </div>
            <p className="muted hint">Drag to move · double-click to edit · Connect to link nodes.</p>
            <WhiteboardCanvas nodes={nodes} edges={edges} onChange={onCanvasChange} />
          </>
        )}
      </div>
    </div>
  );
}
