import { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import {
  fieldTemplatesApi,
  globalFieldTemplatesApi,
  characterSheetsApi,
  statblocksApi,
  documentsApi,
  sheetCategoriesApi,
  articlesApi,
  campaignsApi,
  FieldTemplate,
  GlobalFieldTemplate,
  CharacterSheet,
  Statblock,
  Document,
  SheetCategory,
  ArticleSummary,
  Campaign,
  ApiError,
} from '../api/client';
import { DiceRollerWidget } from '../components/DiceRollerWidget';
import { CategoryTree } from '../components/CategoryTree';
import { CharacterSheetsPanel } from './CharacterSheetsPanel';
import { StatblocksPanel } from './StatblocksPanel';
import { DocumentsPanel } from './DocumentsPanel';
import { FieldTemplatesPanel } from './FieldTemplatesPanel';
import { Button } from '../components/ui/button';
import { TruncatedLabel } from '../components/TruncatedLabel';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
  /** Old UI has no Table Tools dock (its own dice roller), so it still needs this one; `/next` doesn't. */
  showDiceRoller?: boolean;
}

/** One row in the merged Sheets category tree (ADR-0105). */
type TreeItem =
  | { kind: 'sheet'; entity: CharacterSheet }
  | { kind: 'statblock'; entity: Statblock }
  | { kind: 'document'; entity: Document }
  | { kind: 'template'; entity: FieldTemplate };

const KIND_SEGMENT: Record<TreeItem['kind'], string> = {
  sheet: 'characters',
  statblock: 'statblocks',
  document: 'documents',
  template: 'templates',
};

const KIND_ICON: Record<TreeItem['kind'], string> = {
  sheet: '🧑',
  statblock: '👹',
  document: '📄',
  template: '📐',
};

const KIND_SEGMENTS = ['characters', 'statblocks', 'documents', 'templates'];

