import { Toaster as SonnerToaster, type ToasterProps } from 'sonner';
import { useTheme } from '../ThemeProvider';

// Adapted from shadcn's sonner registry component: that scaffold assumes
// Next.js (next-themes + an app-specific icon set), neither of which this
// Vite app has. Wired to the app's own ThemeProvider instead.
function Toaster(props: ToasterProps) {
  const { theme } = useTheme();

  return (
    <SonnerToaster
      theme={theme}
      className="toaster group"
      style={
        {
          '--normal-bg': 'var(--popover)',
          '--normal-text': 'var(--popover-foreground)',
          '--normal-border': 'var(--border)',
          '--border-radius': 'var(--radius)',
        } as React.CSSProperties
      }
      {...props}
    />
  );
}

export { Toaster };
