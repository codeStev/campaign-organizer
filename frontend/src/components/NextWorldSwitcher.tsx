import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { worldsApi, World } from '../api/client';
import { Button } from './ui/button';

interface Props {
  currentWorldId?: string;
  currentWorldName?: string;
}

/**
 * World switcher, matching the reviewed mockup's header pattern — a
 * compact pill button opening a small anchored panel, not a dedicated
 * full-page world list (docs/ui-overhaul-plan.md Phase 1 follow-up).
 *
 * Plain positioned-div dropdown, not shadcn's Popover: the mockup itself
 * uses a manual absolute-positioned panel rather than a floating-ui
 * library, and matching that sidesteps a reproducible Radix Popper bug
 * hit here — its content stayed pinned at Popper's pre-measurement
 * placeholder transform (translate(0,-200%)) and never repositioned,
 * even though the trigger's own getBoundingClientRect() was valid
 * throughout. Root cause not identified; Select's (non-Popper,
 * item-aligned) dropdown positioning is unaffected. Full world management
 * (create/backup/import/delete) stays on the old /worlds page for now.
 */
export function NextWorldSwitcher({ currentWorldId, currentWorldName }: Props) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [worlds, setWorlds] = useState<World[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (open) worldsApi.list().then(setWorlds).catch(() => {});
  }, [open]);

  useEffect(() => {
    if (!open) return;
    function onPointerDown(e: PointerEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) setOpen(false);
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  return (
    <div className="next-world-switcher" ref={containerRef}>
      <Button
        variant="outline"
        size="sm"
        className="next-world-switcher-trigger"
        onClick={() => setOpen((o) => !o)}
      >
        <span className="next-world-dot" aria-hidden="true" />
        <span>{currentWorldName ?? 'Choose a world'}</span>
        <span className="muted">WORLD ▾</span>
      </Button>
      {open && (
        <div className="next-world-switcher-panel">
          <div className="next-world-switcher-heading muted">Worlds · {worlds.length}</div>
          <ul className="next-world-switcher-list">
            {worlds.map((w) => (
              <li key={w.id}>
                <button
                  type="button"
                  className={`next-world-switcher-item${w.id === currentWorldId ? ' active' : ''}`}
                  onClick={() => {
                    setOpen(false);
                    navigate(`/next/worlds/${w.id}`);
                  }}
                >
                  <span className="next-world-dot" aria-hidden="true" />
                  <span>{w.name}</span>
                </button>
              </li>
            ))}
            {worlds.length === 0 && <li className="muted next-world-switcher-empty">No worlds yet.</li>}
          </ul>
          <div className="next-world-switcher-footer">
            <Button
              variant="link"
              size="sm"
              onClick={() => {
                setOpen(false);
                navigate('/worlds');
              }}
            >
              Manage worlds →
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
