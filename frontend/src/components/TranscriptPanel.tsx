import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { sessionTranscriptApi, SessionTranscript, TranscriptLine } from '../api/client';
import { Button } from './ui/button';
import { ConfirmDialog } from './ConfirmDialog';

interface Props {
  worldId: string;
  campaignId: string;
  sessionId: string;
  onError: (err: unknown) => void;
}

function clock(offsetMs: number): string {
  const total = Math.floor(offsetMs / 1000);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  const two = (n: number) => String(n).padStart(2, '0');
  return hours > 0 ? `${hours}:${two(minutes)}:${two(seconds)}` : `${minutes}:${two(seconds)}`;
}

function Line({ line }: { line: TranscriptLine }) {
  return (
    <li className="flex gap-2">
      <span className="muted shrink-0 tabular-nums">{clock(line.offsetMs)}</span>
      {line.kind === 'IN_CHARACTER' ? (
        <span>
          {line.speaker && <strong>{line.speaker}: </strong>}
          {line.text}
        </span>
      ) : (
        <em>{line.text}</em>
      )}
    </li>
  );
}

/**
 * The story lines (in-character and narration only) a companion app exported for the session
 * (ADR-0121). Read-only; shows nothing until a transcript exists. The GM can delete it.
 */
export function TranscriptPanel({ worldId, campaignId, sessionId, onError }: Props) {
  const api = useMemo(
    () => sessionTranscriptApi(worldId, campaignId, sessionId),
    [worldId, campaignId, sessionId],
  );
  const [transcript, setTranscript] = useState<SessionTranscript | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const refresh = useCallback(async () => {
    try {
      setTranscript(await api.get());
    } catch (err) {
      onError(err);
    }
  }, [api, onError]);

  useEffect(() => {
    setTranscript(null);
    void refresh();
  }, [refresh]);

  async function handleDelete() {
    try {
      await api.remove();
      toast.success('Transcript deleted');
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  if (transcript === null || transcript.lines.length === 0) return null;

  return (
    <details className="card">
      <summary>
        <strong className="muted">Transcript</strong>{' '}
        <span className="muted">
          ({transcript.lines.length} lines
          {transcript.updatedAt && `, exported ${new Date(transcript.updatedAt).toLocaleString()}`})
        </span>
      </summary>
      <ul className="mt-2 flex flex-col gap-1">
        {transcript.lines.map((line, index) => (
          <Line key={index} line={line} />
        ))}
      </ul>
      <div className="editor-actions">
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="text-destructive hover:text-destructive"
          onClick={() => setConfirmDelete(true)}
        >
          Delete transcript
        </Button>
      </div>
      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Delete this transcript?"
        description="It is removed from the organizer. The next export from DiscordWhispr would add it again."
        confirmLabel="Delete"
        destructive
        onConfirm={() => {
          setConfirmDelete(false);
          void handleDelete();
        }}
      />
    </details>
  );
}
