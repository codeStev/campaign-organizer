import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  whiteboardsApi,
  Whiteboard,
  WhiteboardNode,
  WhiteboardEdge,
  ApiError,
} from '../api/client';
import { WhiteboardCanvas } from '../components/WhiteboardCanvas';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

export function WhiteboardsView({ worldId, onAuthExpired }: Props) {
  const api = useMemo(() => whiteboardsApi(worldId), [worldId]);
  const [list, setList] = useState<Whiteboard[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Whiteboard | null>(null);
  const [nodes, setNodes] = useState<WhiteboardNode[]>([]);
  const [edges, setEdges] = useState<WhiteboardEdge[]>([]);
  const [dirty, setDirty] = useState(false);
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
  }, [refresh]);

  function select(board: Whiteboard) {
    setSelected(board);
    setNodes(board.nodes);
    setEdges(board.edges);
    setDirty(false);
  }

  async function createBoard() {
    const name = window.prompt('Whiteboard name?');
    if (!name) return;
    try {
      const created = await api.create({ name, nodes: [], edges: [] });
      await refresh();
      select(created);
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
    } catch (err) {
      handleError(err);
    }
  }

  async function removeBoard(board: Whiteboard) {
    try {
      await api.remove(board.id);
      if (selected?.id === board.id) setSelected(null);
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
        <button onClick={createBoard}>+ New whiteboard</button>
        <ul className="article-list">
          {list.map((b) => (
            <li key={b.id}>
              <button
                className={b.id === selected?.id ? 'article-link active' : 'article-link'}
                onClick={() => select(b)}
              >
                <span>{b.name}</span>
              </button>
            </li>
          ))}
          {loading && <li className="muted">Loading…</li>}
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
                <button onClick={save} disabled={!dirty}>
                  {dirty ? 'Save' : 'Saved'}
                </button>
                <button className="link-button danger" onClick={() => removeBoard(selected)}>
                  Delete
                </button>
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
