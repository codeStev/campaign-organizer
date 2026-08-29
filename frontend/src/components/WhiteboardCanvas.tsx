import { useRef, useState } from 'react';
import { WhiteboardNode, WhiteboardEdge } from '../api/client';
import { Button } from './ui/button';
import { ConfirmDeleteDialog } from './ConfirmDeleteDialog';
import { PromptDialog } from './PromptDialog';

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
  // null = the node-text dialog is closed, '' = adding a new node, an id =
  // editing that node's existing text.
  const [nodeDialogTarget, setNodeDialogTarget] = useState<string | null>(null);
  const [pendingConnection, setPendingConnection] = useState<{ from: string; to: string } | null>(null);

  function addNode() {
    setNodeDialogTarget('');
  }

  function submitNodeText(text: string) {
    if (nodeDialogTarget) {
      onChange(nodes.map((n) => (n.id === nodeDialogTarget ? { ...n, text } : n)), edges);
    } else {
      const n: WhiteboardNode = { id: uid(), text, x: 40, y: 40, color: '#6d54c9' };
      onChange([...nodes, n], edges);
    }
  }

  function submitConnectionLabel(label: string) {
    if (!pendingConnection) return;
    onChange(nodes, [
      ...edges,
      { id: uid(), fromNodeId: pendingConnection.from, toNodeId: pendingConnection.to, label: label || null },
    ]);
  }

  // Pointer Events cover mouse, touch, and pen through one code path (unlike the
  // Mouse Events this replaced, which never fired for touch input at all).
  function onNodePointerDown(e: React.PointerEvent, node: WhiteboardNode) {
    if (connectFrom !== null) return; // connect mode handles clicks separately
    const rect = ref.current!.getBoundingClientRect();
    drag.current = { id: node.id, dx: e.clientX - rect.left - node.x, dy: e.clientY - rect.top - node.y };
    e.currentTarget.setPointerCapture(e.pointerId);
  }

  function onPointerMove(e: React.PointerEvent) {
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
      setPendingConnection({ from: connectFrom, to: node.id });
    }
    setConnectFrom(null);
  }

  function editNode(node: WhiteboardNode) {
    setNodeDialogTarget(node.id);
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
        <Button onClick={addNode}>+ Node</Button>
        <button
          className={connectFrom ? 'tab active' : 'tab'}
          onClick={() => setConnectFrom(connectFrom ? null : '')}
          title="Click this, then a source node, then a target node"
        >
          {connectFrom !== null ? 'Connecting… (pick nodes)' : 'Connect'}
        </button>
      </div>
      <PromptDialog
        open={nodeDialogTarget !== null}
        onOpenChange={(open) => !open && setNodeDialogTarget(null)}
        title={nodeDialogTarget ? 'Edit node text' : 'New node'}
        label="Node text"
        defaultValue={nodeDialogTarget ? (byId.get(nodeDialogTarget)?.text ?? '') : ''}
        onSubmit={submitNodeText}
      />
      <PromptDialog
        open={pendingConnection !== null}
        onOpenChange={(open) => !open && setPendingConnection(null)}
        title="Connection label"
        label="Label (optional)"
        optional
        onSubmit={submitConnectionLabel}
      />
      <div
        ref={ref}
        className="whiteboard-canvas"
        onPointerMove={onPointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
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
            onPointerDown={(e) => onNodePointerDown(e, n)}
            onClick={() => (connectFrom === '' ? setConnectFrom(n.id) : onNodeClick(n))}
            onDoubleClick={() => editNode(n)}
          >
            <span>{n.text}</span>
            <ConfirmDeleteDialog
              trigger={
                <Button
                  variant="link"
                  className="node-del text-destructive hover:text-destructive"
                  onClick={(e) => e.stopPropagation()}
                >
                  ✕
                </Button>
              }
              title="Delete node?"
              description={`This removes "${n.text}" and any edges connected to it from the board.`}
              onConfirm={() => deleteNode(n.id)}
            />
          </div>
        ))}
        {nodes.length === 0 && <p className="muted whiteboard-empty">Add a node to start plotting.</p>}
      </div>
    </div>
  );
}
