import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError, handoutsApi, Handout, HandoutPreset } from '../api/client';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { NewWindowPortal } from '../components/NewWindowPortal';
import { renderMarkdown } from '../lib/markdown';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

const PRESETS: { value: HandoutPreset; label: string }[] = [
  { value: 'PARCHMENT', label: 'Parchment note' },
  { value: 'NEWSPAPER', label: 'Newspaper page' },
  { value: 'POSTER', label: 'Wanted poster' },
  { value: 'LETTER', label: 'Personal letter' },
];

const EMPTY_DRAFT = { id: null as string | null, title: '', preset: 'PARCHMENT' as HandoutPreset, body: '' };

/**
 * FR-46: player-facing handouts — letters, wanted posters, in-world
 * newspaper pages — as styled one-page printables. The GM writes the text,
 * picks a preset, and prints a prop; deliberately separate from GM-only
 * article content.
 */
export function HandoutsView({ worldId, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { handoutId: urlHandoutId } = useParams<{ handoutId: string }>();
  const api = useMemo(() => handoutsApi(worldId), [worldId]);
  const [list, setList] = useState<Handout[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState(EMPTY_DRAFT);
  const [printing, setPrinting] = useState(false);
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
      const all = await api.list();
      setList(all);
      return all;
    } catch (err) {
      handleError(err);
      return [];
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  // Load the draft behind the URL-selected handout.
  useEffect(() => {
    if (!urlHandoutId) {
      setDraft(EMPTY_DRAFT);
      return;
    }
    api.get(urlHandoutId).then((h) => setDraft({
      id: h.id,
      title: h.title,
      preset: h.preset,
      body: h.body ?? '',
    })).catch(handleError);
  }, [urlHandoutId, api, handleError]);

  async function save(e: FormEvent) {
    e.preventDefault();
    const body = { title: draft.title, preset: draft.preset, body: draft.body || null };
    try {
      if (draft.id) {
        await api.update(draft.id, body);
      } else {
        const created = await api.create(body);
        navigate(`/worlds/${worldId}/handouts/${created.id}`);
      }
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  async function remove() {
    if (!draft.id) return;
    try {
      await api.remove(draft.id);
      navigate(`/worlds/${worldId}/handouts`);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  const editingExisting = draft.id != null;

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <Button
          onClick={() => {
            setDraft(EMPTY_DRAFT);
            navigate(`/worlds/${worldId}/handouts`);
          }}
          data-testid="new-handout-button"
        >
          + New handout
        </Button>
        <ul className="article-list">
          {list.map((h) => (
            <li key={h.id}>
              <button
                className={h.id === urlHandoutId ? 'article-link active' : 'article-link'}
                onClick={() => navigate(`/worlds/${worldId}/handouts/${h.id}`)}
              >
                <span title={h.title}>{h.title}</span>
                <small className="muted">
                  {PRESETS.find((p) => p.value === h.preset)?.label ?? h.preset}
                </small>
              </button>
            </li>
          ))}
          {loading && <li className="muted">Loading…</li>}
          {!loading && list.length === 0 && <li className="muted">No handouts yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        <form className="card" onSubmit={save}>
          <Input
            className="title-input"
            placeholder="Handout title"
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
            required
            data-testid="handout-title-input"
          />
          <div className="editor-actions">
            <Select value={draft.preset} onValueChange={(v) => setDraft({ ...draft, preset: v as HandoutPreset })}>
              <SelectTrigger title="Visual style">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {PRESETS.map((p) => (
                  <SelectItem key={p.value} value={p.value}>
                    {p.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <MarkdownEditor value={draft.body} onChange={(body) => setDraft({ ...draft, body })} />
          <div className="editor-actions">
            <Button type="submit" disabled={!draft.title}>
              {editingExisting ? 'Save handout' : 'Create handout'}
            </Button>
            {(editingExisting || draft.body) && (
              <Button type="button" variant="outline" onClick={() => setPrinting(true)} disabled={!draft.title}>
                🖨 Print
              </Button>
            )}
            {editingExisting && (
              <Button
                type="button"
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => void remove()}
              >
                Delete
              </Button>
            )}
          </div>
        </form>

        {/* Live preview in the chosen style */}
        <article className={`handout-doc preview-${draft.preset.toLowerCase()}`}>
          <h2 className="handout-title">{draft.title || '(untitled)'}</h2>
          {/* eslint-disable-next-line react/no-danger */}
          <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(draft.body) }} />
        </article>
      </div>

      {printing && (
        <NewWindowPortal title={`Print — ${draft.title}`} onClose={() => setPrinting(false)}>
          <div className="print-toolbar">
            <strong>Handout</strong>
            <span className="print-toolbar-spacer" />
            <Button onClick={() => window.print()}>🖨 Print</Button>
            <Button variant="link" onClick={() => setPrinting(false)}>
              Close
            </Button>
          </div>
          <div className="print-doc handout-print">
            <article className={`handout-doc ${draft.preset.toLowerCase()}`}>
              <h2 className="handout-title">{draft.title}</h2>
              {/* eslint-disable-next-line react/no-danger */}
              <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(draft.body) }} />
            </article>
          </div>
        </NewWindowPortal>
      )}
    </div>
  );
}