export function SheetsView({ worldId, onOpenArticle, onAuthExpired, showDiceRoller = true }: Props) {
  const navigate = useNavigate();
  const location = useLocation();
  // The last URL segment when it's an entity id, not a kind segment or the
  // "new" sentinel — drives the tree's auto-expand-to-active-entity effect.
  const activeEntityId = useMemo(() => {
    const segments = location.pathname.split('/').filter(Boolean);
    const last = segments[segments.length - 1];
    if (!last || last === 'new' || KIND_SEGMENTS.includes(last)) return null;
    return last;
  }, [location.pathname]);
  const sheetsApiRef = useMemo(() => characterSheetsApi(worldId), [worldId]);
  const statblocksApiRef = useMemo(() => statblocksApi(worldId), [worldId]);
  const documentsApiRef = useMemo(() => documentsApi(worldId), [worldId]);
  const templatesApiRef = useMemo(() => fieldTemplatesApi(worldId), [worldId]);
  const categoriesApiRef = useMemo(() => sheetCategoriesApi(worldId), [worldId]);
  const articleApi = useMemo(() => articlesApi(worldId), [worldId]);
  const campaignApi = useMemo(() => campaignsApi(worldId), [worldId]);

  const [sheets, setSheets] = useState<CharacterSheet[]>([]);
  const [statblocks, setStatblocks] = useState<Statblock[]>([]);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [templates, setTemplates] = useState<FieldTemplate[]>([]);
  const [globalTemplates, setGlobalTemplates] = useState<GlobalFieldTemplate[]>([]);
  const [categories, setCategories] = useState<SheetCategory[]>([]);
  const [loading, setLoading] = useState(true);
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

  const refreshAll = useCallback(async () => {
    try {
      const [sheetsList, statblocksList, documentsList, templatesList, globalTemplatesList, categoriesList] =
        await Promise.all([
          sheetsApiRef.list(),
          statblocksApiRef.list(),
          documentsApiRef.list(),
          templatesApiRef.list(),
          globalFieldTemplatesApi.list(),
          categoriesApiRef.list(),
        ]);
      setSheets(sheetsList);
      setStatblocks(statblocksList);
      setDocuments(documentsList);
      setTemplates(templatesList);
      setGlobalTemplates(globalTemplatesList);
      setCategories(categoriesList);
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [sheetsApiRef, statblocksApiRef, documentsApiRef, templatesApiRef, categoriesApiRef, onError]);

  useEffect(() => {
    void refreshAll();
    articleApi.list().then(setArticles).catch(onError);
    campaignApi.list().then(setCampaigns).catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId]);

  // Every kind merged into one category tree (ADR-0105), kind shown per row.
  const treeItems: TreeItem[] = useMemo(
    () => [
      ...sheets.map((entity): TreeItem => ({ kind: 'sheet', entity })),
      ...statblocks.map((entity): TreeItem => ({ kind: 'statblock', entity })),
      ...documents.map((entity): TreeItem => ({ kind: 'document', entity })),
      ...templates.map((entity): TreeItem => ({ kind: 'template', entity })),
    ],
    [sheets, statblocks, documents, templates],
  );

  async function createCategory(name: string, parentId: string | null) {
    try {
      await categoriesApiRef.create({ name, parentId });
      await refreshAll();
    } catch (err) {
      onError(err);
    }
  }

  async function removeCategory(category: SheetCategory) {
    try {
      await categoriesApiRef.remove(category.id);
      await refreshAll();
    } catch (err) {
      onError(err);
    }
  }

  // Every owner's update() must carry its full existing state, or an
  // unrelated field would get clobbered — mirrors Atlas/Handouts/Tables&Decks.
  async function moveEntityToCategory(item: TreeItem, categoryId: string | null) {
    if ((item.entity.categoryId ?? null) === categoryId) return;
    try {
      switch (item.kind) {
        case 'sheet': {
          const s = item.entity;
          await sheetsApiRef.update(s.id, {
            categoryId,
            name: s.name,
            worldTemplateId: s.worldTemplateId,
            globalTemplateId: s.globalTemplateId,
            articleId: s.articleId ?? null,
            campaignId: s.campaignId ?? null,
            values: s.values,
          });
          break;
        }
        case 'statblock': {
          const s = item.entity;
          await statblocksApiRef.update(s.id, {
            categoryId,
            name: s.name,
            articleId: s.articleId ?? null,
            campaignId: s.campaignId ?? null,
            worldTemplateId: s.worldTemplateId ?? null,
            globalTemplateId: s.globalTemplateId ?? null,
            stats: s.stats,
            notes: s.notes ?? null,
          });
          break;
        }
        case 'document': {
          const d = item.entity;
          await documentsApiRef.update(d.id, {
            categoryId,
            name: d.name,
            templateId: d.templateId,
            campaignId: d.campaignId ?? null,
            values: d.values,
          });
          break;
        }
        case 'template': {
          const t = item.entity;
          await templatesApiRef.update(t.id, {
            categoryId,
            name: t.name,
            kind: t.kind,
            systemId: t.systemId ?? null,
            sections: t.sections,
          });
          break;
        }
      }
      await refreshAll();
    } catch (err) {
      onError(err);
    }
  }

  const charactersPane = (
    <CharacterSheetsPanel
      worldId={worldId}
      templates={templates}
      globalTemplates={globalTemplates}
      articles={articles}
      campaigns={campaigns}
      onOpenArticle={onOpenArticle}
      onChanged={refreshAll}
      onError={onError}
    />
  );
  const statblocksPane = (
    <StatblocksPanel
      worldId={worldId}
      templates={templates}
      globalTemplates={globalTemplates}
      campaigns={campaigns}
      onChanged={refreshAll}
      onError={onError}
    />
  );
  const documentsPane = (
    <DocumentsPanel
      worldId={worldId}
      templates={templates}
      campaigns={campaigns}
      onChanged={refreshAll}
      onError={onError}
    />
  );
  const templatesPane = (
    <FieldTemplatesPanel
      worldId={worldId}
      templates={templates}
      loading={loading}
      onChanged={refreshAll}
      onError={onError}
    />
  );

  return (
    <div className="wiki-layout">
      <aside className="wiki-sidebar">
        {showDiceRoller && <DiceRollerWidget onAuthExpired={onAuthExpired} />}
        <div className="editor-actions">
          <Button size="sm" onClick={() => navigate('characters/new')}>
            + New sheet
          </Button>
          <Button size="sm" onClick={() => navigate('statblocks/new')}>
            + New statblock
          </Button>
        </div>
        <div className="editor-actions">
          <Button size="sm" onClick={() => navigate('documents/new')}>
            + New document
          </Button>
          <Button size="sm" onClick={() => navigate('templates')}>
            + New template
          </Button>
        </div>
        <CategoryTree
          categories={categories}
          entities={treeItems}
          entityId={(i) => i.entity.id}
          entityLabel={(i) => i.entity.name}
          entityCategoryId={(i) => i.entity.categoryId ?? null}
          activeEntityId={activeEntityId}
          onOpenEntity={(id) => {
            const item = treeItems.find((i) => i.entity.id === id);
            if (item) navigate(`${KIND_SEGMENT[item.kind]}/${id}`);
          }}
          onMoveEntity={(item, categoryId) => void moveEntityToCategory(item, categoryId)}
          onCreateCategory={(name, parentId) => void createCategory(name, parentId)}
          onRemoveCategory={(c) => void removeCategory(c)}
          loading={loading}
          searchPlaceholder="Search sheets…"
          emptyLabel="No sheets, statblocks, documents, or templates yet."
          renderEntityRow={(i) => (
            <TruncatedLabel label={i.entity.name}>
              {KIND_ICON[i.kind]} {i.entity.name}
            </TruncatedLabel>
          )}
        />
      </aside>

      <div className="wiki-main">
        {error && <p className="error">{error}</p>}
        <Routes>
          <Route index element={<Navigate to="characters" replace />} />
          <Route path="characters" element={charactersPane} />
          <Route path="characters/:sheetId" element={charactersPane} />
          <Route path="statblocks" element={statblocksPane} />
          <Route path="statblocks/:statblockId" element={statblocksPane} />
          <Route path="documents" element={documentsPane} />
          <Route path="documents/:documentId" element={documentsPane} />
          <Route path="templates" element={templatesPane} />
          <Route path="templates/:templateId" element={templatesPane} />
          <Route path="*" element={<Navigate to="characters" replace />} />
        </Routes>
      </div>
    </div>
  );
}
