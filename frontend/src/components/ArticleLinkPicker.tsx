import { useEffect, useState } from 'react';
import { articlesApi, ArticleSummary, ApiError } from '../api/client';
import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from './ui/command';

interface Props {
  worldId: string;
  open: boolean;
  /** Inserted as [[Title]] at the cursor of the field that opened the picker. */
  onPick: (title: string) => void;
  onClose: () => void;
}

/**
 * Searchable world-article list that inserts a `[[Title]]` wiki-link.
 * Used by the table-entry and deck-card bodies (FR-40).
 */
export function ArticleLinkPicker({ worldId, open, onPick, onClose }: Props) {
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Load lazily so opening the tab doesn't pay for an article fetch nobody uses.
  useEffect(() => {
    if (!open || articles.length > 0) return;
    articlesApi(worldId)
      .list()
      .then(setArticles)
      .catch((err: unknown) => {
        if (!(err instanceof ApiError && err.status === 401)) setError(err instanceof Error ? err.message : 'Failed to load articles');
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  return (
    <CommandDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) onClose();
      }}
      title="Link an article"
      description="Inserts a [[wiki-link]] into the outcome text"
    >
      <Command>
        <CommandInput placeholder="Find an article to link…" />
        <CommandList>
          <CommandEmpty>{error ?? 'No matches.'}</CommandEmpty>
          <CommandGroup heading="Articles">
            {articles.map((a) => (
              <CommandItem
                key={a.id}
                value={a.title}
                onSelect={() => {
                  onPick(a.title);
                  onClose();
                }}
              >
                {a.title}
              </CommandItem>
            ))}
          </CommandGroup>
        </CommandList>
      </Command>
    </CommandDialog>
  );
}
