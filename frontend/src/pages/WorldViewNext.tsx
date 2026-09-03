import { useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from '../components/ui/sidebar';
import { Button } from '../components/ui/button';
import { NextTopBar } from '../components/NextTopBar';
import { NextRelationshipsView } from './NextRelationshipsView';
import { NextConsistencyView } from './NextConsistencyView';
import { NextWhiteboardsView } from './NextWhiteboardsView';
import { NextTagBrowseView } from './NextTagBrowseView';
import { NextPlayersPanel } from './NextPlayersPanel';
import { NextSheetsPage } from './NextSheetsPage';
import { TablesView } from './TablesView';
import { HandoutsView } from './HandoutsView';
import { NextChroniclePage } from './NextChroniclePage';
import { NextOverviewPage } from './NextOverviewPage';
import { NextEncountersPage } from './NextEncountersPage';
import { NextCampaignsPage } from './NextCampaignsPage';
import { TableToolsDock } from '../components/TableToolsDock';
import { MapsView } from './MapsView';
import { NextWikiPage } from './NextWikiPage';
import { NextPrintShopPage } from './NextPrintShopPage';

type Tab =
  | 'overview'
  | 'wiki'
  | 'atlas'
  | 'chronicle'
  | 'relations'
  | 'tags'
  | 'consistency'
  | 'campaigns'
  | 'encounters'
  | 'players'
  | 'sheets'
  | 'whiteboards'
  | 'tables'
  | 'handouts';

type TabGroup = 'World' | 'Play';

/**
 * In-world /next nav (docs/ui-overhaul-plan.md Phase 1) — two groups, World
 * and Play, no separate Tools group (Consistency folds into World), per the
 * confirmed mockup-review decisions. Every tab now renders real content
 * (Phases 2-5 landed one screen at a time); Print Shop below is the one
 * bottom-pinned, ungrouped entry.
 */
const TABS: { key: Tab; label: string; group: TabGroup }[] = [
  { key: 'overview', label: 'Overview', group: 'World' },
  { key: 'wiki', label: 'Wiki', group: 'World' },
  { key: 'atlas', label: 'Atlas', group: 'World' },
  { key: 'chronicle', label: 'Chronicle', group: 'World' },
  { key: 'relations', label: 'Relations', group: 'World' },
  { key: 'tags', label: 'Tags', group: 'World' },
  { key: 'consistency', label: 'Consistency', group: 'World' },
  { key: 'campaigns', label: 'Campaigns', group: 'Play' },
  { key: 'encounters', label: 'Encounters', group: 'Play' },
  { key: 'players', label: 'Players', group: 'Play' },
  { key: 'sheets', label: 'Sheets', group: 'Play' },
  { key: 'whiteboards', label: 'Whiteboards', group: 'Play' },
  { key: 'tables', label: 'Tables & Decks', group: 'Play' },
  { key: 'handouts', label: 'Handouts', group: 'Play' },
];

const TAB_GROUPS: TabGroup[] = ['World', 'Play'];

interface Props {
  worldId: string;
  worldName: string;
  onAuthExpired: () => void;
}

export function WorldViewNext({ worldId, worldName, onAuthExpired }: Props) {
  const location = useLocation();
  const navigate = useNavigate();
  const [toolsOpen, setToolsOpen] = useState(false);
  const activeTabKey = location.pathname.replace(`/next/worlds/${worldId}`, '').split('/').filter(Boolean)[0];
  // Only a real match highlights a group item — falling back to 'overview'
  // for anything unrecognized would wrongly highlight it while on a
  // non-grouped route like Print Shop.
  const activeTab: Tab | null = TABS.some((t) => t.key === activeTabKey) ? (activeTabKey as Tab) : null;
  const openArticle = (articleId: string) => navigate(`/next/worlds/${worldId}/wiki/${articleId}`);
  const openStatblock = (id: string) => navigate(`/next/worlds/${worldId}/sheets/statblocks/${id}`);

  return (
    <section className="next-world-view">
      <NextTopBar
        currentWorldId={worldId}
        currentWorldName={worldName}
        rightSlot={
          <>
            <Button variant={toolsOpen ? 'default' : 'outline'} size="sm" onClick={() => setToolsOpen((v) => !v)}>
              🎲 Table Tools
            </Button>
            <Button variant="link" size="sm" asChild>
              <NavLink to={`/worlds/${worldId}`}>← Back to current UI</NavLink>
            </Button>
          </>
        }
      />

      <SidebarProvider className="min-h-0 sidebar-shell-next">
        <Sidebar collapsible="none" className="border-r border-sidebar-border" style={{ alignSelf: 'stretch', height: 'auto' }}>
          <SidebarContent>
            {TAB_GROUPS.map((group) => (
              <SidebarGroup key={group}>
                <SidebarGroupLabel>{group}</SidebarGroupLabel>
                <SidebarGroupContent>
                  <SidebarMenu>
                    {TABS.filter((t) => t.group === group).map((t) => (
                      <SidebarMenuItem key={t.key}>
                        <SidebarMenuButton asChild size="sm" isActive={activeTab === t.key}>
                          <NavLink to={t.key}>{t.label}</NavLink>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                    ))}
                  </SidebarMenu>
                </SidebarGroupContent>
              </SidebarGroup>
            ))}
          </SidebarContent>
          <SidebarFooter>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton asChild size="sm" isActive={activeTabKey === 'print'}>
                  <NavLink to="print">Print Shop</NavLink>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarFooter>
        </Sidebar>
        <SidebarInset className="next-shell-content" style={{ alignSelf: 'stretch', height: 'auto' }}>
          <Routes>
            <Route index element={<Navigate to="overview" replace />} />
            <Route
              path="overview"
              element={<NextOverviewPage worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route path="wiki" element={<NextWikiPage worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route path="wiki/:articleId" element={<NextWikiPage worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route path="atlas" element={<MapsView worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />} />
            <Route
              path="atlas/:mapId"
              element={<MapsView worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="chronicle/*"
              element={<NextChroniclePage worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="relations"
              element={<NextRelationshipsView worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="tags"
              element={
                <NextTagBrowseView
                  worldId={worldId}
                  onOpenArticle={openArticle}
                  onOpenStatblock={openStatblock}
                  onAuthExpired={onAuthExpired}
                />
              }
            />
            <Route
              path="tags/:tagName"
              element={
                <NextTagBrowseView
                  worldId={worldId}
                  onOpenArticle={openArticle}
                  onOpenStatblock={openStatblock}
                  onAuthExpired={onAuthExpired}
                />
              }
            />
            <Route
              path="consistency"
              element={
                <NextConsistencyView
                  worldId={worldId}
                  worldName={worldName}
                  onOpenArticle={openArticle}
                  onAuthExpired={onAuthExpired}
                />
              }
            />
            <Route
              path="campaigns"
              element={<NextCampaignsPage worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="campaigns/:campaignId"
              element={<NextCampaignsPage worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route path="encounters" element={<NextEncountersPage worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route
              path="encounters/:campaignId"
              element={<NextEncountersPage worldId={worldId} onAuthExpired={onAuthExpired} />}
            />
            <Route path="players" element={<NextPlayersPanel worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route
              path="sheets/*"
              element={<NextSheetsPage worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route path="whiteboards" element={<NextWhiteboardsView worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route
              path="whiteboards/:whiteboardId"
              element={<NextWhiteboardsView worldId={worldId} onAuthExpired={onAuthExpired} />}
            />
            <Route path="tables" element={<TablesView worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route path="tables/:kind" element={<TablesView worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route
              path="tables/:kind/:entityId"
              element={<TablesView worldId={worldId} onAuthExpired={onAuthExpired} />}
            />
            <Route path="handouts" element={<HandoutsView worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route
              path="handouts/:handoutId"
              element={<HandoutsView worldId={worldId} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="print"
              element={<NextPrintShopPage worldId={worldId} worldName={worldName} onAuthExpired={onAuthExpired} />}
            />
            <Route path="*" element={<Navigate to="overview" replace />} />
          </Routes>
        </SidebarInset>
        <TableToolsDock
          worldId={worldId}
          open={toolsOpen}
          onAuthExpired={onAuthExpired}
          onClose={() => setToolsOpen(false)}
        />
      </SidebarProvider>
    </section>
  );
}
