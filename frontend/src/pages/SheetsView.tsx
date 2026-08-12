import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  sheetTemplatesApi,
  articlesApi,
  SheetTemplate,
  ArticleSummary,
  ApiError,
} from '../api/client';
import { DiceRollerWidget } from '../components/DiceRollerWidget';
import { CharacterSheetsPanel } from './CharacterSheetsPanel';
import { StatblocksPanel } from './StatblocksPanel';
import { SheetTemplatesPanel } from './SheetTemplatesPanel';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

type SubTab = 'characters' | 'statblocks' | 'templates';

export function SheetsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const templatesApiRef = useMemo(() => sheetTemplatesApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);
  const [sub, setSub] = useState<SubTab>('characters');
  const [templates, setTemplates] = useState<SheetTemplate[]>([]);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  const onError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const loadTemplates = useCallback(() => {
    templatesApiRef.list().then(setTemplates).catch(onError);
  }, [templatesApiRef, onError]);

  useEffect(() => {
    loadTemplates();
    articleApi.list().then(setArticles).catch(onError);
  }, [loadTemplates, articleApi, onError]);

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <DiceRollerWidget onAuthExpired={onAuthExpired} />
        <nav className="subtabs">
          {(['characters', 'statblocks', 'templates'] as SubTab[]).map((t) => (
            <button
              key={t}
              className={sub === t ? 'tab active' : 'tab'}
              onClick={() => setSub(t)}
            >
              {t.charAt(0).toUpperCase() + t.slice(1)}
            </button>
          ))}
        </nav>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        {sub === 'characters' && (
          <CharacterSheetsPanel
            worldId={worldId}
            templates={templates}
            articles={articles}
            onOpenArticle={onOpenArticle}
            onError={onError}
          />
        )}
        {sub === 'statblocks' && <StatblocksPanel worldId={worldId} onError={onError} />}
        {sub === 'templates' && (
          <SheetTemplatesPanel
            worldId={worldId}
            templates={templates}
            onChanged={loadTemplates}
            onError={onError}
          />
        )}
      </div>
    </div>
  );
}
