import { useEditor, EditorContent } from '@tiptap/react';
import { EditorView } from '@tiptap/pm/view';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import { ChangeEvent, useEffect, useRef } from 'react';

interface Props {
  value: string;
  onChange: (html: string) => void;
  /** Uploads a file and resolves to its URL; enables image embedding when set. */
  onUploadImage?: (file: File) => Promise<string>;
}

/** Image node that also persists an editor-chosen display width (px or %). */
const SizedImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      width: {
        default: null,
        parseHTML: (el) => el.getAttribute('width'),
        renderHTML: (attrs) => (attrs.width ? { width: attrs.width } : {}),
      },
    };
  },
});

// Pixel widths (the sanitizer accepts integer widths); CSS still caps to 100%.
const WIDTHS: { label: string; value: string | null }[] = [
  { label: 'S', value: '320' },
  { label: 'M', value: '480' },
  { label: 'L', value: '720' },
  { label: 'Full', value: null },
];

/** Minimal TipTap rich-text editor producing HTML (see ADR-0013, ADR-0039). */
export function RichTextEditor({ value, onChange, onUploadImage }: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  // Keep the latest uploader without rebuilding the editor.
  const uploadRef = useRef(onUploadImage);
  uploadRef.current = onUploadImage;

  // Upload image files and insert them at the current selection; returns true if it handled any.
  function insertFiles(view: EditorView, files?: FileList | null): boolean {
    const upload = uploadRef.current;
    const images = files ? Array.from(files).filter((f) => f.type.startsWith('image/')) : [];
    if (!upload || images.length === 0) return false;
    images.forEach(async (file) => {
      try {
        const url = await upload(file);
        const node = view.state.schema.nodes.image.create({ src: url });
        view.dispatch(view.state.tr.replaceSelectionWith(node).scrollIntoView());
      } catch {
        // Surfaced by the caller's error handling; keep the editor usable.
      }
    });
    return true;
  }

  const editor = useEditor({
    extensions: [StarterKit, SizedImage],
    content: value,
    onUpdate: ({ editor }) => onChange(editor.getHTML()),
    editorProps: {
      handlePaste: (view, event) => insertFiles(view, event.clipboardData?.files),
      handleDrop: (view, event) => {
        const files = (event as DragEvent).dataTransfer?.files;
        if (files && files.length && insertFiles(view, files)) {
          event.preventDefault();
          return true;
        }
        return false;
      },
    },
  });

  async function handleFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !onUploadImage || !editor) return;
    try {
      const url = await onUploadImage(file);
      editor.chain().focus().setImage({ src: url }).run();
    } catch {
      // Surfaced by the caller's error handling; keep the editor usable.
    }
  }

  // Keep the editor in sync when the selected article changes externally.
  useEffect(() => {
    if (editor && value !== editor.getHTML()) {
      editor.commands.setContent(value, false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, editor]);

  if (!editor) {
    return null;
  }

  const imageSelected = editor.isActive('image');

  return (
    <div className="editor">
      <div className="editor-toolbar">
        <button
          type="button"
          className={editor.isActive('bold') ? 'active' : ''}
          onClick={() => editor.chain().focus().toggleBold().run()}
        >
          B
        </button>
        <button
          type="button"
          className={editor.isActive('italic') ? 'active' : ''}
          onClick={() => editor.chain().focus().toggleItalic().run()}
        >
          i
        </button>
        <button
          type="button"
          className={editor.isActive('heading', { level: 2 }) ? 'active' : ''}
          onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        >
          H2
        </button>
        <button
          type="button"
          className={editor.isActive('bulletList') ? 'active' : ''}
          onClick={() => editor.chain().focus().toggleBulletList().run()}
        >
          • List
        </button>
        {onUploadImage && (
          <button type="button" onClick={() => fileInputRef.current?.click()}>
            🖼 Image
          </button>
        )}
        {imageSelected &&
          WIDTHS.map((w) => (
            <button
              key={w.label}
              type="button"
              title={`Resize image to ${w.label}`}
              onClick={() => editor.chain().focus().updateAttributes('image', { width: w.value }).run()}
            >
              {w.label}
            </button>
          ))}
      </div>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        hidden
        onChange={handleFileSelected}
      />
      <EditorContent editor={editor} className="editor-content" />
      {onUploadImage && (
        <p className="muted hint">Tip: paste or drag an image into the text to embed it.</p>
      )}
    </div>
  );
}
