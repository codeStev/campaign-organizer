import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ApiError,
  handoutsApi,
  handoutCategoriesApi,
  mediaApi,
  campaignsApi,
  sessionsApi,
  Handout,
  HandoutCategory,
  HandoutPreset,
} from '../api/client';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';
import { renderMarkdown } from '../lib/markdown';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { TruncatedLabel } from '../components/TruncatedLabel';
import { CategoryTree } from '../components/CategoryTree';
import { Toggle } from '../components/ui/toggle';
import { toast } from 'sonner';
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
  categoryId: null as string | null,
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
  const categoriesApi = useMemo(() => handoutCategoriesApi(worldId), [worldId]);
  const media = useMemo(() => mediaApi(worldId), [worldId]);
  const [list, setList] = useState<Handout[]>([]);
  const [categories, setCategories] = useState<HandoutCategory[]>([]);
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
      const [all, cats] = await Promise.all([api.list(), categoriesApi.list()]);
      setList(all);
      setCategories(cats);
      return all;
    } catch (err) {
      handleError(err);
      return [];
    } finally {
      setLoading(false);
    }
  }, [api, categoriesApi, handleError]);

  async function createHandoutCategory(name: string, parentId: string | null) {
    try {
      await categoriesApi.create({ name, parentId });
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  async function removeHandoutCategory(category: HandoutCategory) {
    try {
      await categoriesApi.remove(category.id);
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  async function renameHandoutCategory(category: HandoutCategory, newName: string) {
    try {
      await categoriesApi.update(category.id, { name: newName, parentId: category.parentId ?? null });
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  async function renameHandout(h: Handout, newTitle: string) {
    try {
      const updated = await api.update(h.id, {
        categoryId: h.categoryId ?? null,
        title: newTitle,
        preset: h.preset,
        body: h.body ?? null,
        sessionId: h.sessionId ?? null,
        revealed: h.revealed,
      });
      if (draft.id === updated.id) setDraft((d) => ({ ...d, title: updated.title }));
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

  // Handout's list response already carries every field an update needs
  // (unlike Article, there's no separate "summary vs full" split), so this
  // can update directly without a full-fetch-first step (mirrors Atlas).
  async function moveHandoutToCategory(h: Handout, categoryId: string | null) {
    if ((h.categoryId ?? null) === categoryId) return;
    try {
      const updated = await api.update(h.id, {
        categoryId,
        title: h.title,
        preset: h.preset,
        body: h.body ?? null,
        sessionId: h.sessionId ?? null,
        revealed: h.revealed,
      });
      if (draft.id === updated.id) setDraft((d) => ({ ...d, categoryId: updated.categoryId ?? null }));
      await refresh();
    } catch (err) {
      handleError(err);
    }
  }

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
      categoryId: h.categoryId ?? null,
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
      categoryId: draft.categoryId,
      title: draft.title,
      preset: draft.preset,
      body: draft.body || null,
      sessionId: draft.sessionId,
      revealed: draft.revealed,
    };
    try {
      const saved = draft.id ? await api.update(draft.id, body) : await api.create(body);
      loadDraft(saved);
      if (!draft.id) navigate(saved.id);
      await refresh();
      toast.success(`Handout "${draft.title}" saved`);
    } catch (err) {
      handleError(err);
    }
  }

  // Reorder buttons act within one category grouping at a time (the tree
  // visually groups handouts by category), not the flat world-wide list —
  // otherwise "move up" on a handout that's merely first *within its
  // category* could swap it into a different category's item entirely.
  // The two siblings still swap positions in the full `list` sent to
  // api.reorder (there's only one world-wide sortOrder), which moves them
  // relative to each other without disturbing anyone else's order.
  async function move(handout: Handout, delta: number) {
    const categoryId = handout.categoryId ?? null;
    const siblings = list.filter((h) => (h.categoryId ?? null) === categoryId);
    const index = siblings.findIndex((h) => h.id === handout.id);
    const target = index + delta;
    if (index < 0 || target < 0 || target >= siblings.length) return;
    const a = siblings[index];
    const b = siblings[target];
    const next = list.map((h) => (h.id === a.id ? b : h.id === b.id ? a : h));
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
        categoryId: h.categoryId ?? null,
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
      navigate(`../${copy.id}`, { relative: 'path' });
      toast.success(`Handout "${copy.title}" created`);
    } catch (err) {
      handleError(err);
    }
  }

  async function remove() {
    if (!draft.id) return;
    try {
      await api.remove(draft.id);
      navigate('..', { relative: 'path' });
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
            navigate(urlHandoutId ? '..' : '.', { relative: 'path' });
          }}
          data-testid="new-handout-button"
        >
          + New handout
        </Button>
        <CategoryTree
          categories={categories}
          entities={list}
          entityId={(h) => h.id}
          entityLabel={(h) => h.title}
          entityCategoryId={(h) => h.categoryId ?? null}
          activeEntityId={urlHandoutId ?? null}
          onOpenEntity={(id) => navigate(urlHandoutId ? `../${id}` : id, { relative: 'path' })}
          onMoveEntity={(h, categoryId) => void moveHandoutToCategory(h, categoryId)}
          onCreateCategory={(name, parentId) => void createHandoutCategory(name, parentId)}
          onRemoveCategory={(c) => void removeHandoutCategory(c)}
          onRenameCategory={(c, name) => void renameHandoutCategory(c, name)}
          onRenameEntity={(h, name) => void renameHandout(h, name)}
          loading={loading}
          searchPlaceholder="Search handouts…"
          emptyLabel="No handouts yet."
          renderEntityRow={(h) => {
            const siblings = list.filter((x) => (x.categoryId ?? null) === (h.categoryId ?? null));
            const i = siblings.findIndex((x) => x.id === h.id);
            return (
              <div className="handout-tree-row-content">
                <TruncatedLabel label={h.title} className="handout-tree-row-label">
                  {h.title}
                </TruncatedLabel>
                <span className="handout-tree-row-actions" onClick={(e) => e.stopPropagation()}>
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    onClick={() => void move(h, -1)}
                    disabled={i <= 0}
                    title="Move up"
                  >
                    ↑
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    onClick={() => void move(h, 1)}
                    disabled={i < 0 || i === siblings.length - 1}
                    title="Move down"
                  >
                    ↓
                  </Button>
                  <Toggle
                    type="button"
                    size="sm"
                    pressed={h.revealed}
                    onPressedChange={() => void toggleRevealed(h)}
                    title={
                      h.revealed
                        ? 'Revealed to players — click to mark secret'
                        : 'Not yet revealed — click to mark revealed'
                    }
                    aria-label="Revealed to players"
                  >
                    {h.revealed ? '👁' : '🔒'}
                  </Toggle>
                </span>
              </div>
            );
          }}
        />
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
