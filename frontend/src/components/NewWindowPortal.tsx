import { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { toast } from 'sonner';

const NewWindowContainerContext = createContext<HTMLElement | null>(null);

/**
 * The popped-out window's mount element, for components that need an explicit
 * portal container (Radix Select/Dialog dropdowns etc.) — without this they
 * default to the *main* window's document.body, rendering invisibly behind
 * the print window instead of inside it.
 */
export function useNewWindowContainer(): HTMLElement | null {
  return useContext(NewWindowContainerContext);
}

interface Props {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/**
 * Renders its children into a separate browser tab (ADR-0038) so print/PDF views
 * don't hijack the app tab. Clones the app's stylesheets into the new document so
 * the `.print-*` styles apply, and calls onClose when that tab is closed.
 */
export function NewWindowPortal({ title, onClose, children }: Props) {
  const [container, setContainer] = useState<HTMLElement | null>(null);

  useEffect(() => {
    const win = window.open('', '_blank');
    if (!win) {
      // Popup blocked — nothing to render; let the parent reset its state.
      toast.error('Your browser blocked the print window. Allow popups for this site and try again.');
      onClose();
      return;
    }
    win.document.title = title;

    // Copy stylesheets so the print styles resolve in the new document.
    document.querySelectorAll('link[rel="stylesheet"], style').forEach((node) => {
      if (node instanceof HTMLLinkElement) {
        const link = win.document.createElement('link');
        link.rel = 'stylesheet';
        link.href = node.href; // resolved absolute URL, same origin
        win.document.head.appendChild(link);
      } else {
        win.document.head.appendChild(node.cloneNode(true));
      }
    });

    const mount = win.document.createElement('div');
    mount.className = 'print-window';
    win.document.body.style.margin = '0';
    win.document.body.appendChild(mount);
    setContainer(mount);

    win.addEventListener('beforeunload', onClose);
    return () => {
      win.removeEventListener('beforeunload', onClose);
      win.close();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!container) return null;
  return createPortal(
    <NewWindowContainerContext.Provider value={container}>{children}</NewWindowContainerContext.Provider>,
    container,
  );
}
