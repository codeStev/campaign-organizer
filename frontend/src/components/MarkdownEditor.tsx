import { ChangeEvent, useEffect, useRef, useState } from 'react';
import { Editor, rootCtx, defaultValueCtx, commandsCtx, editorStateCtx } from '@milkdown/kit/core';
import type { Ctx } from '@milkdown/kit/ctx';
import {
  commonmark,
  toggleStrongCommand,
  toggleEmphasisCommand,
  wrapInHeadingCommand,
  wrapInBulletListCommand,
  liftListItemCommand,
  isNodeSelectedCommand,
  strongSchema,
  emphasisSchema,
  headingSchema,
  bulletListSchema,
} from '@milkdown/kit/preset/commonmark';
import { gfm } from '@milkdown/kit/preset/gfm';
import { history } from '@milkdown/kit/plugin/history';
import { clipboard } from '@milkdown/kit/plugin/clipboard';
import { listener, listenerCtx } from '@milkdown/kit/plugin/listener';
import { callCommand, insert, replaceAll } from '@milkdown/kit/utils';
import { Milkdown, MilkdownProvider, useEditor } from '@milkdown/react';
import '@milkdown/kit/prose/view/style/prosemirror.css';
import { Button } from './ui/button';
import { Toggle } from './ui/toggle';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { RadioGroup, RadioGroupItem } from './ui/radio-group';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import { Alert, AlertTitle, AlertDescription } from './ui/alert';
import { Tooltip, TooltipContent, TooltipTrigger } from './ui/tooltip';
import { ArticleTemplate, ARTICLE_TEMPLATES, DraftLevel } from '../api/client';

interface Props {
  value: string;
  onChange: (markdown: string) => void;
  /** Uploads a file and resolves to its URL; enables image embedding when set. */
  onUploadImage?: (file: File) => Promise<string>;
  /**
   * Drafts text from instructions + the editor's current content, a level,
   * and an article kind (ADR-0064/ADR-0075); enables the "AI draft" toolbar
   * action when set. Prep-time only - the caller owns the actual API call,
   * this component only inserts the result.
   */
  onAiDraft?: (
    instructions: string,
    existingContent: string,
    level: DraftLevel,
    template: ArticleTemplate,
  ) => Promise<string>;
  /**
   * Current article kind + setter, shared with the caller's own kind picker
   * (e.g. the article template Select) so the AI-draft dialog's kind
   * selector and the article's actual kind field are always the same state.
   * Only offered alongside `onAiDraft`.
   */
  articleTemplate?: ArticleTemplate;
  onArticleTemplateChange?: (template: ArticleTemplate) => void;
}

function templateLabel(t: ArticleTemplate) {
  return t.charAt(0) + t.slice(1).toLowerCase();
}

const LEVEL_OPTIONS: {
  value: DraftLevel;
  label: string;
  description: string;
  /** Shown as the instructions field's placeholder and hint when selected. */
  example: string;
  hint: string;
}[] = [
  {
    value: 'QUICK_INSPIRATION',
    label: 'Quick inspiration',
    description: 'A short spark to get the idea going — a few evocative sentences, nothing more.',
    example: 'a gruff dockmaster who hides a smuggling habit',
    hint: "Tip: a mood or image is enough — you don't need the details yet.",
  },
  {
    value: 'READ_ALOUD',
    label: 'Read-aloud snippet',
    description: 'A short passage to read straight to your players at the table.',
    example: 'the party enters the foggy, abandoned lighthouse for the first time',
    hint: 'Tip: phrase it as a moment ("the party arrives at…"), not just a subject.',
  },
  {
    value: 'BASIC_INFO',
    label: 'Basic info',
    description:
      "Just the essential facts you've already given — won't invent new places, characters, or other proper nouns.",
    example: 'he works the north pier, has a scar on his jaw, distrusts elves',
    hint: "Tip: give it facts you've already decided — it won't invent new names.",
  },
  {
    value: 'FULL_DRAFT',
    label: 'Full draft',
    description: 'A complete first-draft article, ready to edit.',
    example: 'a coastal trade city ruled by a council of merchant princes',
    hint: 'Tip: a short premise is enough — the model fills in supporting details.',
  },
];

