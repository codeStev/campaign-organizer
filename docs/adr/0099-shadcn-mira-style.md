# 0099. Adopt shadcn's "mira" style + "stone" base color

## Status
Accepted

## Context
The user commissioned a Claude Design mockup exploring a fresh visual
direction and nav structure for the app (see the UI overhaul plan,
`docs/ui-overhaul-plan.md`). Reviewing it, the user liked several structural
and functional ideas (covered in that plan) but explicitly did not want to
adopt the mockup's own hand-rolled visual system (inline-styled `.dc.html`,
a hand-picked amber/brass accent, Newsreader/IBM Plex Mono fonts) — that
would mean abandoning the Tailwind + shadcn component architecture and its
swappability. The user also pointed out that the app's *existing* purple
accent (`#6d54c9`, ADR-0063) was never a real brand color either — just a
value an earlier session picked — and asked to move to a real shadcn style
preset instead of another hand-picked palette, specifically **mira**
("compact, made for dense interfaces" — a good match for this data-dense
tool) with "an appropriate theme," leaving the exact color secondary to
keeping the palette easily swappable going forward.

## Decision
Re-ran `npx shadcn@latest init --preset=mira -b radix --reinstall` against
`frontend/`, switching `components.json`'s `style` from `radix-nova` to
`radix-mira` and regenerating all 21 installed `components/ui/*.tsx` files
in the new style (tighter padding/sizing, `xs`/`sm` text scale throughout).
Base color set to **stone** (warm neutral) via
`npx shadcn@latest migrate base-color --to stone` — replacing the old
hand-picked hex palette with shadcn's generated `oklch()` token scale in
both `:root` and `.dark`, still swappable with one more `migrate
base-color` call later rather than hand-edited hex values.

**Icon library reverted to lucide.** The mira preset defaults to
`hugeicons`; reinstalling with `--reinstall` pulled `@hugeicons/*` into
every regenerated component, which would have left two icon libraries
coexisting (every non-`ui/` component in the app still uses `lucide-react`).
Ran `npx shadcn@latest migrate icons --from hugeicons --to lucide`
immediately after, then hand-fixed the one icon `migrate` couldn't map
(`sonner.tsx`'s info toast icon → lucide's `InfoIcon`), and removed the now
unused `@hugeicons/*` packages from `package.json`.

**Two hand-added customizations, lost by the blanket regeneration, restored
on top of the new mira styling:**
- `select.tsx`'s `SelectContent` `container` prop (portal target override —
  defaults to `document.body`, which is wrong when the Select renders
  inside a popped-out print window; see `NewWindowPortal`). Re-added with
  its original explanatory comment.
- `index.css`'s `--font-sans` symbol-glyph fallback chain (Noto Sans
  Symbols/Symbols 2, self-hosted) — the reinit collapsed it down to just
  `'Inter Variable', sans-serif` (mira's font choice), dropping the
  fallback that keeps arrow/symbol Unicode characters from rendering as
  tofu on some system+browser combos. Inter Variable now leads the same
  fallback stack instead of replacing it.

One more stray value fixed: the mira preset's dark-theme `--sidebar-primary`
default is a hardcoded saturated blue (`oklch(0.488 0.243 264.376)`),
inconsistent with the rest of the now-neutral stone palette and unrelated
to the `stone` base-color migration (that migration doesn't touch
`sidebar-primary`'s dark value). Currently unused by our `sidebar.tsx`
usage (no `data-active` styling reads it), but corrected to match the
palette for when it does get used.

## Consequences
- Every page using shadcn `ui/` components (i.e. the whole app) picks up
  the new compact mira spacing and neutral stone palette immediately —
  this is a system-wide restyle, not scoped to new pages only.
- `docs/ui-overhaul-plan.md` covers everything else from the Claude Design
  review (nav/IA, new features, page-by-page migration sequencing) as
  later, separate phases — this ADR is style/tokens only.
- The `--font-sans`/`select.tsx` `container` prop loss is a reminder that
  `--reinstall`/`--overwrite` on shadcn `ui/` files silently drops any
  hand customization layered on top of the generated source — worth a
  deliberate `git diff` review (not just a build check) after any future
  preset/style/base-color migration.

## Alternatives considered
- **Hand-picked replacement palette (another custom hex scale), matching
  the Claude Design mockup's amber/brass accent.** Rejected — the whole
  point raised was to stop hand-picking colors and use a real, swappable
  shadcn preset instead.
- **Keep `radix-nova` + old purple, adopt only the mockup's structural
  ideas.** Rejected — the user asked specifically for mira + an appropriate
  theme now, independent of the structural/IA work.
