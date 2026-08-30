import { ChangeEvent, useEffect, useRef, useState } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { Markdown } from '@tiptap/markdown';
import { TableKit } from '@tiptap/extension-table';
import TaskList from '@tiptap/extension-task-list';
import TaskItem from '@tiptap/extension-task-item';
import { ResizableImage } from '../lib/resizableImageExtension';
import {
  Bold as BoldIcon,
  Italic as ItalicIcon,
  Strikethrough as StrikethroughIcon,
  Code as CodeIcon,
  List as ListIcon,
  ListOrdered as ListOrderedIcon,
  ListChecks as ListChecksIcon,
  Quote as QuoteIcon,
  SquareCode as SquareCodeIcon,
  Link as LinkIcon,
  Image as ImageIcon,
  Minus as MinusIcon,
  Table2 as TableIcon,
  Undo as UndoIcon,
  Redo as RedoIcon,
} from 'lucide-react';
import { WikiLink } from '../lib/wikiLinkExtension';
import { Button } from './ui/button';
import { Toggle } from './ui/toggle';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Separator } from './ui/separator';
import { Tooltip, TooltipContent, TooltipTrigger } from './ui/tooltip';
import { LinkPopover } from './LinkPopover';
import { AiDraftDialog } from './AiDraftDialog';
import { ArticleTemplate, DraftLevel } from '../api/client';

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

const TEXT_STYLE_OPTIONS = [
  { value: 'paragraph', label: 'Paragraph' },
  { value: 'h1', label: 'Heading 1' },
  { value: 'h2', label: 'Heading 2' },
  { value: 'h3', label: 'Heading 3' },
] as const;

/**
 * Rich-text Markdown editor (ADR-0054/ADR-0076), built on Tiptap/ProseMirror
 * with the official `@tiptap/markdown` extension for bidirectional markdown
 * round-tripping - the editor's document is always the source of truth
 * in-session, markdown text is only what's persisted/loaded.
 */
