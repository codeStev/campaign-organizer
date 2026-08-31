import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from './ui/button';
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from './ui/command';
import { worldTagsApi } from '../api/client';

function normalizeTag(raw: string): string {
  return raw.trim().toLowerCase();
}

interface TagInputProps {
  worldId: string;
  value: string[];
  onChange: (tags: string[]) => void;
}

/** Chip-style tag editor with autocomplete against the world's existing tags (ADR-0083). */
export function TagInput({ worldId, value, onChange }: TagInputProps) {
  const [query, setQuery] = useState('');
  const [worldTags, setWorldTags] = useState<string[]>([]);

  useEffect(() => {
    let active = true;
    worldTagsApi(worldId)
      .list()
      .then((tags) => {
        if (active) setWorldTags(tags);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [worldId]);

  function addTag(raw: string) {
    const tag = normalizeTag(raw);
    if (!tag || value.includes(tag)) return;
    onChange([...value, tag]);
    setQuery('');
  }

  function removeTag(tag: string) {
    onChange(value.filter((t) => t !== tag));
  }

  const normalizedQuery = normalizeTag(query);
  const suggestions = worldTags.filter((t) => !value.includes(t) && t.includes(normalizedQuery));
  const exactMatch = worldTags.some((t) => t === normalizedQuery);

  return (
    <div className="tag-input">
      {value.length > 0 && (
        <div className="beat-article-chips">
          {value.map((tag) => (
            <span key={tag} className="beat-chip">
              {tag}
              <Button
                type="button"
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => removeTag(tag)}
                aria-label={`Remove tag ${tag}`}
              >
                ✕
              </Button>
            </span>
          ))}
        </div>
      )}
      <Command className="tag-command" shouldFilter={false}>
        <CommandInput
          placeholder="Add a tag…"
          value={query}
          onValueChange={setQuery}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ',') {
              e.preventDefault();
              if (query.trim()) addTag(query);
            }
          }}
        />
        {normalizedQuery.length > 0 && (
          <CommandList>
            <CommandGroup>
              {suggestions.map((tag) => (
                <CommandItem key={tag} onSelect={() => addTag(tag)}>
                  {tag}
                </CommandItem>
              ))}
              {!exactMatch && (
                <CommandItem onSelect={() => addTag(query)}>Create tag: "{normalizedQuery}"</CommandItem>
              )}
            </CommandGroup>
            {suggestions.length === 0 && exactMatch && <CommandEmpty>Already added.</CommandEmpty>}
          </CommandList>
        )}
      </Command>
    </div>
  );
}

interface TagListProps {
  worldId: string;
  tags: string[];
}

/** Read-only tag chips; clicking one navigates to the cross-entity browse view (ADR-0083). */
export function TagList({ worldId, tags }: TagListProps) {
  const navigate = useNavigate();
  if (tags.length === 0) return null;
  return (
    <div className="beat-article-chips">
      {tags.map((tag) => (
        <button
          key={tag}
          type="button"
          className="beat-chip tag-chip-link"
          onClick={() => navigate(`/worlds/${worldId}/tags/${encodeURIComponent(tag)}`)}
        >
          {tag}
        </button>
      ))}
    </div>
  );
}
