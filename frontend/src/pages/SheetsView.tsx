import { useCallback, useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import {
  fieldTemplatesApi,
  articlesApi,
  campaignsApi,
  FieldTemplate,
  ArticleSummary,
  Campaign,
  ApiError,
} from '../api/client';
import { DiceRollerWidget } from '../components/DiceRollerWidget';
import { CharacterSheetsPanel } from './CharacterSheetsPanel';
import { StatblocksPanel } from './StatblocksPanel';
import { FieldTemplatesPanel } from './FieldTemplatesPanel';
import { Tabs, TabsList, TabsTrigger } from '../components/ui/tabs';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

const SUBTABS = ['characters', 'statblocks', 'templates'] as const;

export function SheetsView({ worldId, onOpenArticle, onAuthExpired }: Props) {
  const location = useLocation();
  const activeSubtab = SUBTABS.find((t) => location.pathname.includes(`/${t}`)) ?? 'characters';
  const templatesApiRef = useMemo(() => fieldTemplatesApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);
  const campaignApi = useMemo(() => campaignsApi(worldId), [worldId]);
  const [templates, setTemplates] = useState<FieldTemplate[]>([]);
  const [templatesLoading, setTemplatesLoading] = useState(true);
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [error, setError] = useState<string | null>(null);

  const onError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const loadTemplates = useCallback(() => {
    templatesApiRef
      .list()
      .then(setTemplates)
      .catch(onError)
      .finally(() => setTemplatesLoading(false));
  }, [templatesApiRef, onError]);

  useEffect(() => {
    loadTemplates();
    articleApi.list().then(setArticles).catch(onError);
    campaignApi.list().then(setCampaigns).catch(onError);
  }, [loadTemplates, articleApi, campaignApi, onError]);

  const charactersPane = (
    <CharacterSheetsPanel
      worldId={worldId}
      templates={templates}
      articles={articles}
      campaigns={campaigns}
      onOpenArticle={onOpenArticle}
      onError={onError}
    />
  );
  const statblocksPane = (
    <StatblocksPanel worldId={worldId} templates={templates} campaigns={campaigns} onError={onError} />
  );
  const templatesPane = (
    <FieldTemplatesPanel
      worldId={worldId}
      templates={templates}
      loading={templatesLoading}
      onChanged={loadTemplates}
      onError={onError}
    />
  );

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        <DiceRollerWidget onAuthExpired={onAuthExpired} />
        <Tabs value={activeSubtab} className="subtabs">
          <TabsList variant="line">
            {SUBTABS.map((t) => (
              <TabsTrigger key={t} value={t} asChild>
                <NavLink to={t}>{t.charAt(0).toUpperCase() + t.slice(1)}</NavLink>
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        <Routes>
          <Route index element={<Navigate to="characters" replace />} />
          <Route path="characters" element={charactersPane} />
          <Route path="characters/:sheetId" element={charactersPane} />
          <Route path="statblocks" element={statblocksPane} />
          <Route path="statblocks/:statblockId" element={statblocksPane} />
          <Route path="templates" element={templatesPane} />
          <Route path="templates/:templateId" element={templatesPane} />
          <Route path="*" element={<Navigate to="characters" replace />} />
        </Routes>
      </div>
    </div>
  );
}
