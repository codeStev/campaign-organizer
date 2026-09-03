import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import {
  Sidebar,
  SidebarContent,
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
import { NextStubPage } from '../components/NextStubPage';
import { NextTopBar } from '../components/NextTopBar';
import { RelationshipsView } from './RelationshipsView';
import { ConsistencyView } from './ConsistencyView';
import { WhiteboardsView } from './WhiteboardsView';
import { NextChroniclePage } from './NextChroniclePage';
import { MapsView } from './MapsView';
import { NextWikiPage } from './NextWikiPage';

type Tab =
  | 'overview'
  | 'wiki'
  | 'atlas'
  | 'chronicle'
  | 'relations'
  | 'consistency'
  | 'campaigns'
  | 'encounters'
  | 'whiteboards';

type TabGroup = 'World' | 'Play';

/**
 * In-world /next nav (docs/ui-overhaul-plan.md Phase 1) — two groups, World
 * and Play, no separate Tools group (Consistency folds into World), per the
 * confirmed mockup-review decisions. Screens land real content phase by
 * phase (Phase 2 in progress); the rest stay NextStubPage placeholders.
 */
const TABS: { key: Tab; label: string; group: TabGroup }[] = [
  { key: 'overview', label: 'Overview', group: 'World' },
  { key: 'wiki', label: 'Wiki', group: 'World' },
  { key: 'atlas', label: 'Atlas', group: 'World' },
  { key: 'chronicle', label: 'Chronicle', group: 'World' },
  { key: 'relations', label: 'Relations', group: 'World' },
  { key: 'consistency', label: 'Consistency', group: 'World' },
  { key: 'campaigns', label: 'Campaigns', group: 'Play' },
  { key: 'encounters', label: 'Encounters', group: 'Play' },
  { key: 'whiteboards', label: 'Whiteboards', group: 'Play' },
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
  const activeTabKey = location.pathname.replace(`/next/worlds/${worldId}`, '').split('/').filter(Boolean)[0];
  const activeTab: Tab = TABS.some((t) => t.key === activeTabKey) ? (activeTabKey as Tab) : 'overview';
  const openArticle = (articleId: string) => navigate(`/next/worlds/${worldId}/wiki/${articleId}`);

  return (
    <section className="next-world-view">
      <NextTopBar
        currentWorldId={worldId}
        currentWorldName={worldName}
        rightSlot={
          <Button variant="link" size="sm" asChild>
            <NavLink to={`/worlds/${worldId}`}>← Back to current UI</NavLink>
          </Button>
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
        </Sidebar>
        <SidebarInset className="next-shell-content" style={{ alignSelf: 'stretch', height: 'auto' }}>
          <Routes>
            <Route index element={<Navigate to="overview" replace />} />
            <Route path="overview" element={<NextStubPage title="Overview" note="Phase 4 — dashboard, clocks, loose threads." />} />
            <Route path="wiki" element={<NextWikiPage worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route path="wiki/:articleId" element={<NextWikiPage worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route path="atlas" element={<MapsView worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />} />
            <Route
              path="atlas/:mapId"
              element={<MapsView worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="chronicle"
              element={<NextChroniclePage worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="relations"
              element={<RelationshipsView worldId={worldId} onOpenArticle={openArticle} onAuthExpired={onAuthExpired} />}
            />
            <Route
              path="consistency"
              element={
                <ConsistencyView
                  worldId={worldId}
                  worldName={worldName}
                  onOpenArticle={openArticle}
                  onAuthExpired={onAuthExpired}
                />
              }
            />
            <Route path="campaigns" element={<NextStubPage title="Campaigns" note="Phase 5 — richer session-prep workspace." />} />
            <Route path="encounters" element={<NextStubPage title="Encounters" note="Phase 5 — relocated encounter builder." />} />
            <Route path="whiteboards" element={<WhiteboardsView worldId={worldId} onAuthExpired={onAuthExpired} />} />
            <Route
              path="whiteboards/:whiteboardId"
              element={<WhiteboardsView worldId={worldId} onAuthExpired={onAuthExpired} />}
            />
            <Route path="*" element={<Navigate to="overview" replace />} />
          </Routes>
        </SidebarInset>
      </SidebarProvider>
    </section>
  );
}
