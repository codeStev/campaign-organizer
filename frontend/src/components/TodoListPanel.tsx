import { KeyboardEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { campaignTodosApi, sessionTodosApi, Todo } from '../api/client';
import { Button } from './ui/button';
import { Checkbox } from './ui/checkbox';
import { Input } from './ui/input';
import { Spinner } from './ui/spinner';
import { toast } from 'sonner';

interface Props {
  worldId: string;
  campaignId: string;
  /** When set, this panel manages that session's todos; otherwise the campaign's standing todos. */
  sessionId?: string;
  onError: (err: unknown) => void;
}

/** A lightweight GM task list - text plus a done checkbox (ADR-0092). */
export function TodoListPanel({ worldId, campaignId, sessionId, onError }: Props) {
  const listApi = useMemo(
    () => (sessionId ? sessionTodosApi(worldId, campaignId, sessionId) : campaignTodosApi(worldId, campaignId)),
    [worldId, campaignId, sessionId],
  );
  // Update/delete for a todo always go through the campaign-scoped route,
  // regardless of whether it's a standing or session-attached todo.
  const writeApi = useMemo(() => campaignTodosApi(worldId, campaignId), [worldId, campaignId]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState('');

  const refresh = useCallback(async () => {
    try {
      setTodos(await listApi.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [listApi, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function addTodo() {
    const trimmed = text.trim();
    if (!trimmed) return;
    try {
      await listApi.create({ text: trimmed });
      setText('');
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      e.preventDefault();
      void addTodo();
    }
  }

  async function toggleDone(todo: Todo, done: boolean) {
    try {
      await writeApi.update(todo.id, { text: todo.text, done });
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function removeTodo(todo: Todo) {
    try {
      await writeApi.remove(todo.id);
      await refresh();
      toast.success('Todo removed');
    } catch (err) {
      onError(err);
    }
  }

  return (
    <div className="loose-threads">
      <span className="muted">{sessionId ? 'Session todos' : 'Campaign todos'}</span>
      <ul className="beat-list">
        {todos.map((t) => (
          <li key={t.id} className="beat-item">
            <div className="beat-row">
              <Checkbox checked={t.done} onCheckedChange={(checked) => toggleDone(t, checked === true)} />
              <span className={t.done ? 'beat-done' : ''}>{t.text}</span>
              <span className="bf-spacer" />
              <Button
                type="button"
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => removeTodo(t)}
              >
                ✕
              </Button>
            </div>
          </li>
        ))}
        {loading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!loading && todos.length === 0 && <li className="muted">No todos yet.</li>}
      </ul>
      <div className="beat-form">
        <Input
          placeholder="Something to do… (Enter to add)"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <Button type="button" disabled={!text.trim()} onClick={() => void addTodo()}>
          Add
        </Button>
      </div>
    </div>
  );
}
