import { useState } from 'react';
import { Button } from './ui/button';
import { Command, CommandInput } from './ui/command';

interface Props {
  value: string[];
  onChange: (aliases: string[]) => void;
}

/**
 * Chip-style alias editor for an article (ADR-0116) - deliberately simpler
 * than TagInput's: no world-wide autocomplete, since aliases aren't a
 * shared vocabulary the way tags are, just alternate names for this one
 * article. Casing is kept as typed (a proper name, unlike a folksonomy tag).
 */
export function AliasInput({ value, onChange }: Props) {
  const [query, setQuery] = useState('');

  function addAlias(raw: string) {
    const alias = raw.trim();
    if (!alias || value.some((a) => a.toLowerCase() === alias.toLowerCase())) return;
    onChange([...value, alias]);
    setQuery('');
  }

  function removeAlias(alias: string) {
    onChange(value.filter((a) => a !== alias));
  }

  return (
    <div className="tag-input">
      {value.length > 0 && (
        <div className="beat-article-chips">
          {value.map((alias) => (
            <span key={alias} className="beat-chip">
              {alias}
              <Button
                type="button"
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => removeAlias(alias)}
                aria-label={`Remove alias ${alias}`}
              >
                ✕
              </Button>
            </span>
          ))}
        </div>
      )}
      <Command className="tag-command" shouldFilter={false}>
        <CommandInput
          placeholder="Add an alias…"
          value={query}
          onValueChange={setQuery}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ',') {
              e.preventDefault();
              if (query.trim()) addAlias(query);
            }
          }}
        />
      </Command>
    </div>
  );
}
