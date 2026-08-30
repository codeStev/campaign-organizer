# 77. Print buttons must target the popped-out window explicitly

Date: 2026-08-30
Status: Accepted

## Context

ADR-0038 established `NewWindowPortal`: every print view renders into a
separate browser tab via `window.open('', '_blank')` and `createPortal`, so
printing never hijacks the app tab. Every print view's "🖨 Print" button
called plain `window.print()`.

A user report ("in the print view when I click the print button this opens
[the campaigns URL]... that's not what I want to print") traced to a real
bug in that pattern, not the URL confusion it first looked like: clicking
the popup's own Print button showed a native print dialog for the app's
live UI (nav bar, sidebar) rather than the clean print document — and it
appeared only after switching back to the original tab.

Root cause: `createPortal` only moves *where* a component's DOM output
renders. The component's *code* — including every closure, including the
Print button's `onClick`— still executes in the JS realm of whichever
document loaded the script, which is always the original app tab (the
print window never loads the app bundle; it only receives cloned
stylesheets and a portalled DOM subtree). So the bare identifier `window`
inside that `onClick` always resolved to the original tab's `window`, not
the print window's. `window.print()` queued a print job against the
backgrounded original tab, invisible until the user switched back to it.

This is a latent bug that predates this session — it was never caught
because nobody had reason to switch back to the app tab immediately after
printing and notice a stray dialog waiting there.

## Decision

`NewWindowPortal` already solves the identical class of problem for Radix
portals: `useNewWindowContainer()` exposes the popup's mount element so
`SelectContent` etc. render into the right document instead of defaulting
to the main window's `document.body`. Extended the same mechanism one
level up: `NewWindowPortal` now also captures the popup's actual `Window`
object (the same `win` it already uses for the title/stylesheets/mount
div) and exposes it via a new `useNewWindowRef()` hook, plus a shared
`<PrintButton>` component (`win.print()` instead of `window.print()`) that
every print view now renders instead of its own inline
`<Button onClick={() => window.print()}>`.

Like `useNewWindowContainer()`'s existing consumers (`ScopeSelect` in
`PrintView.tsx`, `FilterSelect` in `MapPrintView.tsx`), `<PrintButton>`
works correctly specifically because it's authored as its own component
and placed as JSX *inside* `<NewWindowPortal>` at each of the 10 call
sites — `useContext` resolves based on where a component instance sits in
the React tree, not where the JSX literal appears in source, so this
would silently fail again if a future call site called `window.print()`
inline instead of using `<PrintButton>`.

## Consequences

- All 10 print views (`PrintView`, `SessionPacketView`, `MapPrintView`,
  `StatblockCardsView`, `EncounterSheetView`, `RecapView`, `HandoutsView`,
  `CheatSheetView`, `TablesView`, `ConsistencyView`) now use
  `<PrintButton>` instead of a raw `window.print()` call.
- Any future print view must do the same — a code-review checklist item,
  not something a type system can enforce here.