/**
 * Live-preview Markdown editor (ADR-0054), built directly on Milkdown's
 * ProseMirror/remark core + commonmark/gfm presets — deliberately not
 * `@milkdown/crepe` (pulls in Vue/KaTeX/CodeMirror we don't need).
 */
function MarkdownEditorInner({
  value,
  onChange,
  onUploadImage,
  onAiDraft,
  articleTemplate,
  onArticleTemplateChange,
}: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const uploadRef = useRef(onUploadImage);
  uploadRef.current = onUploadImage;
  const aiDraftRef = useRef(onAiDraft);
  aiDraftRef.current = onAiDraft;
  const [drafting, setDrafting] = useState(false);
  const [draftDialogOpen, setDraftDialogOpen] = useState(false);
  const [instructionsInput, setInstructionsInput] = useState('');
  const [level, setLevel] = useState<DraftLevel>('FULL_DRAFT');
  const [draftError, setDraftError] = useState<string | null>(null);
  // Which toolbar formatting is active at the current cursor/selection, so
  // the toolbar buttons can show a pressed state (else there's no way to
  // tell whether e.g. bold is already on without selecting the text).
  const [active, setActive] = useState({ bold: false, italic: false, heading: false, bulletList: false });
  // Tracks the last markdown string this component itself produced or applied,
  // so the external-sync effect below only replaces content on a genuine
  // outside change (switching articles/beats/…), not on every re-render.
  const lastValueRef = useRef(value);

  function readActiveMarks(ctx: Ctx) {
    const state = ctx.get(editorStateCtx);
    const { selection } = state;
    // For a collapsed cursor, doc.rangeHasMark sees an empty range and always
    // says "no" — the mark that toggling just set only exists in
    // storedMarks (what the *next typed character* will get), not yet in the
    // document. Mirror ProseMirror's own markActive idiom instead.
    const markActive = (mark: ReturnType<typeof strongSchema.type>) =>
      selection.empty
        ? !!mark.isInSet(state.storedMarks ?? selection.$from.marks())
        : state.doc.rangeHasMark(selection.from, selection.to, mark);

    const commands = ctx.get(commandsCtx);
    setActive({
      bold: markActive(strongSchema.type(ctx)),
      italic: markActive(emphasisSchema.type(ctx)),
      heading: commands.call(isNodeSelectedCommand.key, headingSchema.type(ctx)),
      bulletList: commands.call(isNodeSelectedCommand.key, bulletListSchema.type(ctx)),
    });
  }

  const { get } = useEditor(
    (root) =>
      Editor.make()
        .config((ctx) => {
          ctx.set(rootCtx, root);
          ctx.set(defaultValueCtx, value);
          ctx.get(listenerCtx).markdownUpdated((_ctx, markdown, prevMarkdown) => {
            if (markdown !== prevMarkdown) {
              lastValueRef.current = markdown;
              onChangeRef.current(markdown);
            }
          });
          ctx.get(listenerCtx).selectionUpdated((selCtx) => readActiveMarks(selCtx));
        })
        .use(commonmark)
        .use(gfm)
        .use(history)
        .use(clipboard)
        .use(listener),
    [],
  );

  useEffect(() => {
    const editor = get();
    if (editor && value !== lastValueRef.current) {
      lastValueRef.current = value;
      editor.action(replaceAll(value));
    }
  }, [value, get]);

  // Toggling a mark on an existing selection doesn't move the selection, so
  // Milkdown's selectionUpdated listener won't fire on its own; re-read the
  // active state explicitly so the button highlights immediately.
  function toggleBold() {
    get()?.action((ctx) => {
      callCommand(toggleStrongCommand.key)(ctx);
      readActiveMarks(ctx);
    });
  }

  function toggleItalic() {
    get()?.action((ctx) => {
      callCommand(toggleEmphasisCommand.key)(ctx);
      readActiveMarks(ctx);
    });
  }

  function toggleHeading() {
    get()?.action((ctx) => {
      callCommand(wrapInHeadingCommand.key, 2)(ctx);
      readActiveMarks(ctx);
    });
  }

  // wrapInBulletListCommand only ever wraps — there's no matching unwrap
  // behind the same button, unlike Bold/Italic which are real toggles.
  // Lift the current item back out when it's already a list.
  function toggleBulletList() {
    get()?.action((ctx) => {
      const commands = ctx.get(commandsCtx);
      const alreadyList = commands.call(isNodeSelectedCommand.key, bulletListSchema.type(ctx));
      commands.call(alreadyList ? liftListItemCommand.key : wrapInBulletListCommand.key);
      readActiveMarks(ctx);
    });
  }

  async function insertImage(file: File) {
    const upload = uploadRef.current;
    if (!upload) return;
    try {
      const url = await upload(file);
      get()?.action(insert(`![](${url})`));
    } catch {
      // Surfaced by the caller's error handling; keep the editor usable.
    }
  }

  function openAiDraftDialog() {
    if (!aiDraftRef.current || drafting) return;
    setDraftError(null);
    setDraftDialogOpen(true);
  }

  async function submitAiDraft() {
    const draft = aiDraftRef.current;
    if (!draft || !instructionsInput.trim() || drafting) return;
    setDrafting(true);
    setDraftError(null);
    try {
      const text = await draft(
        instructionsInput.trim(),
        lastValueRef.current,
        level,
        articleTemplate ?? 'GENERIC',
      );
      get()?.action(insert(text));
      setDraftDialogOpen(false);
      setInstructionsInput('');
    } catch (err) {
      setDraftError(err instanceof Error ? err.message : 'AI draft failed');
    } finally {
      setDrafting(false);
    }
  }

  async function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (file) await insertImage(file);
  }

  function handlePaste(event: React.ClipboardEvent) {
    const files = Array.from(event.clipboardData?.files ?? []).filter((f) =>
      f.type.startsWith('image/'),
    );
    if (files.length === 0 || !uploadRef.current) return;
    event.preventDefault();
    files.forEach((f) => void insertImage(f));
  }

  function handleDrop(event: React.DragEvent) {
    const files = Array.from(event.dataTransfer?.files ?? []).filter((f) =>
      f.type.startsWith('image/'),
    );
    if (files.length === 0 || !uploadRef.current) return;
    event.preventDefault();
    files.forEach((f) => void insertImage(f));
  }

  const showKindPicker = Boolean(onAiDraft && articleTemplate && onArticleTemplateChange);
  const selectedLevelOption = LEVEL_OPTIONS.find((opt) => opt.value === level);

  return (
    <div className="editor md-editor">
      {/* Buttons steal focus from the ProseMirror content on click by default,
          which drops the text selection a toggle needs and swallows the next
          keystroke (it lands on the button, not the editor). Blocking focus
          on mousedown keeps the editor focused through the whole click. */}
      <div className="editor-toolbar" onMouseDown={(e) => e.preventDefault()}>
        <Toggle
          type="button"
          variant="outline"
          size="sm"
          pressed={active.bold}
          onPressedChange={toggleBold}
          data-testid="md-toolbar-bold"
        >
          B
        </Toggle>
        <Toggle
          type="button"
          variant="outline"
          size="sm"
          pressed={active.italic}
          onPressedChange={toggleItalic}
          data-testid="md-toolbar-italic"
        >
          i
        </Toggle>
        <Toggle
          type="button"
          variant="outline"
          size="sm"
          pressed={active.heading}
          onPressedChange={toggleHeading}
          data-testid="md-toolbar-h2"
        >
          H2
        </Toggle>
        <Toggle
          type="button"
          variant="outline"
          size="sm"
          pressed={active.bulletList}
          onPressedChange={toggleBulletList}
          data-testid="md-toolbar-bullet-list"
        >
          • List
        </Toggle>
        {onUploadImage && (
          <Button type="button" variant="outline" size="sm" onClick={() => fileInputRef.current?.click()}>
            🖼 Image
          </Button>
        )}
        {onAiDraft && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={drafting}
            onClick={openAiDraftDialog}
          >
            {drafting ? '✨ Drafting…' : '✨ AI draft'}
          </Button>
        )}
      </div>
      {/* Deliberately NOT nested inside .editor-toolbar above: React bubbles
          a portaled Dialog's synthetic events up through the React tree (not
          the DOM tree) to that div's onMouseDown handler regardless of where
          the DOM node actually lives, and preventDefault() there would kill
          the browser's default focus-on-click behavior for every field in
          this dialog - clicks stop focusing inputs, though Tab still works. */}
      {onAiDraft && (
        <Dialog open={draftDialogOpen} onOpenChange={setDraftDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>AI draft</DialogTitle>
            </DialogHeader>

            {showKindPicker && (
              <div className="ai-draft-field">
                <Label>Article kind</Label>
                <RadioGroup
                  className="ai-draft-kind-grid"
                  value={articleTemplate}
                  onValueChange={(v) => onArticleTemplateChange?.(v as ArticleTemplate)}
                >
                  {ARTICLE_TEMPLATES.map((t) => (
                    <div key={t} className="ai-draft-level-option">
                      <RadioGroupItem value={t} id={`ai-draft-kind-${t}`} />
                      <Label htmlFor={`ai-draft-kind-${t}`}>{templateLabel(t)}</Label>
                    </div>
                  ))}
                </RadioGroup>
              </div>
            )}

            <div className="ai-draft-field">
              <Label htmlFor="ai-draft-instructions">What should I draft?</Label>
              <Textarea
                id="ai-draft-instructions"
                placeholder={selectedLevelOption ? `e.g. ${selectedLevelOption.example}` : 'Keywords/instructions…'}
                value={instructionsInput}
                onChange={(e) => setInstructionsInput(e.target.value)}
                autoFocus
              />
              {selectedLevelOption && <p className="muted hint">{selectedLevelOption.hint}</p>}
            </div>

            <div className="ai-draft-field">
              <Label>Level</Label>
              <RadioGroup value={level} onValueChange={(v) => setLevel(v as DraftLevel)}>
                {LEVEL_OPTIONS.map((opt) => (
                  <div key={opt.value} className="ai-draft-level-option">
                    <RadioGroupItem value={opt.value} id={`ai-draft-level-${opt.value}`} />
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Label htmlFor={`ai-draft-level-${opt.value}`}>{opt.label}</Label>
                      </TooltipTrigger>
                      <TooltipContent>{opt.description}</TooltipContent>
                    </Tooltip>
                  </div>
                ))}
              </RadioGroup>
            </div>

            {draftError && (
              <Alert variant="destructive">
                <AlertTitle>AI draft failed</AlertTitle>
                <AlertDescription>{draftError}</AlertDescription>
              </Alert>
            )}

            <DialogFooter>
              <DialogClose asChild>
                <Button type="button" variant="link">
                  Cancel
                </Button>
              </DialogClose>
              <Button
                type="button"
                disabled={!instructionsInput.trim() || drafting}
                onClick={() => void submitAiDraft()}
              >
                {drafting ? 'Drafting…' : 'Draft'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
      <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleFileSelected} />
      <div
        className="editor-content"
        onPaste={handlePaste}
        onDrop={handleDrop}
        data-testid="md-content"
      >
        <Milkdown />
      </div>
      {onUploadImage && (
        <p className="muted hint">Tip: paste or drag an image into the text to embed it.</p>
      )}
    </div>
  );
}

export function MarkdownEditor(props: Props) {
  return (
    <MilkdownProvider>
      <MarkdownEditorInner {...props} />
    </MilkdownProvider>
  );
}
