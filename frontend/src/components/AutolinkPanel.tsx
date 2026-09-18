import { useState } from 'react';
import { toast } from 'sonner';
import {
  autolinkApi,
  AutolinkCandidateGroup,
  AutolinkMatch,
  AutolinkSelection,
  ApiError,
} from '../api/client';
import { Button } from './ui/button';
import { Label } from './ui/label';
import { RadioGroup, RadioGroupItem } from './ui/radio-group';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
  /** A group's mentions were applied - a freshly-linked mention may resolve
   * an existing broken-link/orphan finding, so the caller should re-check. */
  onApplied: () => void;
}

const SKIP = '__skip__';

function occurrenceKey(articleId: string, match: AutolinkMatch): string {
  return `${articleId}:${match.targetArticleId}:${match.occurrenceIndex}`;
}

/**
 * Manually-triggered auto-link scan (ADR-0116): plain-text mentions of
 * other articles' titles/aliases that aren't linked yet, grouped by source
 * article, reviewed one occurrence at a time before anything is converted.
 * Each occurrence defaults to its target's canonical name but can be
 * switched to any of its aliases, or skipped entirely.
 */
export function AutolinkPanel({ worldId, onOpenArticle, onAuthExpired, onApplied }: Props) {
  const [groups, setGroups] = useState<AutolinkCandidateGroup[] | null>(null);
  const [scanning, setScanning] = useState(false);
  const [applyingArticleId, setApplyingArticleId] = useState<string | null>(null);
  const [selections, setSelections] = useState<Map<string, string>>(new Map());
  const [error, setError] = useState<string | null>(null);

  function handleError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) return onAuthExpired();
    setError(err instanceof Error ? err.message : 'Something went wrong');
  }

  async function scan() {
    setScanning(true);
    setError(null);
    try {
      const result = await autolinkApi(worldId).scan();
      setGroups(result);
      setSelections((prev) => {
        const next = new Map(prev);
        for (const group of result) {
          for (const match of group.matches) {
            const key = occurrenceKey(group.articleId, match);
            if (!next.has(key)) next.set(key, match.candidateNames[0]);
          }
        }
        return next;
      });
    } catch (err) {
      handleError(err);
    } finally {
      setScanning(false);
    }
  }

  async function applyGroup(group: AutolinkCandidateGroup) {
    const selectionsForGroup: AutolinkSelection[] = [];
    for (const match of group.matches) {
      const chosen = selections.get(occurrenceKey(group.articleId, match));
      if (chosen && chosen !== SKIP) {
        selectionsForGroup.push({
          targetArticleId: match.targetArticleId,
          occurrenceIndex: match.occurrenceIndex,
          chosenName: chosen,
        });
      }
    }
    if (selectionsForGroup.length === 0) return;
    setApplyingArticleId(group.articleId);
    try {
      await autolinkApi(worldId).apply(group.articleId, selectionsForGroup);
      toast.success(
        `Linked ${selectionsForGroup.length} mention${selectionsForGroup.length === 1 ? '' : 's'} in "${group.articleTitle}"`,
      );
      onApplied();
      await scan();
    } catch (err) {
      handleError(err);
    } finally {
      setApplyingArticleId(null);
    }
  }

  return (
    <div className="card">
      <div className="form-actions">
        <h3 style={{ margin: 0 }}>Auto-link scan</h3>
        <span className="print-toolbar-spacer" />
        <Button variant="outline" onClick={() => void scan()} disabled={scanning}>
          {scanning ? 'Scanning…' : groups === null ? 'Scan for auto-link candidates' : '↻ Re-scan'}
        </Button>
      </div>
      <p className="muted">
        Finds plain-text mentions of other articles' names that aren't linked yet - nothing is
        converted until you pick and apply it below.
      </p>
      {error && <p className="error">{error}</p>}

      {groups !== null && groups.length === 0 && (
        <p className="muted">No unlinked mentions found.</p>
      )}

      {groups?.map((group) => (
        <div key={group.articleId} className="autolink-group">
          <div className="form-actions">
            <Button variant="link" className="autolink-source-link" onClick={() => onOpenArticle(group.articleId)}>
              {group.articleTitle}
            </Button>
            <span className="print-toolbar-spacer" />
            <Button
              size="sm"
              onClick={() => void applyGroup(group)}
              disabled={applyingArticleId === group.articleId}
            >
              {applyingArticleId === group.articleId ? 'Applying…' : 'Apply selected'}
            </Button>
          </div>
          {group.matches.map((match) => {
            const key = occurrenceKey(group.articleId, match);
            const value = selections.get(key) ?? SKIP;
            return (
              <div key={key} className="autolink-match">
                <p className="muted autolink-snippet">{match.snippet}</p>
                <RadioGroup
                  value={value}
                  onValueChange={(v) => setSelections((prev) => new Map(prev).set(key, v))}
                >
                  <div className="autolink-option">
                    <RadioGroupItem value={SKIP} id={`${key}-skip`} />
                    <Label htmlFor={`${key}-skip`}>Don't link this occurrence</Label>
                  </div>
                  {match.candidateNames.map((name) => (
                    <div key={name} className="autolink-option" data-testid="autolink-candidate">
                      <RadioGroupItem value={name} id={`${key}-${name}`} />
                      <Label htmlFor={`${key}-${name}`}>
                        Link as <code>[[{name}|{match.matchedText}]]</code>
                      </Label>
                    </div>
                  ))}
                </RadioGroup>
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
