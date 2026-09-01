import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError, handoutsApi, mediaApi, campaignsApi, sessionsApi, Handout, HandoutPreset } from '../api/client';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';
import { renderMarkdown } from '../lib/markdown';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { Toggle } from '../components/ui/toggle';
import { toast } from 'sonner';
import { Spinner } from '../components/ui/spinner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';

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

const EMPTY_DRAFT = {
  id: null as string | null,
  title: '',
  preset: 'PARCHMENT' as HandoutPreset,
  body: '',
  sessionId: null as string | null,
  revealed: false,
};

// Radix Select can't use "" as an item value (reserved for "no selection").
const NO_SESSION = '__none__';

interface SessionOption {
  id: string;
  label: string;
}

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
  const media = useMemo(() => mediaApi(worldId), [worldId]);
  const [list, setList] = useState<Handout[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState(EMPTY_DRAFT);
  const [printing, setPrinting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sessionOptions, setSessionOptions] = useState<SessionOption[]>([]);
  // Read (rendered preview) vs edit (the form) — mirrors WorldView's article mode.
  const [mode, setMode] = useState<'read' | 'edit'>('read');
  const { opts: printOpts, setOpts: setPrintOpts, docProps: printDocProps } = usePrintOptions();

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

  // Session picker options, flattened across every campaign in the world.
  useEffect(() => {
    let active = true;
    campaignsApi(worldId)
      .list()
      .then(async (campaigns) => {
        const perCampaign = await Promise.all(
          campaigns.map(async (c) => {
            const sessions = await sessionsApi(worldId, c.id).list();
            return sessions.map((s) => ({
              id: s.id,
              label: `${c.name} — ${s.sessionNumber != null ? `Session ${s.sessionNumber}: ` : ''}${s.title}`,
            }));
          }),
        );
        if (active) setSessionOptions(perCampaign.flat());
      })
      .catch(() => {
        /* Session tagging is optional; the editor works without the picker. */
      });
    return () => {
      active = false;
    };
  }, [worldId]);

  function loadDraft(h: Handout) {
    setDraft({
      id: h.id,
      title: h.title,
      preset: h.preset,
      body: h.body ?? '',
      sessionId: h.sessionId ?? null,
      revealed: h.revealed,
    });
    setMode('read');
  }

  // Load the draft behind the URL-selected handout.
  useEffect(() => {
    if (!urlHandoutId) {
      setDraft(EMPTY_DRAFT);
      return;
    }
    api.get(urlHandoutId).then(loadDraft).catch(handleError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlHandoutId, api, handleError]);

  async function save(e: FormEvent) {
    e.preventDefault();
    const body = {
      title: draft.title,
      preset: draft.preset,
      body: draft.body || null,
      sessionId: draft.sessionId,
      revealed: draft.revealed,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      loadDraft(saved);
      if (!draft.id) navigate(`/worlds/${worldId}/handouts/${saved.id}`);
      await refresh();
      toast.success(`Handout "${draft.title}" saved`);
    } catch (err) {
      handleError(err);
    }
  }

  async function move(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= list.length) return;
    const next = [...list];
    [next[index], next[target]] = [next[target], next[index]];
    setList(next);
    try {
      await api.reorder(next.map((h) => h.id));
    } catch (err) {
      handleError(err);
      await refresh();
    }
  }

  async function toggleRevealed(h: Handout) {
    const revealed = !h.revealed;
    setList((prev) => prev.map((x) => (x.id === h.id ? { ...x, revealed } : x)));
    if (draft.id === h.id) setDraft((d) => ({ ...d, revealed }));
    try {
      await api.update(h.id, {
        title: h.title,
        preset: h.preset,
        body: h.body ?? null,
        sessionId: h.sessionId ?? null,
        revealed,
      });
    } catch (err) {
      handleError(err);
      await refresh();
    }
  }

  async function duplicate() {
    if (!draft.id) return;
    try {
      const copy = await api.duplicate(draft.id);
      await refresh();
      navigate(`/worlds/${worldId}/handouts/${copy.id}`);
      toast.success(`Handout "${copy.title}" created`);
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
            setMode('edit');
            navigate(`/worlds/${worldId}/handouts`);
          }}
          data-testid="new-handout-button"
        >
          + New handout
        </Button>
        <ul className="article-list">
          {list.map((h, i) => (
            <li key={h.id} className="handout-list-row">
              <div className="cheatsheet-order">
                <Button variant="link" onClick={() => void move(i, -1)} disabled={i === 0} title="Move up">
                  ↑
                </Button>
                <Button
                  variant="link"
                  onClick={() => void move(i, 1)}
                  disabled={i === list.length - 1}
                  title="Move down"
                >
                  ↓
                </Button>
              </div>
              <button
                className={h.id === urlHandoutId ? 'article-link active' : 'article-link'}
                onClick={() => navigate(`/worlds/${worldId}/handouts/${h.id}`)}
              >
                <TruncatedLabel label={h.title}>{h.title}</TruncatedLabel>
                <small className="muted">
                  {PRESETS.find((p) => p.value === h.preset)?.label ?? h.preset}
                </small>
              </button>
              <Toggle
                type="button"
                size="sm"
                pressed={h.revealed}
                onPressedChange={() => void toggleRevealed(h)}
                title={h.revealed ? 'Revealed to players — click to mark secret' : 'Not yet revealed — click to mark revealed'}
                aria-label="Revealed to players"
              >
                {h.revealed ? '👁' : '🔒'}
              </Toggle>
            </li>
          ))}
          {loading && (
            <li className="muted loading-row">
              <Spinner /> Loading…
            </li>
          )}
          {!loading && list.length === 0 && <li className="muted">No handouts yet.</li>}
        </ul>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}

        {(mode === 'edit' || !editingExisting) && (
          <>
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
                <Select
                  value={draft.sessionId ?? NO_SESSION}
                  onValueChange={(v) => {
                    // Radix's hidden native-<select> bubble fires onValueChange('')
                    // on its own during (re)mount - not a real user selection, and
                    // our own items never carry an empty-string value, so ignore it.
                    if (v === '') return;
                    setDraft({ ...draft, sessionId: v === NO_SESSION ? null : v });
                  }}
                >
                  <SelectTrigger title="Session">
                    <SelectValue placeholder="No session" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NO_SESSION}>No session</SelectItem>
                    {sessionOptions.map((s) => (
                      <SelectItem key={s.id} value={s.id}>
                        {s.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <MarkdownEditor
                value={draft.body}
                onChange={(body) => setDraft({ ...draft, body })}
                onUploadImage={async (file) => (await media.upload(file)).url}
              />
              <div className="editor-actions">
                <Button type="submit" disabled={!draft.title}>
                  {editingExisting ? 'Save handout' : 'Create handout'}
                </Button>
                {editingExisting && (
                  <Button
                    type="button"
                    variant="link"
                    onClick={() => {
                      const saved = list.find((h) => h.id === draft.id);
                      if (saved) loadDraft(saved);
                      else setMode('read');
                    }}
                  >
                    Cancel
                  </Button>
                )}
                {(editingExisting || draft.body) && (
                  <Button type="button" variant="outline" onClick={() => setPrinting(true)} disabled={!draft.title}>
                    🖨 Print
                  </Button>
                )}
                {editingExisting && (
                  <Button type="button" variant="link" onClick={() => void duplicate()}>
                    Duplicate
                  </Button>
                )}
                {editingExisting && (
                  <ConfirmDeleteDialog
                    trigger={
                      <Button type="button" variant="link" className="text-destructive hover:text-destructive">
                        Delete
                      </Button>
                    }
                    title="Delete handout?"
                    description={`This permanently deletes "${draft.title}" and cannot be undone.`}
                    onConfirm={() => void remove()}
                  />
                )}
              </div>
            </form>

            {/* Live preview in the chosen style */}
            <article className={`handout-doc preview-${draft.preset.toLowerCase()}`}>
              <h2 className="handout-title">{draft.title || '(untitled)'}</h2>
              {/* eslint-disable-next-line react/no-danger */}
              <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(draft.body) }} />
            </article>
          </>
        )}

        {mode === 'read' && editingExisting && (
          <article className="article-read">
            <div className="article-read-head">
              <h2>{draft.title}</h2>
              <div className="editor-actions">
                <Button type="button" onClick={() => setMode('edit')}>
                  Edit
                </Button>
                <Button type="button" variant="outline" onClick={() => setPrinting(true)}>
                  🖨 Print
                </Button>
              </div>
            </div>
            <p className="muted">
              {PRESETS.find((p) => p.value === draft.preset)?.label ?? draft.preset}
              {draft.sessionId && ' · ' + (sessionOptions.find((s) => s.id === draft.sessionId)?.label ?? '')}
              {' · '}
              {draft.revealed ? '👁 Revealed to players' : '🔒 Not yet revealed'}
            </p>
            {/* Rendered in the chosen presentation style */}
            <article className={`handout-doc preview-${draft.preset.toLowerCase()}`}>
              <h2 className="handout-title">{draft.title}</h2>
              {/* eslint-disable-next-line react/no-danger */}
              <div className="preview-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(draft.body) }} />
            </article>
          </article>
        )}
      </div>

      {printing && (
        <NewWindowPortal title={`Print — ${draft.title}`} onClose={() => setPrinting(false)}>
          <div className="print-toolbar">
            <strong>Handout</strong>
            <PrintOptionsMenu opts={printOpts} onChange={setPrintOpts} />
            <span className="print-toolbar-spacer" />
            <PrintButton />
            <Button variant="link" onClick={() => setPrinting(false)}>
              Close
            </Button>
          </div>
          <div className="print-doc handout-print" {...printDocProps}>
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
