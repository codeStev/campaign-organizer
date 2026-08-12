import { useRef, useState } from 'react';
import { WhiteboardNode, WhiteboardEdge } from '../api/client';

interface Props {
  nodes: WhiteboardNode[];
  edges: WhiteboardEdge[];
  onChange: (nodes: WhiteboardNode[], edges: WhiteboardEdge[]) => void;
}

const NODE_W = 130;
const NODE_H = 56;

function uid() {
  return Math.random().toString(36).slice(2, 10);
}

/** Free-form canvas: drag nodes, click-to-connect, edit/delete. Pure DOM/SVG. */
export function WhiteboardCanvas({ nodes, edges, onChange }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const [connectFrom, setConnectFrom] = useState<string | null>(null);
  const drag = useRef<{ id: string; dx: number; dy: number } | null>(null);

  function addNode() {
    const text = window.prompt('Node text') ?? '';
    if (!text) return;
    const n: WhiteboardNode = { id: uid(), text, x: 40, y: 40, color: '#6d54c9' };
    onChange([...nodes, n], edges);
  }

  function onNodeMouseDown(e: React.MouseEvent, node: WhiteboardNode) {
    if (connectFrom !== null) return; // connect mode handles clicks separately
    const rect = ref.current!.getBoundingClientRect();
    drag.current = { id: node.id, dx: e.clientX - rect.left - node.x, dy: e.clientY - rect.top - node.y };
  }

  function onMouseMove(e: React.MouseEvent) {
    if (!drag.current || !ref.current) return;
    const rect = ref.current.getBoundingClientRect();
    const x = Math.max(0, e.clientX - rect.left - drag.current.dx);
    const y = Math.max(0, e.clientY - rect.top - drag.current.dy);
    onChange(nodes.map((n) => (n.id === drag.current!.id ? { ...n, x, y } : n)), edges);
  }

  function endDrag() {
    drag.current = null;
  }

  function onNodeClick(node: WhiteboardNode) {
    if (!connectFrom) return;
    if (connectFrom !== node.id) {
      const label = window.prompt('Connection label (optional)') ?? '';
      onChange(nodes, [
        ...edges,
        { id: uid(), fromNodeId: connectFrom, toNodeId: node.id, label: label || null },
      ]);
    }
    setConnectFrom(null);
  }

  function editNode(node: WhiteboardNode) {
    const text = window.prompt('Edit node text', node.text);
    if (text == null) return;
    onChange(nodes.map((n) => (n.id === node.id ? { ...n, text } : n)), edges);
  }

  function deleteNode(id: string) {
    onChange(
      nodes.filter((n) => n.id !== id),
      edges.filter((e) => e.fromNodeId !== id && e.toNodeId !== id),
    );
  }

  const byId = new Map(nodes.map((n) => [n.id, n]));

  return (
    <div className="whiteboard">
      <div className="editor-actions whiteboard-toolbar">
        <button onClick={addNode}>+ Node</button>
        <button
          className={connectFrom ? 'tab active' : 'tab'}
          onClick={() => setConnectFrom(connectFrom ? null : '')}
          title="Click this, then a source node, then a target node"
        >
          {connectFrom !== null ? 'Connecting… (pick nodes)' : 'Connect'}
        </button>
      </div>
      <div
        ref={ref}
        className="whiteboard-canvas"
        onMouseMove={onMouseMove}
        onMouseUp={endDrag}
        onMouseLeave={endDrag}
      >
        <svg className="whiteboard-edges">
          {edges.map((e) => {
            const a = byId.get(e.fromNodeId);
            const b = byId.get(e.toNodeId);
            if (!a || !b) return null;
            const x1 = a.x + NODE_W / 2;
            const y1 = a.y + NODE_H / 2;
            const x2 = b.x + NODE_W / 2;
            const y2 = b.y + NODE_H / 2;
            return (
              <g key={e.id}>
                <line x1={x1} y1={y1} x2={x2} y2={y2} stroke="#6d54c9" strokeWidth={2} />
                {e.label && (
                  <text x={(x1 + x2) / 2} y={(y1 + y2) / 2} className="rel-edge-label" textAnchor="middle">
                    {e.label}
                  </text>
                )}
              </g>
            );
          })}
        </svg>
        {nodes.map((n) => (
          <div
            key={n.id}
            className={`whiteboard-node${connectFrom === '' ? ' connectable' : ''}`}
            style={{ left: n.x, top: n.y, width: NODE_W, minHeight: NODE_H }}
            onMouseDown={(e) => onNodeMouseDown(e, n)}
            onClick={() => (connectFrom === '' ? setConnectFrom(n.id) : onNodeClick(n))}
            onDoubleClick={() => editNode(n)}
          >
            <span>{n.text}</span>
            <button
              className="link-button danger node-del"
              onClick={(e) => {
                e.stopPropagation();
                deleteNode(n.id);
              }}
            >
              ✕
            </button>
          </div>
        ))}
        {nodes.length === 0 && <p className="muted whiteboard-empty">Add a node to start plotting.</p>}
      </div>
    </div>
  );
}
