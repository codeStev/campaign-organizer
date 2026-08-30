import { Checkbox } from './ui/checkbox';

/** A node in a hierarchical print-selection tree (file-explorer style). */
export interface CheckTreeNode {
  /** Composite `${kind}:${id}` — namespaced so ids from different entity
   *  kinds can never collide in a single excluded-ids Set. */
  id: string;
  label: string;
  children: CheckTreeNode[];
}

export function subtreeNodeIds(node: CheckTreeNode): string[] {
  return [node.id, ...node.children.flatMap(subtreeNodeIds)];
}

export function nodeCheckState(
  node: CheckTreeNode,
  excludedIds: Set<string>,
): boolean | 'indeterminate' {
  const ids = subtreeNodeIds(node);
  const includedCount = ids.filter((id) => !excludedIds.has(id)).length;
  if (includedCount === 0) return false;
  if (includedCount === ids.length) return true;
  return 'indeterminate';
}

export function CheckTreeRow({
  node,
  excludedIds,
  onToggle,
}: {
  node: CheckTreeNode;
  excludedIds: Set<string>;
  onToggle: (ids: string[], checked: boolean) => void;
}) {
  const state = nodeCheckState(node, excludedIds);
  return (
    <li>
      <label className="print-check">
        <Checkbox
          checked={state}
          onCheckedChange={(checked) => onToggle(subtreeNodeIds(node), checked === true)}
        />
        {node.label}
      </label>
      {node.children.length > 0 && (
        <ul className="check-tree-nested">
          {node.children.map((child) => (
            <CheckTreeRow key={child.id} node={child} excludedIds={excludedIds} onToggle={onToggle} />
          ))}
        </ul>
      )}
    </li>
  );
}
