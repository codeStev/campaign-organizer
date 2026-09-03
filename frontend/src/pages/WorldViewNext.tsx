import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
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
 * confirmed mockup-review decisions. All screens are stubs until their own
 * phase lands real content.
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
}

export function WorldViewNext({ worldId, worldName }: Props) {
  const location = useLocation();
  const activeTabKey = location.pathname.replace(`/next/worlds/${worldId}`, '').split('/').filter(Boolean)[0];
  const activeTab: Tab = TABS.some((t) => t.key === activeTabKey) ? (activeTabKey as Tab) : 'overview';

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
            <Route path="wiki" element={<NextStubPage title="Wiki" note="Phase 2 — articles, flat category list first." />} />
            <Route path="atlas" element={<NextStubPage title="Atlas" note="Phase 2 — maps." />} />
            <Route path="chronicle" element={<NextStubPage title="Chronicle" note="Phase 2 — timelines + calendars merged." />} />
            <Route path="relations" element={<NextStubPage title="Relations" note="Phase 2." />} />
            <Route path="consistency" element={<NextStubPage title="Consistency" note="Phase 2." />} />
            <Route path="campaigns" element={<NextStubPage title="Campaigns" note="Phase 5 — richer session-prep workspace." />} />
            <Route path="encounters" element={<NextStubPage title="Encounters" note="Phase 5 — relocated encounter builder." />} />
            <Route path="whiteboards" element={<NextStubPage title="Whiteboards" note="Phase 5." />} />
            <Route path="*" element={<Navigate to="overview" replace />} />
          </Routes>
        </SidebarInset>
      </SidebarProvider>
    </section>
  );
}
