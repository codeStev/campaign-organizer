import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import { ChangeEvent, useEffect, useRef } from 'react';

interface Props {
  value: string;
  onChange: (html: string) => void;
  /** Uploads a file and resolves to its URL; enables the image button when set. */
  onUploadImage?: (file: File) => Promise<string>;
}

/** Minimal TipTap rich-text editor producing HTML (see ADR-0013). */
export function RichTextEditor({ value, onChange, onUploadImage }: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const editor = useEditor({
    extensions: [StarterKit, Image],
    content: value,
    onUpdate: ({ editor }) => onChange(editor.getHTML()),
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
      </div>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        hidden
        onChange={handleFileSelected}
      />
      <EditorContent editor={editor} className="editor-content" />
    </div>
  );
}
