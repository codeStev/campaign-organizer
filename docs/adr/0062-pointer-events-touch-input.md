# ADR-0062: Pointer Events for canvas drag interactions; mobile/tablet touch support

- Status: Accepted
- Date: 2026-08-25

## Context
A UI review for responsiveness/UX on desktop, tablet, and phone found that two
interactive components were desktop-only in practice, despite the app rendering
a usable layout on narrow screens:

- **Whiteboards** (FR-20, ADR-0027): `WhiteboardCanvas` wired only
  `onMouseDown`/`onMouseMove`/`onMouseUp`. Mouse Events never fire for touch
  input, so dragging or connecting nodes did nothing on a phone or tablet.
- **Sheet template builder** (FR-4, ADR-0030): `TemplateBuilder` used the
  native HTML5 Drag and Drop API (`draggable`, `onDragStart`/`onDragOver`/
  `onDrop`) to move a component from the palette into a section, and to
  reorder fields by their drag handle. HTML5 DnD has no touch equivalent on
  iOS Safari and most mobile browsers, so adding a field to a template was
  impossible on a touch device (the existing ↑/↓ buttons let a touch user
  reorder fields already in a section, but not insert a new one).

A separate, unrelated CSS bug was found in the same pass: both the command
palette (`CommandPalette`, FR-26) and the template builder's component tray
used `className="palette"`. The two rules collided in the cascade — the
later-loaded rule's `border-style`, `border-radius`, and `align-items: center`
leaked into the command palette modal, breaking its width and corner styling.

## Decision
- Replace Mouse Events with the **Pointer Events API**
  (`onPointerDown`/`onPointerMove`/`onPointerUp` + `setPointerCapture`) as the
  standard interaction model for custom drag gestures. Pointer Events unify
  mouse, touch, and pen through one code path, so no parallel touch handling
  is needed.
  - `WhiteboardCanvas`: node dragging now uses pointer events; `touch-action:
    none` on `.whiteboard-node` stops the browser treating a touch-drag as a
    page pan.
  - `TemplateBuilder`: both "drag a palette chip into a section" and "drag a
    field to reorder/move it between sections" now use pointer events with
    manual hit-testing (`document.elementFromPoint` + `data-drop-section`/
    `data-drop-field` markers) instead of native HTML5 DnD, which relied on
    the browser's own `dragover`/`drop` event firing over other elements.
- Rename the template builder's component tray class from `.palette` to
  `.field-palette` to remove the collision with the command palette.
- Give small stat-tracker/map-layer touch targets (`.pip`, `.layer-color`) a
  larger hit area under the existing `max-width: 680px` breakpoint, and
  reorder the maps view so the map itself precedes the map-list/layer sidebar
  when they stack on a narrow screen (previously the settings sidebar came
  first, pushing the map below the fold).

## Consequences
- Whiteboards and the sheet template builder are now usable on tablet and
  phone, not just desktop with a mouse.
- One interaction code path per gesture (pointer events) instead of separate
  mouse/touch implementations — less code to maintain, not more.
- The template builder's drop-target hit-testing is a small amount of manual
  DOM querying (`elementFromPoint`) that the native HTML5 DnD API used to do
  implicitly; this is the standard technique other pointer-based drag-and-drop
  implementations use and is a one-time cost, not a per-feature one — future
  drag interactions in the app should reuse the same pattern rather than
  reaching for HTML5 DnD again.

## Alternatives considered
- **Add `onTouchStart`/`onTouchMove`/`onTouchEnd` alongside the existing mouse
  handlers**: works, but doubles the event-handling code for every gesture
  and still leaves stylus/pen input unhandled. Pointer Events cover all three
  natively.
- **A drag-and-drop library (e.g. dnd-kit)**: reasonable for a larger app, but
  overkill for two call sites, and pulls in a dependency for something the
  app now does directly in ~30 lines.
