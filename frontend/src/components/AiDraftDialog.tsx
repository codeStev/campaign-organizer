import { useState } from 'react';
import { Button } from './ui/button';
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

interface Props {
  /**
   * Drafts text from instructions + the editor's current content, a level,
   * and an article kind (ADR-0064/ADR-0075); the whole component renders
   * nothing when unset. Prep-time only - the caller owns the actual API
   * call, this component only requests text and hands it back via `onInsert`.
   */
  onAiDraft?: (
    instructions: string,
    existingContent: string,
    level: DraftLevel,
    template: ArticleTemplate,
  ) => Promise<string>;
  /**
   * Current article kind + setter, shared with the caller's own kind picker
   * (e.g. the article template Select) so this dialog's kind selector and
   * the article's actual kind field are always the same state. Only offered
   * alongside `onAiDraft`.
   */
  articleTemplate?: ArticleTemplate;
  onArticleTemplateChange?: (template: ArticleTemplate) => void;
  /** Reads the editor's current markdown at submit time. */
  getExistingContent: () => string;
  /** Inserts the drafted text into the editor at the cursor. */
  onInsert: (text: string) => void;
}

/** Toolbar trigger + dialog for AI-assisted drafting, extracted out of
 * MarkdownEditor since it's fully self-contained and the toolbar was
 * growing well beyond a single component's worth of formatting logic. */
export function AiDraftDialog({
  onAiDraft,
  articleTemplate,
  onArticleTemplateChange,
  getExistingContent,
  onInsert,
}: Props) {
  const [drafting, setDrafting] = useState(false);
  const [open, setOpen] = useState(false);
  const [instructionsInput, setInstructionsInput] = useState('');
  const [level, setLevel] = useState<DraftLevel>('FULL_DRAFT');
  const [draftError, setDraftError] = useState<string | null>(null);

  if (!onAiDraft) return null;

  const showKindPicker = Boolean(articleTemplate && onArticleTemplateChange);
  const selectedLevelOption = LEVEL_OPTIONS.find((opt) => opt.value === level);

  function openDialog() {
    if (drafting) return;
    setDraftError(null);
    setOpen(true);
  }

  async function submit() {
    if (!onAiDraft || !instructionsInput.trim() || drafting) return;
    setDrafting(true);
    setDraftError(null);
    try {
      const text = await onAiDraft(
        instructionsInput.trim(),
        getExistingContent(),
        level,
        articleTemplate ?? 'GENERIC',
      );
      onInsert(text);
      setOpen(false);
      setInstructionsInput('');
    } catch (err) {
      setDraftError(err instanceof Error ? err.message : 'AI draft failed');
    } finally {
      setDrafting(false);
    }
  }

  return (
    <>
      <Button type="button" variant="outline" size="sm" disabled={drafting} onClick={openDialog}>
        {drafting ? '✨ Drafting…' : '✨ AI draft'}
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
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
            <Button type="button" disabled={!instructionsInput.trim() || drafting} onClick={() => void submit()}>
              {drafting ? 'Drafting…' : 'Draft'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
