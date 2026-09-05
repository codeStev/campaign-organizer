import { NavLink, useLocation } from 'react-router-dom';
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from './ui/sidebar';
import { useIsMobile } from '../hooks/use-mobile';

/**
 * /next app-wide nav (docs/ui-overhaul-plan.md Phase 1) — two sections:
 * Worlds/Settings as flat entries, and a "Library" group for the
 * world-independent catalogs (Game Systems/Statblocks/Templates stay three
 * separate pages per ADR-0098, just grouped here under one label, matching
 * the reviewed mockup's framing without merging the pages themselves). All
 * three now have their own /next-chrome routes: Game Systems reuses
 * GameSystemsPage unchanged; Templates/Statblocks route to
 * NextGlobalTemplatesPanel/NextGlobalStatblocksPanel (ADR-0106).
 */
const TOP_ITEMS = [{ to: '/next/worlds', label: 'Worlds', icon: '🌍' }];

const LIBRARY_ITEMS = [
  { to: '/next/templates/global', label: 'Templates', icon: '🧩' },
  { to: '/next/templates/statblocks', label: 'Statblocks', icon: '📋' },
  { to: '/next/game-systems', label: 'Game Systems', icon: '🎲' },
];

const BOTTOM_ITEMS = [{ to: '/next/settings', label: 'Settings', icon: '⚙' }];

export function AppSidebarNext() {
  const location = useLocation();
  const isMobile = useIsMobile();
  const isActive = (to: string) => location.pathname.startsWith(to);
  return (
    <Sidebar
      collapsible={isMobile ? 'offcanvas' : 'none'}
      className="border-r border-sidebar-border"
      style={{ alignSelf: 'stretch', height: 'auto' }}
    >
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {TOP_ITEMS.map((item) => (
                <SidebarMenuItem key={item.to}>
                  <SidebarMenuButton asChild size="sm" isActive={isActive(item.to)}>
                    <NavLink to={item.to}>
                      <span aria-hidden="true">{item.icon}</span>
                      <span>{item.label}</span>
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupLabel className="eyebrow">Library · all worlds</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {LIBRARY_ITEMS.map((item) => (
                <SidebarMenuItem key={item.to}>
                  <SidebarMenuButton asChild size="sm" isActive={isActive(item.to)}>
                    <NavLink to={item.to}>
                      <span aria-hidden="true">{item.icon}</span>
                      <span>{item.label}</span>
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {BOTTOM_ITEMS.map((item) => (
                <SidebarMenuItem key={item.to}>
                  <SidebarMenuButton asChild size="sm" isActive={isActive(item.to)}>
                    <NavLink to={item.to}>
                      <span aria-hidden="true">{item.icon}</span>
                      <span>{item.label}</span>
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
