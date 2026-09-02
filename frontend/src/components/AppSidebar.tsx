import { NavLink, useLocation } from 'react-router-dom';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from './ui/sidebar';

/**
 * Persistent, app-wide nav (ADR-0098). Rendered for every top-level route;
 * a World takes over the screen with its own in-world sidebar instead.
 */
const ITEMS = [
  { to: '/worlds', label: 'Worlds', icon: '🌍', title: 'Your worlds and campaigns' },
  {
    to: '/templates/global',
    label: 'Templates',
    icon: '🧩',
    title: 'Global, system-scoped templates shared across every world',
  },
  {
    to: '/templates/statblocks',
    label: 'Statblocks',
    icon: '📋',
    title: 'Global, system-scoped statblock catalog',
  },
  {
    to: '/game-systems',
    label: 'Game Systems',
    icon: '🎲',
    title: 'The game systems your templates, statblocks, and campaigns are keyed by',
  },
  { to: '/settings', label: 'Settings', icon: '⚙', title: 'Settings' },
];

export function AppSidebar() {
  const location = useLocation();
  return (
    <Sidebar collapsible="none" className="border-r border-sidebar-border">
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {ITEMS.map((item) => (
                <SidebarMenuItem key={item.to}>
                  <SidebarMenuButton asChild isActive={location.pathname.startsWith(item.to)}>
                    <NavLink to={item.to} title={item.title}>
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
      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild>
              <NavLink to="/next/worlds" title="Preview the in-progress UI overhaul (docs/ui-overhaul-plan.md)">
                <span aria-hidden="true">✨</span>
                <span>Try the new UI</span>
              </NavLink>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
