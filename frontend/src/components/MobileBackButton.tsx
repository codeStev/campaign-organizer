import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from './ui/button';

/**
 * Mobile-only "← Back to list" affordance for `.wiki-layout` (and similar)
 * screens once Phase 2's CSS hides the sidebar in favor of the open detail
 * view (see `.wiki-layout[data-has-selection]` in index.css) — desktop
 * shows list and detail side by side, so it never needs this. Always
 * navigates via browser history rather than a per-page target: opening an
 * item is itself a `navigate()` push, so going back always lands on
 * whatever list state preceded it, with no per-screen wiring needed.
 * Hidden above the 767px breakpoint by its own CSS class.
 */
export function MobileBackButton() {
  const navigate = useNavigate();
  return (
    <Button variant="outline" size="sm" className="mobile-back-button" onClick={() => navigate(-1)}>
      <ArrowLeft /> Back
    </Button>
  );
}
