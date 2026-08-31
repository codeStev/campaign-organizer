import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { clocksApi, Clock, ClockRequest, ClockSegment } from '../api/client';
import { Button } from '../components/ui/button';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Input } from '../components/ui/input';
import { Toggle } from '../components/ui/toggle';
import { Spinner } from '../components/ui/spinner';
import { toast } from 'sonner';

interface Props {
  worldId: string;
  campaignId: string;
  onError: (err: unknown) => void;
}

function blankSegments(count: number): ClockSegment[] {
  return Array.from({ length: Math.max(1, count) }, () => ({ filled: false, title: null, description: null }));
}

/** Clicking pip n fills segments 1..n (clicking the last filled pip clears it) - mirrors CircleTracker's interaction. */
function withToggledSegment(segments: ClockSegment[], index: number): ClockSegment[] {
  const filledCount = segments.filter((s) => s.filled).length;
  const n = index + 1;
  const newFilledCount = filledCount === n ? n - 1 : n;
  return segments.map((s, i) => ({ ...s, filled: i < newFilledCount }));
}

export function ClockBoard({ worldId, campaignId, onError }: Props) {
  const api = useMemo(() => clocksApi(worldId, campaignId), [worldId, campaignId]);
  const [clocks, setClocks] = useState<Clock[]>([]);
  const [loading, setLoading] = useState(true);
  const [newTitle, setNewTitle] = useState('');
  const [newSegmentCount, setNewSegmentCount] = useState(6);

  const refresh = useCallback(async () => {
    try {
      setClocks(await api.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function addClock(e: FormEvent) {
    e.preventDefault();
    if (!newTitle) return;
    try {
      const created = await api.create({ title: newTitle, segments: blankSegments(newSegmentCount) });
      setNewTitle('');
      await refresh();
      toast.success(`Clock "${created.title}" created`);
    } catch (err) {
      onError(err);
    }
  }

  async function saveClock(clock: Clock, request: ClockRequest) {
    try {
      const updated = await api.update(clock.id, request);
      setClocks((cs) => cs.map((c) => (c.id === clock.id ? updated : c)));
    } catch (err) {
      onError(err);
    }
  }

  async function removeClock(clock: Clock) {
    try {
      await api.remove(clock.id);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  return (
    <section className="card">
      <h3>Clocks</h3>
      <form className="editor-actions" onSubmit={addClock}>
        <Input placeholder="New clock title" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} />
        <Input
          type="number"
          min={1}
          max={24}
          value={newSegmentCount}
          title="Number of segments"
          onChange={(e) => setNewSegmentCount(Math.max(1, Number(e.target.value) || 1))}
        />
        <Button type="submit" disabled={!newTitle}>
          Add clock
        </Button>
      </form>

      <div className="arc-list">
        {clocks.map((clock) => (
          <ClockCard
            key={clock.id}
            clock={clock}
            onSave={(request) => saveClock(clock, request)}
            onRemove={() => removeClock(clock)}
          />
        ))}
        {loading && (
          <p className="muted loading-row">
            <Spinner /> Loading…
          </p>
        )}
        {!loading && clocks.length === 0 && <p className="muted">No clocks yet.</p>}
      </div>
    </section>
  );
}

interface ClockCardProps {
  clock: Clock;
  onSave: (request: ClockRequest) => void;
  onRemove: () => void;
}

function ClockCard({ clock, onSave, onRemove }: ClockCardProps) {
  const [open, setOpen] = useState(false);
  const filledCount = clock.segments.filter((s) => s.filled).length;

  function togglePip(index: number) {
    onSave({
      title: clock.title,
      description: clock.description,
      segments: withToggledSegment(clock.segments, index),
      position: clock.position,
    });
  }

  function setDescription(description: string) {
    onSave({ title: clock.title, description: description || null, segments: clock.segments, position: clock.position });
  }

  function setSegmentTitle(index: number, title: string) {
    const segments = clock.segments.map((s, i) => (i === index ? { ...s, title: title || null } : s));
    onSave({ title: clock.title, description: clock.description, segments, position: clock.position });
  }

  function setSegmentDescription(index: number, description: string) {
    const segments = clock.segments.map((s, i) => (i === index ? { ...s, description: description || null } : s));
    onSave({ title: clock.title, description: clock.description, segments, position: clock.position });
  }

  function addSegment() {
    onSave({
      title: clock.title,
      description: clock.description,
      segments: [...clock.segments, { filled: false, title: null, description: null }],
      position: clock.position,
    });
  }

  function removeLastSegment() {
    if (clock.segments.length <= 1) return;
    onSave({
      title: clock.title,
      description: clock.description,
      segments: clock.segments.slice(0, -1),
      position: clock.position,
    });
  }

  return (
    <div className="arc-card">
      <div className="arc-head">
        <button className="arc-toggle" onClick={() => setOpen((v) => !v)}>
          <span className="caret">{open ? '▼' : '▶'}</span>
          <strong>{clock.title}</strong>
        </button>
        <span className="muted">
          {filledCount}/{clock.segments.length}
        </span>
        <div className="circle-tracker">
          {clock.segments.map((segment, i) => (
            <Toggle
              key={i}
              type="button"
              className={segment.filled ? 'pip on' : 'pip'}
              title={segment.title || `${i + 1}`}
              pressed={segment.filled}
              onPressedChange={() => togglePip(i)}
            />
          ))}
        </div>
        <ConfirmDeleteDialog
          trigger={
            <Button variant="link" className="text-destructive hover:text-destructive">
              ✕
            </Button>
          }
          title="Delete clock?"
          description={`This permanently deletes "${clock.title}" and cannot be undone.`}
          onConfirm={onRemove}
        />
      </div>

      {open && (
        <div className="arc-beats">
          <Input
            placeholder="Description"
            defaultValue={clock.description ?? ''}
            onBlur={(e) => setDescription(e.target.value)}
          />
          <ul className="beat-list">
            {clock.segments.map((segment, i) => (
              <li key={i} className="beat-item">
                <div className="beat-row">
                  <Toggle
                    type="button"
                    className={segment.filled ? 'pip on' : 'pip'}
                    pressed={segment.filled}
                    onPressedChange={() => togglePip(i)}
                  />
                  <Input
                    placeholder={`Segment ${i + 1} title (optional)`}
                    defaultValue={segment.title ?? ''}
                    onBlur={(e) => setSegmentTitle(i, e.target.value)}
                  />
                  <Input
                    placeholder="Description (optional)"
                    defaultValue={segment.description ?? ''}
                    onBlur={(e) => setSegmentDescription(i, e.target.value)}
                  />
                </div>
              </li>
            ))}
          </ul>
          <div className="editor-actions">
            <Button type="button" variant="link" onClick={addSegment}>
              + Add segment
            </Button>
            <Button
              type="button"
              variant="link"
              className="text-destructive hover:text-destructive"
              onClick={removeLastSegment}
              disabled={clock.segments.length <= 1}
            >
              − Remove last segment
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
