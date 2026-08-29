import { ChangeEvent, useEffect, useRef, useState } from 'react';
import { Editor, rootCtx, defaultValueCtx, commandsCtx } from '@milkdown/kit/core';
import type { Ctx } from '@milkdown/kit/ctx';
import {
  commonmark,
  toggleStrongCommand,
  toggleEmphasisCommand,
  wrapInHeadingCommand,
  wrapInBulletListCommand,
  isMarkSelectedCommand,
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

interface Props {
  value: string;
  onChange: (markdown: string) => void;
  /** Uploads a file and resolves to its URL; enables image embedding when set. */
  onUploadImage?: (file: File) => Promise<string>;
  /**
   * Drafts text from instructions + the editor's current content (ADR-0064);
   * enables the "AI draft" toolbar action when set. Prep-time only - the
   * caller owns the actual API call, this component only inserts the result.
   */
  onAiDraft?: (instructions: string, existingContent: string) => Promise<string>;
}

/**
 * Live-preview Markdown editor (ADR-0054), built directly on Milkdown's
 * ProseMirror/remark core + commonmark/gfm presets — deliberately not
 * `@milkdown/crepe` (pulls in Vue/KaTeX/CodeMirror we don't need).
 */
function MarkdownEditorInner({ value, onChange, onUploadImage, onAiDraft }: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const uploadRef = useRef(onUploadImage);
  uploadRef.current = onUploadImage;
  const aiDraftRef = useRef(onAiDraft);
  aiDraftRef.current = onAiDraft;
  const [drafting, setDrafting] = useState(false);
  // Which toolbar formatting is active at the current cursor/selection, so
  // the toolbar buttons can show a pressed state (else there's no way to
  // tell whether e.g. bold is already on without selecting the text).
  const [active, setActive] = useState({ bold: false, italic: false, heading: false, bulletList: false });
  // Tracks the last markdown string this component itself produced or applied,
  // so the external-sync effect below only replaces content on a genuine
  // outside change (switching articles/beats/…), not on every re-render.
  const lastValueRef = useRef(value);

  function readActiveMarks(ctx: Ctx) {
    const commands = ctx.get(commandsCtx);
    setActive({
      bold: commands.call(isMarkSelectedCommand.key, strongSchema.type(ctx)),
      italic: commands.call(isMarkSelectedCommand.key, emphasisSchema.type(ctx)),
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

  function toggleBulletList() {
    get()?.action((ctx) => {
      callCommand(wrapInBulletListCommand.key)(ctx);
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

  async function requestAiDraft() {
    const draft = aiDraftRef.current;
    if (!draft || drafting) return;
    const instructions = window.prompt('What should I draft? (keywords/instructions)');
    if (!instructions) return;
    setDrafting(true);
    try {
      const text = await draft(instructions, lastValueRef.current);
      get()?.action(insert(text));
    } catch (err) {
      window.alert(err instanceof Error ? err.message : 'AI draft failed');
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

  return (
    <div className="editor md-editor">
      <div className="editor-toolbar">
        <Button
          type="button"
          variant={active.bold ? 'default' : 'outline'}
          size="sm"
          aria-pressed={active.bold}
          onClick={toggleBold}
        >
          B
        </Button>
        <Button
          type="button"
          variant={active.italic ? 'default' : 'outline'}
          size="sm"
          aria-pressed={active.italic}
          onClick={toggleItalic}
        >
          i
        </Button>
        <Button
          type="button"
          variant={active.heading ? 'default' : 'outline'}
          size="sm"
          aria-pressed={active.heading}
          onClick={toggleHeading}
        >
          H2
        </Button>
        <Button
          type="button"
          variant={active.bulletList ? 'default' : 'outline'}
          size="sm"
          aria-pressed={active.bulletList}
          onClick={toggleBulletList}
        >
          • List
        </Button>
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
            onClick={() => void requestAiDraft()}
          >
            {drafting ? '✨ Drafting…' : '✨ AI draft'}
          </Button>
        )}
      </div>
      <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleFileSelected} />
      <div className="editor-content" onPaste={handlePaste} onDrop={handleDrop}>
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