export function MarkdownEditor({
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
  // Tracks the last markdown string this component itself produced or
  // applied, so the external-sync effect below only replaces content on a
  // genuine outside change (switching articles/beats/…), not on every
  // re-render, and so onUpdate doesn't re-report a change it just caused.
  const lastValueRef = useRef(value);
  const [linkPopoverOpen, setLinkPopoverOpen] = useState(false);
  // Tiptap's `editor` isn't itself reactive - bump this on every transaction
  // so toolbar pressed-states/dropdown values stay current with the cursor.
  const [, setTick] = useState(0);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        link: { openOnClick: false, HTMLAttributes: { class: 'md-link' } },
      }),
      Markdown,
      TableKit.configure({ table: { resizable: false } }),
      TaskList,
      TaskItem.configure({ nested: true }),
      ResizableImage.configure({
        resize: { enabled: true, directions: ['bottom-right'], minWidth: 40, alwaysPreserveAspectRatio: true },
      }),
      WikiLink,
    ],
    content: value,
    contentType: 'markdown',
    onUpdate: ({ editor: e }) => {
      const markdown = e.getMarkdown();
      if (markdown !== lastValueRef.current) {
        lastValueRef.current = markdown;
        onChangeRef.current(markdown);
      }
    },
    onTransaction: () => setTick((t) => t + 1),
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (editor && value !== lastValueRef.current) {
      lastValueRef.current = value;
      editor.commands.setContent(value, { contentType: 'markdown', emitUpdate: false });
    }
  }, [value, editor]);

  async function insertImage(file: File) {
    const upload = uploadRef.current;
    if (!upload || !editor) return;
    try {
      const url = await upload(file);
      editor.chain().focus().setImage({ src: url }).run();
    } catch {
      // Surfaced by the caller's error handling; keep the editor usable.
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

  const textStyle = editor?.isActive('heading', { level: 1 })
    ? 'h1'
    : editor?.isActive('heading', { level: 2 })
      ? 'h2'
      : editor?.isActive('heading', { level: 3 })
        ? 'h3'
        : 'paragraph';

  function setTextStyle(v: string) {
    if (!editor) return;
    if (v === 'paragraph') editor.chain().focus().setParagraph().run();
    else editor.chain().focus().toggleHeading({ level: Number(v.slice(1)) as 1 | 2 | 3 }).run();
  }

  const linkHref = editor?.isActive('link') ? ((editor.getAttributes('link').href as string) ?? '') : '';
  const insideTable = Boolean(editor?.isActive('table'));

  return (
    <div className="editor md-editor">
      {/* Buttons steal focus from the editor content on click by default,
          which drops the text selection a toggle needs and swallows the next
          keystroke (it lands on the button, not the editor). Blocking focus
          on mousedown keeps the editor focused through the whole click - but
          only for genuine DOM descendants of this toolbar: portaled content
          (the link popover, select dropdown, table-editing group) isn't one,
          even though React bubbles its synthetic events here too, so
          preventDefault must not fire for those or their own inputs lose the
          browser's default click-to-focus behavior. */}
      <div
        className="editor-toolbar"
        onMouseDown={(e) => {
          if (e.currentTarget.contains(e.target as Node)) e.preventDefault();
        }}
      >
        <Toggle
          type="button"
          variant="outline"
          size="sm"
          disabled={!editor?.can().undo()}
          onPressedChange={() => editor?.chain().focus().undo().run()}
          data-testid="md-toolbar-undo"
        >
          <Tooltip>
            <TooltipTrigger asChild>
              <UndoIcon />
            </TooltipTrigger>
            <TooltipContent>Undo</TooltipContent>
          </Tooltip>
        </Toggle>
        <Toggle
          type="button"
          variant="outline"
          size="sm"
          disabled={!editor?.can().redo()}
          onPressedChange={() => editor?.chain().focus().redo().run()}
          data-testid="md-toolbar-redo"
        >
          <Tooltip>
            <TooltipTrigger asChild>
              <RedoIcon />
            </TooltipTrigger>
            <TooltipContent>Redo</TooltipContent>
          </Tooltip>
        </Toggle>

        <Separator orientation="vertical" />

        <Select value={textStyle} onValueChange={setTextStyle}>
          <SelectTrigger size="sm" data-testid="md-toolbar-text-style">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {TEXT_STYLE_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Separator orientation="vertical" />

        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('bold') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleBold().run()}
              data-testid="md-toolbar-bold"
            >
              <BoldIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Bold</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('italic') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleItalic().run()}
              data-testid="md-toolbar-italic"
            >
              <ItalicIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Italic</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('strike') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleStrike().run()}
              data-testid="md-toolbar-strike"
            >
              <StrikethroughIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Strikethrough</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('code') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleCode().run()}
              data-testid="md-toolbar-code"
            >
              <CodeIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Inline code</TooltipContent>
        </Tooltip>

        <Separator orientation="vertical" />

        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('bulletList') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleBulletList().run()}
              data-testid="md-toolbar-bullet-list"
            >
              <ListIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Bullet list</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('orderedList') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleOrderedList().run()}
              data-testid="md-toolbar-ordered-list"
            >
              <ListOrderedIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Ordered list</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('taskList') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleTaskList().run()}
              data-testid="md-toolbar-task-list"
            >
              <ListChecksIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Task list</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('blockquote') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleBlockquote().run()}
              data-testid="md-toolbar-blockquote"
            >
              <QuoteIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Blockquote</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Toggle
              type="button"
              variant="outline"
              size="sm"
              pressed={editor?.isActive('codeBlock') ?? false}
              onPressedChange={() => editor?.chain().focus().toggleCodeBlock().run()}
              data-testid="md-toolbar-code-block"
            >
              <SquareCodeIcon />
            </Toggle>
          </TooltipTrigger>
          <TooltipContent>Code block</TooltipContent>
        </Tooltip>

        <Separator orientation="vertical" />

        <LinkPopover
          trigger={
            <Toggle type="button" variant="outline" size="sm" pressed={editor?.isActive('link') ?? false}>
              <LinkIcon />
            </Toggle>
          }
          open={linkPopoverOpen}
          onOpenChange={setLinkPopoverOpen}
          href={linkHref}
          onSave={(href) => editor?.chain().focus().extendMarkRange('link').setLink({ href }).run()}
          onRemove={
            editor?.isActive('link') ? () => editor?.chain().focus().unsetLink().run() : undefined
          }
        />
        {onUploadImage && (
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
              >
                <ImageIcon />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Image</TooltipContent>
          </Tooltip>
        )}
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => editor?.chain().focus().setHorizontalRule().run()}
            >
              <MinusIcon />
            </Button>
          </TooltipTrigger>
          <TooltipContent>Horizontal rule</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() =>
                editor?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
              }
            >
              <TableIcon />
            </Button>
          </TooltipTrigger>
          <TooltipContent>Table</TooltipContent>
        </Tooltip>

        {insideTable && (
          <>
            <Separator orientation="vertical" />
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => editor?.chain().focus().addRowAfter().run()}
            >
              + Row
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => editor?.chain().focus().addColumnAfter().run()}
            >
              + Col
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => editor?.chain().focus().deleteRow().run()}
            >
              − Row
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => editor?.chain().focus().deleteColumn().run()}
            >
              − Col
            </Button>
          </>
        )}

        {onAiDraft && (
          <>
            <Separator orientation="vertical" />
            <AiDraftDialog
              onAiDraft={onAiDraft}
              articleTemplate={articleTemplate}
              onArticleTemplateChange={onArticleTemplateChange}
              getExistingContent={() => lastValueRef.current}
              onInsert={(text) =>
                editor?.chain().focus().insertContent(text, { contentType: 'markdown' }).run()
              }
            />
          </>
        )}
      </div>
      <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleFileSelected} />
      <div className="editor-content" onPaste={handlePaste} onDrop={handleDrop} data-testid="md-content">
        <EditorContent editor={editor} />
      </div>
      {onUploadImage && (
        <p className="muted hint">Tip: paste or drag an image into the text to embed it.</p>
      )}
    </div>
  );
}
