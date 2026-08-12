import { useEffect, useMemo, useRef, useState } from 'react';
import { Relationship } from '../api/client';

interface GraphNode {
  id: string;
  label: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
}

interface Props {
  nodeLabels: Map<string, string>;
  relationships: Relationship[];
  onSelectNode: (articleId: string) => void;
}

const WIDTH = 640;
const HEIGHT = 460;

/**
 * Dependency-free force-directed graph in SVG. Runs a short simulation
 * (repulsion + spring + centering) then renders nodes for articles that
 * participate in at least one relationship.
 */
export function RelationshipGraph({ nodeLabels, relationships, onSelectNode }: Props) {
  const [nodes, setNodes] = useState<GraphNode[]>([]);
  const frame = useRef<number>();

  // Node set = articles that appear in an edge.
  const nodeIds = useMemo(() => {
    const ids = new Set<string>();
    relationships.forEach((r) => {
      ids.add(r.fromArticleId);
      ids.add(r.toArticleId);
    });
    return Array.from(ids);
  }, [relationships]);

  useEffect(() => {
    // Seed nodes on a circle for a stable starting layout.
    const seeded: GraphNode[] = nodeIds.map((id, i) => {
      const angle = (2 * Math.PI * i) / Math.max(nodeIds.length, 1);
      return {
        id,
        label: nodeLabels.get(id) ?? 'Article',
        x: WIDTH / 2 + Math.cos(angle) * 160,
        y: HEIGHT / 2 + Math.sin(angle) * 160,
        vx: 0,
        vy: 0,
      };
    });

    let working = seeded;
    let ticks = 0;
    const step = () => {
      working = simulate(working, relationships);
      setNodes([...working]);
      ticks += 1;
      if (ticks < 200) frame.current = requestAnimationFrame(step);
    };
    frame.current = requestAnimationFrame(step);
    return () => {
      if (frame.current) cancelAnimationFrame(frame.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nodeIds, relationships]);

  const pos = new Map(nodes.map((n) => [n.id, n]));

  if (nodeIds.length === 0) {
    return <p className="muted">No relationships yet. Add one to see the web.</p>;
  }

  return (
    <svg className="rel-graph" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label="Relationship graph">
      <defs>
        <marker id="arrow" viewBox="0 0 10 10" refX="18" refY="5" markerWidth="6" markerHeight="6"
                orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" fill="#6d54c9" />
        </marker>
      </defs>
      {relationships.map((r) => {
        const a = pos.get(r.fromArticleId);
        const b = pos.get(r.toArticleId);
        if (!a || !b) return null;
        const mx = (a.x + b.x) / 2;
        const my = (a.y + b.y) / 2;
        return (
          <g key={r.id}>
            <line
              x1={a.x}
              y1={a.y}
              x2={b.x}
              y2={b.y}
              stroke="#3a3550"
              strokeWidth={1.5}
              markerEnd={r.directed ? 'url(#arrow)' : undefined}
            />
            {r.label && (
              <text x={mx} y={my} className="rel-edge-label" textAnchor="middle">
                {r.label}
              </text>
            )}
          </g>
        );
      })}
      {nodes.map((n) => (
        <g key={n.id} className="rel-node" onClick={() => onSelectNode(n.id)}>
          <circle cx={n.x} cy={n.y} r={8} fill="#6d54c9" stroke="#14121a" strokeWidth={2} />
          <text x={n.x} y={n.y - 12} textAnchor="middle" className="rel-node-label">
            {n.label}
          </text>
        </g>
      ))}
    </svg>
  );
}

/** One iteration of a simple force simulation. */
function simulate(nodes: GraphNode[], edges: Relationship[]): GraphNode[] {
  const REPULSION = 6000;
  const SPRING = 0.01;
  const REST_LENGTH = 120;
  const CENTER = 0.005;
  const DAMPING = 0.85;

  const byId = new Map(nodes.map((n) => [n.id, { ...n }]));

  // Repulsion between all pairs.
  const list = Array.from(byId.values());
  for (let i = 0; i < list.length; i++) {
    for (let j = i + 1; j < list.length; j++) {
      const a = list[i];
      const b = list[j];
      let dx = a.x - b.x;
      let dy = a.y - b.y;
      let distSq = dx * dx + dy * dy || 0.01;
      const force = REPULSION / distSq;
      const dist = Math.sqrt(distSq);
      const fx = (dx / dist) * force;
      const fy = (dy / dist) * force;
      a.vx += fx;
      a.vy += fy;
      b.vx -= fx;
      b.vy -= fy;
    }
  }

  // Spring along edges.
  edges.forEach((e) => {
    const a = byId.get(e.fromArticleId);
    const b = byId.get(e.toArticleId);
    if (!a || !b) return;
    const dx = b.x - a.x;
    const dy = b.y - a.y;
    const dist = Math.sqrt(dx * dx + dy * dy) || 0.01;
    const force = SPRING * (dist - REST_LENGTH);
    const fx = (dx / dist) * force;
    const fy = (dy / dist) * force;
    a.vx += fx;
    a.vy += fy;
    b.vx -= fx;
    b.vy -= fy;
  });

  // Centering + integrate.
  list.forEach((n) => {
    n.vx += (WIDTH / 2 - n.x) * CENTER;
    n.vy += (HEIGHT / 2 - n.y) * CENTER;
    n.vx *= DAMPING;
    n.vy *= DAMPING;
    n.x = Math.max(16, Math.min(WIDTH - 16, n.x + n.vx));
    n.y = Math.max(24, Math.min(HEIGHT - 16, n.y + n.vy));
  });

  return list;
}
