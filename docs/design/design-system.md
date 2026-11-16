# PetHaven Design System

| | |
|---|---|
| **Status** | v1.2, approved by the owner. Implementation in progress (`odd/tasks/ui-upgrade.md`) |
| **Scope** | Authentication and the Client, Veterinarian and Administrator portals. The landing page redesign is deferred (see 9.10) |
| **Stack** | Angular 19, PrimeNG 19 (Aura-based `PetHavenPreset`), Tailwind CSS v4, `tailwindcss-primeui` |
| **Direction** | "Bloques" (color blocks with geometry and sticker illustrations) |
| **UI language** | Spanish (es-CO). This document is in English; all UI copy examples are in Spanish |

This document is the single source of truth for PetHaven's visual language and interaction rules. When code and this document disagree, fix one of them in the same pull request.

Requirement words: **must** is mandatory, **should** is the default unless there is a written reason, **may** is optional.

## Contents

1. [Principles](#1-principles)
2. [Color](#2-color)
3. [Typography](#3-typography)
4. [Space, shape and elevation](#4-space-shape-and-elevation)
5. [Iconography](#5-iconography)
6. [Illustration](#6-illustration)
7. [Motion](#7-motion)
8. [Theming](#8-theming)
9. [Layout](#9-layout)
10. [Components](#10-components)
11. [Patterns](#11-patterns)
12. [Content and formatting](#12-content-and-formatting)
13. [Accessibility](#13-accessibility)
14. [Implementation](#14-implementation)
15. [Governance](#15-governance)
16. [Decision log and open items](#16-decision-log-and-open-items)

---

## 1. Principles

1. **Color carries meaning.** Saturated color appears at surface scale in color blocks, and each hue has a job: resource identity, status, or the primary action. Neutral surfaces carry everything else.
2. **Dense, with nothing wasted.** Screens fill the viewport. The contextual sidebar, the 12-column grid and the detail panel exist so that a user rarely needs to navigate away to answer a question.
3. **One sticker makes the page.** Each screen gets at most one large illustration. Stickers add personality to a block that already has a purpose; they never fill empty space.
4. **Flat and outlined.** Depth comes from a 1px border and color contrast. Shadows are reserved for surfaces that float above the page.
5. **Same resource, same color.** A pet, drug, veterinarian or owner keeps its hue everywhere it appears.
6. **State is always designed.** Every data view ships with loading (skeleton), empty, error and populated states.
7. **Use the platform.** Components are PrimeNG components themed through the preset. Custom components are built only when PrimeNG has no equivalent.

---

## 2. Color

### 2.1 Token architecture

Color is defined in three layers. Components must reference semantic tokens only.

| Layer | Purpose | Example |
|---|---|---|
| Primitive | Raw palette values | `olive-700`, `hue-honey` |
| Semantic | Role in the UI, switches with the theme | `surface`, `ink-muted`, `accent` |
| Component | Optional per-component alias | `tabs-active-bg` |

### 2.2 Neutrals and accent (semantic tokens)

| Token | Light | Dark | Use |
|---|---|---|---|
| `ground` | `#efe8da` | `#0e0e0d` | Application background |
| `surface` | `#fffaf0` | `#1a1a18` | Cards, top bar, inputs, dialogs |
| `surface-sunken` | `#f5ecdc` | `#252522` | Tab container, segmented control track, hover rows, skeleton blocks |
| `ink` | `#261a10` | `#f3efe6` | Primary text and icons |
| `ink-muted` | `#6b5e4e` | `#a59e92` | Secondary text, placeholders, axis labels |
| `border` | `#ddd0b9` | `#32312d` | Card outlines and dividers (decorative) |
| `border-strong` | `#857760` | `#757368` | Boundaries of interactive controls (inputs, checkboxes, outlined buttons) |
| `accent` | `#73704b` | `#cbc795` | Primary action, active state, progress, focus ring |
| `accent-hover` | `#5c5a3b` | `#dedaa9` | Hover and pressed state of `accent` |
| `on-accent` | `#fffaf0` | `#1c1b10` | Text and icons on `accent` |
| `accent-soft` | `#e5e2c9` | `#33321f` | Selected row, selected option, soft accent backgrounds |
| `accent-text` | `#5c5a3b` | `#cbc795` | Accent-colored text and text buttons |
| `overlay` | `rgb(20 14 8 / 0.5)` | `rgb(0 0 0 / 0.6)` | Scrim behind dialogs and drawers |

Rules:

- There is one accent: olive. No other hue may be used for primary actions, active navigation or progress.
- There is one neutral family: warm in light theme, true neutral in dark theme. Cool grays must not be introduced.
- `accent` must not be used as text color on `ground` in light theme (4.15:1). Use `accent-text`.

### 2.3 Hues (color blocks and identity)

Six hues, identical in both themes.

| Token | Value | Name in UI docs |
|---|---|---|
| `hue-honey` | `#eec56f` | Honey |
| `hue-sage` | `#a9c193` | Sage |
| `hue-sky` | `#a3c5cf` | Sky |
| `hue-rose` | `#e9b9b3` | Rose |
| `hue-lilac` | `#c6b5e2` | Lilac |
| `hue-peach` | `#ecab8c` | Peach |
| `on-hue` | `#261a10` (light), `#1b130b` (dark) | Text and icons on any hue |

Derived values, computed with `color-mix()` so they never need new tokens:

| Derived role | Formula |
|---|---|
| Geometry highlight | `color-mix(in srgb, #fff 34%, var(--hue))` |
| Geometry shade | `color-mix(in srgb, var(--on-hue) 16%, var(--hue))` |
| Muted text on hue | `color-mix(in srgb, var(--on-hue) 72%, var(--hue))` |
| Divider on hue | `color-mix(in srgb, var(--on-hue) 18%, var(--hue))` |
| Tag or track on hue | `color-mix(in srgb, var(--on-hue) 12%, var(--hue))` |

Controls placed on a hue invert to ink: a primary button on a color block uses `on-hue` as background and the hue as text color; a secondary button uses a 1.5px `on-hue` outline. The olive accent must not be placed on a hue.

### 2.4 Identity color

Pets, drugs, veterinarians and owners each have a permanent identity hue derived from their ID.

```ts
export const IDENTITY_HUES = ['honey', 'sage', 'sky', 'rose', 'lilac', 'peach'] as const;

export function identityHue(id: number | string): (typeof IDENTITY_HUES)[number] {
  const n = typeof id === 'number'
    ? Math.abs(Math.trunc(id))
    : [...id].reduce((sum, ch) => sum + ch.codePointAt(0)!, 0);
  return IDENTITY_HUES[n % IDENTITY_HUES.length];
}
```

Rules:

- The order of `IDENTITY_HUES` is frozen. Reordering or removing an entry changes the color of every existing resource. New hues may only be appended, and appending also reassigns colors, so it requires an explicit decision.
- The identity hue is applied to the resource's avatar background, the header block of its detail panel and detail page, its card header in card view, and its appointment blocks in the agenda (appointments take the pet's hue).
- Identity color is a recognition aid and must never be the only identifier. The name is always visible next to it.
- Identity color must not encode status. A drug that is out of stock keeps its hue; the status is shown with a tag and the stock meter.
- Six hues means collisions are expected and acceptable.

### 2.5 Status colors

| Status | Base | Soft bg (light) | Soft text (light) | Soft bg (dark) | Soft text (dark) | Meaning |
|---|---|---|---|---|---|---|
| `success` | `#6f9455` | `#e2e6d1` | `#55693d` | `#2d3525` | `#a4b88f` | Up to date, active, recovered, in stock |
| `warning` | `#d1922a` | `#f6e5c8` | `#845c1e` | `#42341c` | `#dfb775` | Low stock, expiring soon, in treatment |
| `danger` | `#c9573c` | `#f4d9cc` | `#90422d` | `#412720` | `#da9480` | Out of stock, overdue, error, destructive |
| `info` | `#5f8794` | `#dfe3de` | `#4b6166` | `#293233` | `#9ab1b5` | Improving, scheduled, informational |
| `neutral` | n/a | `surface-sunken` | `ink-muted` | `surface-sunken` | `ink-muted` | No status, metadata |

- Base values are for fills without text: meter bars, chart marks, dots, icons.
- Text on a status must use the soft pair. White text on the base fills fails contrast for `success`, `warning` and `info`.
- The solid destructive button uses `danger-solid` `#b84a30` with `#ffffff` text (5.17:1). In dark theme it uses `#e08a73` with `#1b130b` text.
- Status is never communicated by color alone: every status has a text label, and warnings and errors also carry an icon.

### 2.6 Data visualization colors

| Order | Series color | Notes |
|---|---|---|
| 1 | `accent` | Primary series |
| 2 | `#d1922a` | Secondary series |
| 3 | `#5f8794` | |
| 4 | `#b98580` | Deepened rose |
| 5 | `#8e78bd` | Deepened lilac |
| 6 | `#7d9a66` | Deepened sage |

- Charts should use at most four series; beyond that, group into "Otros".
- Grid lines use `border`; axis labels and legends use `ink-muted` at 12px.
- A chart that shows a single resource may use that resource's identity hue for its mark, with `on-hue` at 25% mix as stroke.
- Series 4 to 6 have not been contrast-tested against both surfaces; validate before first use (see open items).

### 2.7 Measured contrast

Ratios computed with the WCAG 2.x formula for the values above.

| Pair | Light | Dark |
|---|---|---|
| `ink` on `surface` | 16.31 | 15.19 |
| `ink-muted` on `surface` | 6.05 | 6.56 |
| `ink-muted` on `ground` | 5.16 | 7.27 |
| `on-accent` on `accent` | 4.87 | 10.01 |
| `accent-text` on `ground` | 5.78 | 11.17 |
| `accent-text` on `accent-soft` | 5.39 | 7.51 |
| `on-hue` on hues (lowest: sage) | 8.67 | 9.38 |
| Muted text on hues (lowest: peach) | 4.65 | not measured |
| `border-strong` on `surface` / `ground` | above 3.5 / 3.58 | 3.66 / above 3.5 |
| Soft status text on soft bg (lowest: success) | 4.74 | 5.55 |

---

## 3. Typography

### 3.1 Families

| Role | Family | Fallback | Use |
|---|---|---|---|
| Display | Fraunces (variable, optical size 9 to 144) | Georgia, serif | Page titles, card titles, resource names, large figures |
| Text | Nunito Sans (variable, optical size 6 to 12) | system-ui, sans-serif | Everything else: body, labels, tables, controls |
| Script | Caveat 600 | cursive | Handwritten accents only |

Rules:

- Caveat appears at most once per screen and only for human, informal content: a greeting kicker, a veterinarian's quoted note, a tip. It must not be used for labels, data, navigation or anything below 20px.
- Emphasis stays within the family: use weight, never a second typeface inside a sentence.
- Body text is never set in Fraunces.

### 3.2 Type scale

| Token | Family | Size / line height | Weight | Use |
|---|---|---|---|---|
| `display-xl` | Fraunces | 48 / 52 | 600 | Landing hero |
| `display-lg` | Fraunces | 36 / 40 | 600 | Landing section titles, auth title |
| `title-1` | Fraunces | 28 / 32 | 600 | Page title, hero block headline |
| `title-2` | Fraunces | 20 / 26 | 600 | Resource name in panels, dialog title |
| `title-3` | Fraunces | 17 / 22 | 600 | Card title |
| `figure` | Fraunces | 28 / 28 | 600 | KPI values and key numbers |
| `body-lg` | Nunito Sans | 16 / 24 | 500 | Landing paragraphs, long-form text |
| `body` | Nunito Sans | 14 / 20 | 500 | Default UI text |
| `body-strong` | Nunito Sans | 14 / 20 | 800 | Row titles, emphasized values |
| `label` | Nunito Sans | 13 / 18 | 700 | Field labels, buttons, tabs, chips |
| `caption` | Nunito Sans | 12.5 / 18 | 500 | Secondary lines, helper text |
| `micro` | Nunito Sans | 12 / 16 | 800 | Tags, chart axis labels |
| `script` | Caveat | 21 / 24 | 600 | Handwritten accents |

Rules:

- 12px is the minimum size anywhere.
- `title-1` in hero blocks may scale fluidly: `clamp(21px, 2.3cqi, 29px)`.
- Letter spacing: `-0.01em` on `title-1` and larger; default elsewhere.
- Numbers in tables, meters and figures use `font-variant-numeric: tabular-nums`.
- Headlines use `text-wrap: balance`; paragraphs cap at 65 characters.
- Uppercase text with wide tracking is not part of this system.
- Text alignment: left by default; numeric table columns right; centered text only in empty states and picker tiles.

---

## 4. Space, shape and elevation

### 4.1 Spacing scale

Base unit 4px. Use only these values.

| Token | px | Typical use |
|---|---|---|
| `space-1` | 4 | Icon to label gap inside tags |
| `space-2` | 8 | Gap between inline controls |
| `space-3` | 12 | Gap inside cards between groups |
| `space-3.5` | 14 | Grid gutter (card to card) |
| `space-4` | 16 | Card padding |
| `space-5` | 20 | Hero block padding, dialog padding |
| `space-6` | 24 | Section spacing inside a page |
| `space-8` | 32 | Landing block spacing (small) |
| `space-12` | 48 | Landing section spacing |
| `space-16` | 64 | Landing section spacing (large) |

### 4.2 Radius scale

| Token | px | Applies to |
|---|---|---|
| `radius-xs` | 7 | Checkbox, small swatches |
| `radius-sm` | 10 | Text inputs, select, textarea, calendar day cell |
| `radius-md` | 12 | Buttons, icon buttons, chips, segmented control, quantity stepper |
| `radius-lg` | 14 | List rows, appointment blocks, alerts, time slots |
| `radius-xl` | 20 | Cards, color blocks, dialogs, picker tiles |
| `radius-full` | 999 | Tags, avatars, toggle switch, meters, primary navigation tabs, toast |

Shape rule: actions are 12, containers are 20, fields are 10, labels and people are round. The primary navigation tabs are the single pill-shaped control group; no other button-like control may be a pill.

### 4.3 Borders

| Use | Spec |
|---|---|
| Cards, dialogs, top bar bottom edge, dividers | 1px `border` |
| Inputs, select, checkbox, radio, outlined button, quantity stepper | 1.5px `border-strong` |
| Selected picker tile | 1.5px `accent` plus `inset 0 0 0 1px accent` |
| Selected appointment block | `0 0 0 2.5px ink` ring |
| Color blocks | No border |
| Empty time slot | 1.5px dashed `border-strong` |

### 4.4 Elevation

The interface is flat. Only floating surfaces cast a shadow.

| Level | Shadow | Used by |
|---|---|---|
| 0 | none | Cards, color blocks, tables, top bar, sidebar |
| 1 | `0 1px 2px rgb(0 0 0 / 0.14)` | Active primary tab, active segmented option |
| 2 | `0 12px 30px -10px rgb(0 0 0 / 0.35)` | Popovers, select panel, date picker panel, toast |
| 3 | `0 24px 60px -20px rgb(0 0 0 / 0.45)` | Dialog, drawer |

Drop shadows on cards and bottom-edge "volume" shadows are not part of this system.

### 4.5 Scrollbars

Native scrollbars are replaced with a custom style so they match the interface in both themes.

| Part | Spec |
|---|---|
| Size | 10px wide (vertical) and tall (horizontal), with a 3px transparent inset so the visible thumb is 4px |
| Track | Transparent |
| Thumb | `border-strong` at 60% opacity, fully rounded |
| Thumb, hover | `ink-muted` |
| Corner | Transparent |

- Implemented with `scrollbar-width: thin` and `scrollbar-color` (standard), plus `::-webkit-scrollbar` rules for the finer shape.
- Applies to every scrolling region of authentication and the portals: content region, contextual sidebar, detail panel, tables, menus, dialogs.
- Scoped by the `ph-scrollbars` class on `<html>`, set while authentication or a portal is on screen, so the landing page keeps the native scrollbar.
- Scrollbars are never hidden on regions that scroll; the user must be able to see that more content exists.

### 4.6 Control sizes

| Size | Height | Use |
|---|---|---|
| `sm` | 30px | Inline actions inside cards and rows |
| `md` | 38px | Default for buttons, inputs, select |
| `lg` | 44px | Landing calls to action, auth forms, all controls below 680px width |

Minimum pointer target is 24 by 24px on desktop and 44 by 44px on touch layouts.

---

## 5. Iconography

- Library: **Lucide** via `@lucide/angular`. PrimeIcons remains only for icons rendered internally by PrimeNG components and must not be used in application templates.
- Style: outline, `stroke-width: 1.8`, round caps and joins, `currentColor`.
- Sizes: 18px default, 16px in `sm` controls, 13px inside tags, 24px in empty headers. No other sizes.
- An icon next to a label is decorative (`aria-hidden="true"`). An icon-only button must have an `aria-label` and a tooltip.
- Emoji must not be used as icons.

Canonical mapping (extend this table when adding a concept):

| Concept | Lucide icon | | Concept | Lucide icon |
|---|---|---|---|---|
| Home | `house` | | Search | `search` |
| Pets, patients | `paw-print` | | Notifications | `bell` |
| Agenda, date | `calendar` | | Add | `plus` |
| Time | `clock` | | Edit | `pencil` |
| Treatment, drug | `pill` | | Delete | `trash-2` |
| Inventory | `package` | | Download, export | `download` |
| Vaccine | `syringe` | | Close | `x` |
| Consultation | `stethoscope` | | Sort | `arrow-up-down` |
| People | `users` | | Warning | `triangle-alert` |
| Dashboard | `chart-column` | | Confirmed | `check` |
| Document, PDF | `file-text` | | Light / dark theme | `sun` / `moon` |
| Card view / table view | `layout-grid` / `table` | | Open full detail | `arrow-up-right` |
| Sidebar toggle | `panel-left` | | Sign out | `log-out` |

---

## 6. Illustration

### 6.1 Sticker style

The approved style is "sticker": a character or object drawn with flat fills, a dark outline, and a light halo that separates it from any background.

| Property | Spec |
|---|---|
| Outline | `#261a10`, 3 units on a 120-unit canvas, round joins and caps, both themes |
| Halo | `#fffaf0`, 5 units beyond the outline, both themes |
| Fills | Flat. No gradients, textures or inner shadows |
| Shading | At most one darker patch color per character |
| Faces | Dot eyes, simple nose and mouth, front-facing or side profile |
| Format | Inline SVG `<symbol>`, colored through CSS custom properties |

Characters are recolored with four variables so one drawing serves many coats:

| Variable | Role |
|---|---|
| `--c1` | Main coat |
| `--c2` | Ears, patches, stripes |
| `--c3` | Muzzle, belly, paws |
| `--c4` | Collar |

Approved coat kits:

| Kit | `--c1` | `--c2` | `--c3` | `--c4` |
|---|---|---|---|---|
| Tan | `#e9b872` | `#b0743c` | `#fff1d8` | `#73704b` |
| White-gray | `#f1ede6` | `#9a958d` | `#ffffff` | `#c96f4a` |
| Red | `#c9824a` | `#84502a` | `#f3d6b0` | `#5f8794` |
| Dark brown | `#5b4a3c` | `#33271e` | `#e9d5b8` | `#e8b04b` |
| Orange | `#eaa25a` | `#b9682a` | `#fff1d8` | `#73704b` |
| Cream | `#fff6e2` | `#d9a15c` | `#ffffff` | `#c96f4a` |

When a pet has no coat information, the kit is chosen with `id mod 6` so it stays stable.

### 6.2 Library

| Group | Items |
|---|---|
| Characters | Dog (sitting, front), Cat (sitting, front), Long dog (side profile) |
| Objects | Bone, Pill bottle, Syringe, Calendar, Paw, Heart, Food bowl, Shield, Star |

The drawings are approved in style. The implementation includes a final art pass on proportions and details, and extends the object set with veterinary-themed stickers for the daily tips (for example: stethoscope, thermometer, bandage, shampoo bottle, toothbrush, leash, water bowl, carrier, pet bed, treat). New illustrations must follow 6.1 and be added to this table.

### 6.3 Pet photo and sticker

- If a pet has a photo, the avatar and the detail header show the photo, cropped to a circle with `object-fit: cover`, on top of the pet's identity hue with a 2px `surface` ring.
- If there is no photo or it fails to load, show the species sticker (head crop in avatars, full body in headers) on the identity hue.
- Stickers of species must match the pet's species. Never show a dog sticker for a cat.

### 6.4 Usage rules

| Context | Size | Rule |
|---|---|---|
| Hero block | 180 to 290px wide | One per screen. Overflows the top edge of the block by 16 to 32px and keeps at least 12px of clear space between its halo and the bottom edge. The halo must never touch or be cut by the bottom edge |
| Resource card header | 96 to 130px | Centered on the identity hue |
| KPI block | 46 to 56px object | Bottom-right corner, never overlapping the figure |
| Empty state | 80 to 130px | Above the title |
| Avatar | 32, 42, 60px | Head crop |
| Picker tile | 46px object | Above the label |

- Stickers must not be placed on `ground` without a color block or card behind them, except in empty states and on the landing page.
- Stickers are decorative and take `aria-hidden="true"`. Meaning is always in the text.
- At most one hero-scale sticker and a handful of small objects per screen.

### 6.5 Block geometry

Color blocks carry flat geometric shapes behind their content. This is the signature of the system.

| Shape | Spec | Fill |
|---|---|---|
| Circle | Full circle, partially cropped by the block edge | Geometry highlight |
| Quarter circle | `border-radius: 100% 0 0 0`, anchored to the bottom-right corner | Geometry shade |
| Triangle | Isosceles, anchored to the bottom edge | Geometry highlight at lower strength (26%) |

| Block type | Shapes | Sizes |
|---|---|---|
| Hero | Circle, quarter circle, triangle (up to 3) | 210px circle, 170px quarter, 88 by 70px triangle |
| KPI | Circle, quarter circle | 96px circle, 70px quarter |
| Compact (sidebar, card header) | One shape | 80 to 90px |

Rules:

- Shapes sit in a clipped layer (`overflow: hidden`, inherited radius) below the content; only the sticker may overflow the block.
- Shapes never sit behind body text at a size that reduces contrast below 4.5:1. Keep them on the trailing half of the block.
- Shapes do not animate.
- Organic blobs, gradients and wave dividers are not used in the portals or authentication screens. The landing page keeps them until its redesign (see 9.10).

---

## 7. Motion

Motion is functional and sparse. If an animation has no one-word purpose (feedback, continuity, state change), it is removed.

| Token | Value | Use |
|---|---|---|
| `duration-fast` | 120ms | Press feedback, toggles |
| `duration-base` | 180ms | Hover, color transitions, fade |
| `duration-slow` | 250ms | Dialog, drawer, toast entrance |
| `ease-out` | `cubic-bezier(0.23, 1, 0.32, 1)` | All entrances and state changes |

- Press feedback: `scale(0.97)` on buttons and picker tiles; background highlight (no scale) on rows.
- Overlays enter from `opacity: 0` and `scale(0.96)` or an 10px offset, and exit faster than they enter.
- Tabs, view switches and theme changes do not animate content.
- Charts render without entrance animation. Data the user is reading never moves for style.
- The sign-in illustration block is the single ambient animation in the product (see 9.11). Its elements drift no more than 12px over 8 to 12 seconds with ease-in-out, and compositions cross-fade over 600ms every 12 to 16 seconds. It pauses when the tab is hidden.
- Skeletons pulse opacity between 1 and 0.45 over 1.1s.
- `prefers-reduced-motion: reduce` collapses all transitions and animations to instant, including the skeleton pulse.

---

## 8. Theming

- Two themes: light and dark. Both are first-class; every component and screen must be verified in both.
- First visit follows `prefers-color-scheme`. The theme button in the top bar toggles between light and dark, and the choice is stored in `localStorage` under `pethaven.theme` (`light` or `dark`). A stored choice overrides the system preference.
- The theme is applied as the `app-dark` class on `<html>` before first paint (inline script in `index.html`) to avoid a flash of the wrong theme. It is the class the project already configured as PrimeNG's `darkModeSelector`.
- Dark theme is true neutral black, not tinted. Hues are the same values as in light theme and provide the color.
- In dark theme, borders do more work than in light theme because surfaces are close in value; never remove the card border.
- Photos and stickers are not altered by the theme.
- Authentication screens follow the same theme rules. The landing page is out of scope until its redesign.

---

## 9. Layout

### 9.1 Breakpoints

Layout responds to the width of the application container (container queries), so embedded and split views behave correctly.

| Name | Container width | Behavior |
|---|---|---|
| `compact` | below 680px | Single column, bottom tab bar, `lg` controls |
| `medium` | 680 to 1079px | Two-column grid, sidebar as drawer |
| `expanded` | 1080px and above | Full 12-column grid, sidebar visible |

### 9.2 Application shell

```
+----------------------------------------------------------------------+
| Logo   [ Tab | Tab | Tab ]                 Search  Bell  Theme  User |  top bar, 60px
+-------------+--------------------------------------------------------+
| Contextual  |  Page header: title, subtitle, actions                  |
| sidebar     |  ------------------------------------------------------ |
| 236px       |  Content grid, 12 columns, 14px gutter                  |
|             |                                                        |
+-------------+--------------------------------------------------------+
```

- The shell fills the viewport height and never scrolls as a whole. It has three independent regions:
  - The top bar is fixed at the top at all times.
  - The contextual sidebar is fixed in place. If its content is taller than the viewport, the sidebar scrolls on its own.
  - The content region scrolls independently of both.
- Content uses the full available width. There is no centered max-width column in the portals.
- Page padding: 18px horizontal, 16px top, 22px bottom (12px on `compact`).

### 9.3 Top bar

- Background `surface`, 1px `border` bottom edge, 60px tall.
- Left: logo (paw mark in an `accent` circle plus the "PetHaven" wordmark in Fraunces 700 19px).
- Center-left: primary navigation tabs.
- Right, in order: global search (220px), notifications, theme toggle, user menu (avatar with initials, name, role).
- On `medium`, search collapses to an icon button and the user menu shows the avatar only.

### 9.4 Primary navigation tabs

- A pill container (`surface-sunken`, 4px padding, `radius-full`) holding one tab per section of the current role.
- Tab: icon 18px plus label, `label` type, `ink-muted`, 8px by 14px padding, `radius-full`.
- Active tab: `surface` background, `ink` text, elevation level 1. Hover: `ink` text.
- Up to seven tabs per role. Every role ends with the same two tabs, in this order: "Notificaciones", then "Perfil". An eighth section would move into a "Más" menu placed before those two.
- When the tabs do not fit, the top bar gives up space in this order: the user name and role text collapse to the avatar (container below 1340px), then the search field narrows, then the tab group scrolls horizontally.
- The active tab reflects the top-level section even on nested routes (for example the pet detail page keeps "Pacientes" active).
- Keyboard: arrow keys move between tabs, Enter activates. Implemented as navigation links with `aria-current="page"`.

### 9.5 Contextual sidebar

The left sidebar shows information and shortcuts related to the current section, so the main area can stay dense. It is not navigation.

- Width 236px, padding 4px 0 22px 18px, 12px gap between modules. No background of its own; modules are cards and compact color blocks.
- Collapsible with the `panel-left` button in the page header. The state is stored in `localStorage` under `pethaven.sidebar` and applies to all sections.
- On `medium` and `compact` it is closed by default and opens as a drawer from the left over a scrim.
- Content rules:
  - At most three contextual modules: one compact color block (key figure or primary call to action) and up to two cards.
  - A "Notificaciones" card is always the last module, for every role. It shows the three most recent notifications with icon, title, one line of detail and relative time, and ends with a "Mostrar todo" text button that opens the Notificaciones tab. It is hidden on the Notificaciones screen itself.
  - Content must change with the section. It must not repeat a module that is visible in the main area of the same screen.
  - Every item is a shortcut: selecting a pet, opening a waiting patient, jumping to a low-stock drug.
  - If a section has nothing contextual to show, the sidebar is omitted and the content takes the full width.

Default content per role (to be refined per screen):

| Role | Color block | Cards |
|---|---|---|
| Client | Next appointment or "Agendar cita" | Pet switcher (hidden on Inicio), reminders, clinic info |
| Veterinarian | Today's appointment count | Waiting room, "Nueva consulta" action |
| Administrator | Month-to-date revenue against target | Inventory alerts |

### 9.6 Page header

- Left: `title-1` page title and a one-line `caption` subtitle (context, counts, date).
- Right: page-level actions. One primary button at most; secondary actions are outlined; overflow goes in a menu.
- Detail pages add a back link above the title: "‹ Pacientes".

### 9.7 Content grid

- 12 columns, 14px gutter, `align-items: start` (use stretch only when a row of blocks should share a height).
- Spans collapse by breakpoint:

| Span on `expanded` | `medium` | `compact` |
|---|---|---|
| 3, 4, 5 | 6 | 12 |
| 6 | 6 | 12 |
| 7, 8, 9, 12 | 12 | 12 |

- Recommended compositions: 8 + 4 (hero plus summary), 4 + 5 + 3, 3 + 5 + 4 (calendar, timeline, detail), 8 + 4 (table plus detail panel), 4 by 3 (KPI row), 5 + 7 (form plus builder).
- A row should not leave an empty trailing column on `expanded`. Rebalance spans instead.

### 9.8 Density

- One density. Row height 44px minimum in tables, 9 to 10px vertical padding in list rows, 16px card padding.
- A screen on a 1440 by 900 viewport should show its primary content without scrolling.

### 9.9 Compact layout (mobile)

- Primary navigation becomes a fixed bottom tab bar: `surface` background, 1px `border` top edge, one item per section with a 20px icon above a 12px label, 56px tall plus the safe-area inset. Active item uses an `accent` pill behind icon and label with `on-accent` content. The bar shows at most five items: the first four sections and a "Más" item that opens the remaining ones (including Notificaciones and Perfil) in a bottom sheet.
- The top bar keeps the logo mark, the sidebar button and the user avatar. Search and notifications move into the user menu.
- All controls use the `lg` size. Tables switch to card view (see 11.1).
- Hero stickers are hidden; geometry stays.
- Page content must not scroll horizontally. Wide tables scroll inside their own container.

### 9.10 Landing page (deferred)

The landing page redesign is pending and is out of scope for this version.

- The current landing keeps its existing design, including its blobs, wave dividers, marquee and paw-trail animations. None of them are retired.
- Until the redesign, the landing keeps the legacy tokens it depends on (see 14.1). Portal and authentication code must not use those legacy tokens.
- When the redesign starts, this section is replaced with the rules for marketing surfaces. The `display-xl` and `display-lg` type tokens and the `space-12` and `space-16` spacing tokens are reserved for it.

### 9.11 Authentication

- Two-panel layout on `expanded`: a form (max 440px) on `ground` and a large color block. On `compact`, only the form.
- The color block is richer than portal blocks. A composition has five to seven geometric shapes of varied kinds (circle, quarter circle, triangle, ring, rounded square), two characters and three to four small object stickers placed around them at slight rotations (up to 18 degrees). Nothing may sit behind or closer than 24px to the brand line and benefit list in the bottom-left corner.
- The block cycles through at least three compositions that use different stickers from the library, with the slow drift and cross-fade defined in section 7. With `prefers-reduced-motion: reduce` it shows one static composition.
- Controls use `lg` size. One primary button per screen.

---

## 10. Components

Each entry lists the PrimeNG component to theme. "Custom" means it is built in the project's shared components folder.

### 10.1 Button — `p-button`

| Variant | Spec | Use |
|---|---|---|
| Primary | `accent` bg, `on-accent` text | The main action of a page or dialog. One per view |
| Secondary | Transparent bg, 1.5px `border-strong`, `ink` text | Alternative actions |
| Text | Transparent, `accent-text` text | Low-emphasis actions inside cards |
| Danger | `danger-solid` bg, white text | Destructive confirmation only |
| On-hue | See 2.3 | Any button placed on a color block |

- Shape `radius-md`, `label` type at weight 800, 7px icon gap, sizes per 4.5.
- States: hover (`accent-hover` or `surface-sunken`), pressed (`scale(0.97)`), focus-visible (2.5px `accent` outline, 2px offset), disabled (45% opacity, no pointer events), loading (label stays, leading spinner replaces the icon, button is disabled).
- Labels are verb-first and specific: "Guardar tratamiento", not "Aceptar".
- Icon-only button (`p-button` icon mode): 36px square, `radius-md`, 1px `border`, `surface` bg. A notification dot is an 8px `danger` circle with a 2px `surface` ring.

### 10.2 Text fields — `pInputText`, `p-textarea`, `p-inputnumber`, `p-select`, `p-datepicker`, `p-autocomplete`

- Anatomy: label (`label`, above the field), control, helper or error text (`caption`, below).
- Control: `surface` bg, 1.5px `border-strong`, `radius-sm`, 38px tall, 11px horizontal padding. Textarea minimum 84px, vertical resize only.
- Placeholder: `ink-muted`. Placeholders are examples, never labels.
- States: hover (border `ink-muted`), focus (2.5px `accent` outline), invalid (border `danger`, error text in soft danger text color with the reason and how to fix it), disabled (`surface-sunken` bg, 60% opacity), read-only (no border, `surface-sunken` bg).
- Search field: leading `search` icon, same shape as other inputs, optional clear button.
- Required fields are marked with "(obligatorio)" only when a form mixes required and optional fields; otherwise mark the optional ones with "(opcional)".

### 10.3 Selection controls

| Control | PrimeNG | Spec |
|---|---|---|
| Checkbox | `p-checkbox` | 22px, `radius-xs`, 1.5px `border-strong`; checked: `accent` bg, `on-accent` check |
| Radio | `p-radiobutton` | 22px circle; selected: `accent` bg with 10px `on-accent` dot |
| Toggle | `p-toggleswitch` | 40 by 23px track, 17px thumb; on: `accent` |
| Segmented control | `p-selectbutton` | `surface-sunken` track, 3px padding, `radius-md`; option `radius-sm`; selected: `surface` bg, elevation 1 |
| Filter chip | `p-togglebutton` | 1.5px `border`, `radius-md`, `ink-muted`; selected: `ink` bg, `ground` text |
| Quantity stepper | `p-inputnumber` (buttons) | 28px buttons, value centered, 1.5px `border-strong`, `radius-md` |

- Use a segmented control for 2 to 4 mutually exclusive views of the same data (period, chart range, card or table view). Use filter chips for filters that narrow a list. Use a select beyond four options.
- Toggles apply immediately; checkboxes are for forms that are submitted.

### 10.4 Tag — `p-tag`

- Pill (`radius-full`), `micro` type, 3px by 10px padding, optional 13px leading icon.
- Variants: `neutral`, `success`, `warning`, `danger`, `info` using the soft pairs in 2.5. On a hue: see 2.3.
- Tags are read-only. They are never clickable.
- One status vocabulary per resource (see 12.3).

### 10.5 Card — custom (`ph-card`)

- `surface` bg, 1px `border`, `radius-xl`, 16px padding, no shadow.
- Optional header: `title-3` title on the left, one action on the right (text button, tag or segmented control), 12px below.
- Cards do not nest. Group content inside a card with dividers (1px `border`, 12px margin) or spacing.
- Clickable cards have a hover state (`surface-sunken`) and a single destination.

### 10.6 Color block — custom (`ph-block`)

- A hue background with geometry (6.5), `radius-xl`, 16px padding (20 by 22px for hero), no border.
- All content uses `on-hue` and the derived values in 2.3.
- Types:

| Type | Content | Min height |
|---|---|---|
| Hero | Kicker, `title-1` headline, one sentence, up to two buttons, one sticker | 196px |
| KPI | Label, `figure`, trend line, small object sticker | 128px |
| Summary | Label, key value or short list, optional action | content |
| Header | Resource sticker or photo plus name, used at the top of panels and cards | 118px |

- A screen should have one hero at most, and between two and six blocks overall. If everything is a color block, nothing stands out.
- The hue of a block is either the identity hue of the resource it describes or, for non-resource blocks, a fixed hue assigned per block type within a screen. Adjacent blocks must use different hues.
- KPI blocks must contain real information density: value, comparison against the previous period with direction, and where possible a sparkline or progress bar. A block with a single number and a label is not acceptable.

### 10.7 Avatar — custom (`ph-avatar`)

- Sizes 32, 42, 60px, circle, identity hue background.
- Pets: photo or sticker head (6.3). People: initials in `on-hue`, weight 800 (Fraunces 600 at 60px).
- Built as a custom component rather than `p-avatar`: it needs the identity hue, the three sizes, and a fallback from a failed photo to the sticker or initials.
- Avatar group: overlap of 6px with a 2px `surface` ring, maximum five, then "+N".

### 10.8 List row — custom (`ph-list-item`)

- Leading avatar or icon, title (`body-strong`) and secondary line (`caption`, `ink-muted`), trailing tag, value or action.
- 9 by 10px padding, `radius-lg`, 2px gap between rows. Hover `surface-sunken`; selected `accent-soft`.
- If the whole row is interactive it is one button or link; it must not also contain other buttons.

### 10.9 Data table — `p-table`

- Header: `caption` at weight 700, `ink-muted`, 1px `border` bottom, no background, no uppercase.
- Rows: 10px padding, 1px `border` between rows, no zebra striping. Hover `surface-sunken`; selected `accent-soft` with `aria-selected="true"`.
- First column is the resource identity: avatar, name (`body-strong`) and a secondary line.
- Numeric columns are right-aligned with tabular numerals. Status is a tag. Quantities with a threshold use the stock meter.
- Sorting: sortable headers are buttons with the `arrow-up-down` icon, replaced by a direction chevron when active; `aria-sort` is set.
- Row actions: clicking a row selects it and updates the detail panel. Row-level actions live in the detail panel, not in a trailing actions column, except for a single icon button where an action is very frequent.
- The table lives inside a card and scrolls horizontally inside it when needed.

### 10.10 Pagination — `p-paginator`

- Placed at the bottom of the list card: range text on the left ("1 a 12 de 86"), previous and next icon buttons with the current page between them on the right, and a page-size select (12, 24, 48).
- Same component for table view and card view. Page size 12 by default in both.
- Changing filters or sort returns to page 1. The current page is kept in the URL query string.

### 10.11 View switcher — `p-selectbutton`

- A two-option icon segmented control (`layout-grid`, `table`) in the list toolbar, with `aria-label`s "Vista de tarjetas" and "Vista de tabla".
- Default per resource: pets and veterinarians open in cards; drugs and owners open in table. The user's choice is stored per list in `localStorage` (`pethaven.view.<resource>`).
- Both views share the same filters, sort, pagination and selection behavior.

### 10.12 Resource card (card view) — custom

- Card with no padding: a header color block in the identity hue (118px) with the sticker or photo, then a 12 by 14px body with name (`title-3`), status tag, secondary line, one or two key facts and one action.
- Grid: span 3 on `expanded`, 6 on `medium`, 12 on `compact`.
- Clicking the card selects it and updates the detail panel, exactly like a table row.

### 10.13 Detail panel — custom (`ph-detail-panel`)

The right-hand panel that describes the selected row or card without leaving the list.

- Span 4 beside a span 8 list on `expanded`. It stays fixed at the top of the content region while the table or card grid scrolls underneath, so the detail is always visible. If the panel is taller than the region, it scrolls on its own.
- Anatomy, top to bottom:
  1. Header color block in the identity hue: avatar or sticker, name (`title-2`), secondary line.
  2. Key figure with its status tag and meter when relevant.
  3. Definition list of the most useful attributes (five to seven rows).
  4. Optional small chart or recent history.
  5. Actions: one primary, one secondary, and **always** an "Abrir ficha completa" button with the `arrow-up-right` icon that navigates to the resource's detail page.
- With nothing selected: an empty state that tells the user to pick an item. On first load the first row is selected.
- On `medium` and `compact` the panel opens as a drawer from the right when a row is selected.
- Required on every resource list: pets, owners, veterinarians, drugs and treatments.

### 10.14 Section tabs — `p-tabs`

- Underline style for switching sub-views inside a page: `label` type, 9 by 14px padding, 2.5px `accent` underline on the active tab, 1px `border` baseline.
- Not to be confused with primary navigation tabs (pill). Section tabs never appear in the top bar.

### 10.15 Stepper — `p-stepper`

- Horizontal: numbered 26px circles with labels, joined by 2px lines.
- States: upcoming (`surface-sunken` circle, `ink-muted` label), current (`accent` circle, `ink` label), done (`accent-soft` circle with a check, `accent` line).
- The footer has "Atrás" (secondary, left) and "Continuar" (primary, right). "Continuar" is disabled until the step is valid; the final step's label names the outcome ("Confirmar cita").
- A live summary beside the steps shows the choices made so far.

### 10.16 Timeline — `p-timeline` (customized)

Used for treatment history and activity.

- Vertical line (2px `border`) with a 12px marker per event; the marker uses the status color of the event.
- Each event: date (`caption`, weight 800), title (`body-strong`), one-line summary, status tag. Events are grouped under year or month headings (`title-3`).
- Events are selectable; selection updates the detail panel beside the timeline.
- Active or ongoing items are pinned to the top with a progress bar.

### 10.17 Calendar and day timeline — `p-datepicker` (inline) plus custom day view

- Month grid: 36px day cells, `radius-sm`, Monday first. Today has a 1.5px `accent` inset ring; selected is `accent` with `on-accent`; days with appointments show a 4px dot; closed days are `ink-muted` at 55%.
- Day timeline: one row per time slot with a 54px time column. An appointment is a color block in the pet's identity hue (`radius-lg`) with avatar, pet name, reason, owner and duration. A free slot is a dashed button that starts a new appointment.
- Urgent appointments carry a tag with the `triangle-alert` icon. Selected appointment: 2.5px `ink` ring.
- Selecting an appointment updates the visit detail panel.

### 10.18 Charts — `p-chart` (Chart.js)

- Types in use: line with area (trends over time), grouped bars (comparisons), bars (distribution). Pie and donut charts are not used; a ring is only for a single completion ratio.
- Every chart has: a card title that states what is shown, the unit, labelled axes with values on the value axis, a legend when there is more than one series, and a tooltip on hover and keyboard focus.
- Line: 3px stroke, 4.5px points (7px when selected), area fill `accent-soft`. Bars: 7px top radius, 2px gap inside a group.
- Colors per 2.6. Theme colors are read from CSS custom properties and re-applied when the theme changes.
- Each chart has a text alternative: an `aria-label` that summarizes the trend and, for key charts, a "Ver como tabla" toggle.

### 10.19 Meters

| Component | PrimeNG | Spec |
|---|---|---|
| Progress bar | `p-progressbar` | 8px, `radius-full`, `surface-sunken` track, `accent` fill; label beside it, not inside |
| Stock meter | custom (`ph-stock-meter`) | 8px bar; fill `success` at or above minimum, `warning` below, `danger` at zero; a 2 by 14px `ink` tick marks the minimum threshold; the numeric value is always shown next to it ("42 uds., mín. 20") |
| Ring | custom SVG | 76px, 9px stroke, `accent` on `surface-sunken`, value in Fraunces at the center ("4/5") |
| Sparkline | custom SVG | 2.2px stroke, `currentColor`, no axes; decorative, with the figure in text beside it |

### 10.20 Feedback

| Component | PrimeNG | Spec and use |
|---|---|---|
| Toast | `p-toast` | Bottom center, `ink` bg, `ground` text, `radius-full`, leading icon, elevation 2. Confirms a completed action. Auto-dismiss after 4s; errors stay until dismissed. One at a time |
| Inline alert | `p-message` | `radius-lg`, soft status background, leading icon, text, optional action. For conditions that affect the current view |
| Dialog | `p-dialog` | `surface`, `radius-xl`, 20px padding, max width 460px (forms up to 640px), `overlay` scrim. Title (`title-2`), close button, footer with secondary then primary on the right. For short, self-contained tasks |
| Confirm dialog | `p-confirmdialog` | For destructive or irreversible actions. Title states the action and the object ("¿Eliminar a Luna?"), body states the consequence, primary button is Danger and repeats the verb |
| Drawer | `p-drawer` | Sidebar and detail panel on smaller widths. 320 to 420px, elevation 3 |
| Tooltip | `pTooltip` | `ink` bg, `ground` text, `caption`, `radius-sm`. Required on icon-only buttons. Never holds essential information. Native browser tooltips (`title` attribute) are not used anywhere; every tooltip is a `pTooltip`, including multi-line ones on chart and schedule segments |
| Menu | `p-menu`, `p-popover` | `surface`, 1px `border`, `radius-lg`, elevation 2. Opens from its trigger |

Dialogs trap focus, close on Escape, and return focus to the trigger. A dialog that needs a second step should be a page or a stepper instead.

### 10.21 Skeleton — `p-skeleton`

- Skeletons replace spinners for all content loading.
- A skeleton keeps the exact outer shape of what it stands for (card, block, row), with `surface-sunken` bars for text lines: one at 42% width and 16px for the title, then lines of 11px.
- Color blocks load as `surface` cards with skeleton bars, not as colored blocks.
- The page chrome (top bar, tabs, page title, toolbar) renders immediately; only data regions are skeletons.
- Show the skeleton only if loading takes longer than 150ms, and keep it for at least 300ms to avoid flicker.
- A spinner is allowed only inside a button while its action is in flight.
- Skeletons come in a small fixed set of variants, chosen by the component they stand for. Do not add more than these:

| Variant | Stands for | Shape |
|---|---|---|
| `text` | Cards with a title and paragraphs | One 42% title bar and three to four text lines |
| `row` | Table rows and list rows | A 32px circle, two stacked lines, and a short trailing pill |
| `media` | Resource cards and detail panel headers | A full-width block on top, then a title bar and two lines |
| `figure` | KPI blocks | A short label bar, a wide 28px figure bar and a short trend bar |
| `chart` | Charts | A title bar and five bars of different heights aligned to a baseline |

### 10.22 Empty state — custom (`ph-empty-state`)

- Centered: sticker (80 to 130px), title (`title-3`), one sentence (max 36 characters wide) that says why it is empty and how to fill it, and one action when the user can act.
- Two kinds: "no data yet" (invites the first action) and "no results" (names the search term and offers "Quitar filtros").
- If the user's role cannot create the missing item, there is no action and the text says who can.

### 10.23 Picker tile — custom

- Large selectable option for wizards: `surface`, 1.5px `border`, `radius-xl`, 12px padding, object or avatar above a label, centered.
- Selected: `accent-soft` background with the selection border in 4.3. Disabled: 40% opacity with strikethrough for unavailable time slots.
- Grids use `repeat(auto-fill, minmax(128px, 1fr))`; time slots use 84px.

### 10.24 Demo tag — custom

Marks a capability that is shown with example data and has no backend yet.

- A neutral tag with a 1.5px dashed `border-strong` outline, transparent background, `ink-muted` text, label "Demo".
- Placed after the title of the card, block, tab or page it applies to, with 8px of space before it. It must not touch the text.
- It has a `pTooltip`: "Función en demostración: usa datos de ejemplo."
- Removed from a feature the moment it reads real data.

### 10.25 Definition list — custom

- Two columns: term in `ink-muted` on the left, value in `body-strong` right-aligned. 7px row gap.
- Used in detail panels, summaries and confirmation steps. Not for more than eight rows; beyond that, use a section with sub-headings.

---

## 11. Patterns

### 11.1 Resource list

Applies to pets, owners, veterinarians, drugs and treatments.

1. **Page header:** title, count subtitle ("86 pacientes"), primary action when the role may create.
2. **Toolbar:** search field (260px), filter chips, then on the right the view switcher.
3. **List (span 8):** table or card grid, with pagination at the bottom.
4. **Detail panel (span 4):** as defined in 10.13, always including "Abrir ficha completa".
5. **States:** skeleton on load; "no data yet" and "no results" empty states; inline alert with a retry action on error.

Search filters as the user types (debounced 250ms). Search text, filters, sort, page and view are reflected in the URL.

### 11.2 Resource detail page

- Reached from "Abrir ficha completa" and from direct links. Has its own URL (for example `/veterinario/pacientes/12`).
- Structure: back link, header color block in the identity hue (photo or sticker, name, key facts, status tag, page actions), section tabs, then a grid of cards.
- The header shows only actions the current role may perform.

### 11.3 Treatment history

- The treatments tab is a timeline (10.16) on the left (span 8) and the selected treatment's detail panel on the right (span 4).
- The active treatment is pinned at the top with day-of-course progress.
- Each event shows date, diagnosis, veterinarian and evolution tag; the panel shows drugs, notes and a PDF download.

### 11.4 Forms

- Single column, labels above fields, related fields grouped under a `title-3` heading. Two columns only for short paired fields (price and stock).
- Validate on blur and on submit, never on every keystroke. On submit, focus the first invalid field.
- Error messages say what is wrong and how to fix it: "Escribe el peso con números, por ejemplo 11,2."
- The submit button stays enabled; disabling it hides why the form cannot be sent. (Wizard "Continuar" is the exception, see 10.15.)
- Leaving a form with unsaved changes asks for confirmation.
- Create and edit forms of up to six fields open in a dialog; larger ones are pages.

### 11.5 Wizards

- For multi-step tasks such as booking an appointment. Stepper on top, step content, footer navigation, live summary beside it.
- Each step asks one question. Choices are picker tiles where there are six or fewer options.
- The last step is a review of all choices before the commit. Success replaces the wizard with a confirmation state (sticker, what happened, next actions).
- Booking for a new pet: the "Mascota" step includes a "Mascota nueva" tile that collects name and species only; the veterinarian completes the record at the visit. Clients cannot create or edit pet records elsewhere.

### 11.6 Dashboards

- First row: up to four KPI blocks. Then charts (span 8) with a ranked list (span 4), then operational cards.
- Period selector: segmented control with "Mes", "Trimestre", "Año", "Todo". "Todo" covers all recorded history and hides the comparison against a previous period.
- Every KPI states its comparison basis once per screen ("frente al periodo anterior").
- Daily summaries must show structure, not just counts: a segmented bar or mini timeline of the day, next appointment, and counts as supporting figures. Each segment has a hover and focus state (it grows slightly and its border darkens) and a `pTooltip` with the time, pet, reason, duration and owner, or "Libre" for an open slot.
- The administrator dashboard shows every metric the dashboard API returns as a first-class element, never demoted to the sidebar: total sales, gross profit, treatments administered in the last month, treatments by medication, top three medications by units sold, active and inactive veterinarians, and total and active pets. Demo-only metrics (period comparison, occupancy, low stock) are added around them.

### 11.7 Roles and permissions in the UI

- Actions a role cannot perform are not rendered. Do not show disabled buttons for permission reasons.
- Clients have read-only access to pet data: no "add pet" or "edit" controls. Veterinarians create and edit pets and treatments. Administrators manage veterinarians, owners and drugs.
- Each role has its own tab set; the role name appears under the user name in the top bar.

### 11.8 Loading, error and success

| Situation | Treatment |
|---|---|
| Page or panel data loading | Skeleton (10.21) |
| Action in flight | Button loading state; the rest of the view stays usable |
| Action succeeded | Toast with what happened |
| Action failed | Inline alert near the action, with the reason and a retry; the user's input is preserved |
| Page failed to load | Empty-state layout with an error sticker, plain explanation and "Reintentar" |
| Destructive action | Confirm dialog first |

### 11.9 Notifications (demo)

Notifications exist in three places for every role, all fed by the same source:

| Place | Shows | Ends with |
|---|---|---|
| Bell menu in the top bar | Three most recent: icon, title, relative time | "Mostrar todo" |
| Last card of the contextual sidebar | Three most recent: icon, title, one line of detail, relative time | "Mostrar todo" |
| "Notificaciones" tab | All notifications grouped by "Hoy" and "Esta semana", with unread markers, an "unread" filter and "Marcar todo como leído" | n/a |

- "Mostrar todo" always opens the Notificaciones tab.
- The bell shows a dot while there are unread notifications.
- Content is role-specific: appointments, daily care and vaccines for clients; agenda, urgencies, stock and pending check-ups for veterinarians; inventory, sales, team and imports for administrators.

### 11.10 Role-specific detail in treatments

The treatment detail panel shows more as the role gains responsibility:

| Role | Drugs | Totals |
|---|---|---|
| Client | Name only | None. Prices are never shown to clients |
| Veterinarian | Name, units, unit sale price, line subtotal | Prescription total |
| Administrator | Everything the veterinarian sees, plus unit purchase cost | Prescription total, purchase cost and profit |

The timeline shows the six most recent treatments, then a "Mostrar N más" text button that expands the rest in place ("Mostrar menos" collapses it).

### 11.11 Pet activation

- A pet is activated or deactivated with a toggle switch labelled "Mascota activa", the same pattern used for veterinarians.
- It appears in two places for veterinarians: in the detail panel, between the summary and the action buttons, and in the create/edit form as the last field before the form actions, with a one-line explanation.
- Toggling applies immediately and confirms with a toast. Administrators and clients do not see the switch.

### 11.12 Create owner, then offer a pet

After "Guardar dueño" succeeds, the same dialog switches to a confirmation state: a success title and the question "¿Desea registrar una mascota para este dueño?" with two buttons. "Registrar" opens the pet form with that owner preselected. "Más tarde" closes the dialog and shows the usual success toast.

### 11.13 Pet form preview

The form has a live preview card beside it. With a valid photo URL it shows the photo cropped to a circle on the identity hue; otherwise it shows the species sticker. A URL that fails to load falls back to the sticker and shows an inline hint under the field.

### 11.14 Drug import page

Importing drugs from Excel is a page with its own URL, not a dialog. It has three numbered steps and a side column:

1. "Prepara el archivo": the required headers in order, an example table with two rows, and a template download.
2. "Sube el archivo": drop zone, chosen file name, "Cancelar" and "Importar".
3. "Resultado": appears after the backend responds. Success shows file name, sheet name, rows read, drugs imported and blank rows skipped. Failure shows an inline danger alert with the backend message, and the row and column when provided.

The side column lists the file requirements (format, size, required and unique name, numeric rules, blank rows) and what happens when a row is invalid.

### 11.15 Daily tip

"Consejo del día" is a color block on the client home with a Caveat kicker, a `title-1`-scale headline, one sentence and a 130px sticker, sized so the block has no dead space. The tip comes from a fixed collection; the one shown is chosen by the date, so it changes every day and is the same for the whole day. Each tip names the sticker that matches its topic.

### 11.16 Search and filters

- Global search in the top bar searches across resources the role can see and groups results by type.
- List search is scoped to the list. Active filters are visible as selected chips; "Quitar filtros" appears when any is active.
- The drug list has a price range filter in the toolbar: two numeric fields, "Precio mín." and "Precio máx.", joined by "a". When the minimum is greater than the maximum the range is ignored and an inline message says so.

---

## 12. Content and formatting

### 12.1 Voice

- Spanish (Colombia), informal "tú", warm and plain. Sentence case everywhere, including titles and buttons.
- Say what happened and what to do next. No blame, no jargon, no exclamation marks in errors.
- One label per intent across the product:

| Intent | Label |
|---|---|
| Create an appointment | Agendar cita |
| Save a form | Guardar (plus object when ambiguous) |
| Dismiss without saving | Cancelar |
| Go to full record | Abrir ficha completa |
| Remove filters | Quitar filtros |
| Permanently remove | Eliminar |
| Go back a step | Atrás |
| Next step | Continuar |

### 12.2 Formats

| Data | Format | Example |
|---|---|---|
| Date, long | `d 'de' MMMM 'de' y` | 15 de noviembre de 2026 |
| Date, short | `d MMM y` | 12 nov 2026 |
| Day and time | weekday, day, 24h time | Martes 17 · 10:30 |
| Currency | COP, no decimals, dot thousands separator | $14.500 |
| Large currency | One decimal, "M" for millions | $18,4 M |
| Decimal | Comma | 11,2 kg |
| Percent | Space before the sign | 47 % |
| Missing value | Em dash in tables; "Sin registrar" in detail views | — |

Use Angular's `es-CO` locale pipes; do not format by hand.

### 12.3 Status vocabulary

| Resource | Status | Tag variant |
|---|---|---|
| Pet | Estable (active, no diagnosis) | success |
| Pet | En tratamiento (active, with a diagnosis) | warning |
| Pet | Inactiva (deactivated) | neutral |
| Drug | Disponible | success |
| Drug | Stock bajo | warning |
| Drug | Agotado | danger |
| Treatment | Mejorando | info |
| Treatment | Estable | neutral |
| Treatment | Empeorando | danger |
| Treatment | Recuperado | success |
| Vaccine | Vigente | success |
| Vaccine | Vence en N días | warning |
| Vaccine | Pendiente, Vencida | danger |
| Veterinarian | Activo / Inactivo | success / neutral |

---

## 13. Accessibility

Target: WCAG 2.2 level AA.

- **Contrast:** text 4.5:1 (3:1 for text at 24px or 18.66px bold and above); interactive boundaries and meaningful graphics 3:1. Use the pairs in 2.7; any new pair must be measured.
- **Color independence:** status, selection and identity always have a text or icon equivalent.
- **Focus:** every interactive element has a visible `:focus-visible` ring: 2.5px `accent`, 2px offset. On color blocks the ring is `on-hue`. Never remove outlines.
- **Keyboard:** everything is operable by keyboard in a logical order. Tables, tabs, menus, the calendar and the stepper follow the WAI-ARIA Authoring Practices patterns provided by PrimeNG.
- **Targets:** per 4.5.
- **Semantics:** one `h1` per page (the page title); card titles are `h2` or `h3` in order. Landmarks: `header`, `nav`, `main`, `aside` (sidebar and detail panel, each labelled).
- **Selection state:** `aria-pressed` for toggles and chips, `aria-selected` for rows and tabs, `aria-current="page"` for navigation.
- **Live regions:** toasts use `role="status"`; errors use `role="alert"`. Skeleton regions set `aria-busy="true"`.
- **Forms:** every control has a programmatic label; errors are linked with `aria-describedby`.
- **Images:** stickers and geometry are decorative. Pet photos use `alt="Foto de {nombre}"`.
- **Motion:** honor `prefers-reduced-motion` (section 7).
- **Zoom:** layouts work at 200% zoom and at 320px width without horizontal scrolling of the page.

---

## 14. Implementation

### 14.1 Tailwind tokens (`frontend/src/styles.css`)

Semantic tokens are CSS custom properties exposed to Tailwind v4 through a `@theme static` block (static, so every token is emitted even before a template uses it), and redefined under `.app-dark`. The dark variant is declared once: `@custom-variant dark (&:where(.app-dark, .app-dark *));`.

Token names in code differ from the conceptual names in section 2 where a conceptual name would collide with an existing utility of `tailwindcss-primeui` (`border-surface`) or with Tailwind's default radius scale, which the landing page still uses.

| Concept (sections 2 and 4) | Token | Utility examples |
|---|---|---|
| `ground` | `--color-ground` | `bg-ground` |
| `surface` | `--color-card` | `bg-card` |
| `surface-sunken` | `--color-sunken` | `bg-sunken` |
| `ink`, `ink-muted` | `--color-ink`, `--color-ink-muted` | `text-ink`, `text-ink-muted` |
| `border`, `border-strong` | `--color-line`, `--color-line-strong` | `border-line`, `border-line-strong` |
| `accent`, `accent-hover`, `on-accent`, `accent-soft`, `accent-text` | `--color-accent`, `--color-accent-hover`, `--color-on-accent`, `--color-accent-soft`, `--color-accent-text` | `bg-accent`, `text-on-accent` |
| Hues, `on-hue` | `--color-hue-honey` (and sage, sky, rose, lilac, peach), `--color-on-hue` | `bg-hue-sage`, `text-on-hue` |
| Status base | `--color-success`, `--color-warning`, `--color-danger`, `--color-info` | `bg-success` |
| Status soft pair | `--color-<status>-soft`, `--color-on-<status>-soft` | `bg-warning-soft text-on-warning-soft` |
| Solid destructive | `--color-danger-solid`, `--color-on-danger-solid` | `bg-danger-solid` |
| `radius-xs` (7px) | `--radius-check` | `rounded-check` |
| `radius-sm` (10px) | `--radius-field` | `rounded-field` |
| `radius-md` (12px) | `--radius-control` | `rounded-control` |
| `radius-lg` (14px) | `--radius-row` | `rounded-row` |
| `radius-xl` (20px) | `--radius-box` | `rounded-box` |
| `radius-full` | Tailwind default | `rounded-full` |
| `ease-out` | `--ease-out-strong` | `ease-out-strong` |
| Fonts | `--font-display`, `--font-body`, `--font-script` (already in the project) | `font-display`, `font-body`, `font-script` |

- The legacy tokens (`olive`, `secondary`, `tertiary`, `honey`, `terracotta`, `sage`, `background`, `text`) and the landing animations stay in `styles.css` while the current landing uses them. They are removed only when the landing is redesigned.
- Authentication and portal templates must use the semantic utilities above. Raw hex values, Tailwind's default palette, the legacy tokens and `rounded-sm` to `rounded-3xl` are not allowed there.

### 14.2 Hue utility

A color block sets one local variable and everything inside derives from it:

```css
.hue-honey { --hue: var(--color-hue-honey); }
/* ...one class per hue... */

.ph-block {
  background: var(--hue);
  color: var(--color-on-hue);
  --color-ink-muted: color-mix(in srgb, var(--color-on-hue) 72%, var(--hue));
  --color-border: color-mix(in srgb, var(--color-on-hue) 18%, var(--hue));
}
```

`identityHue()` (2.4) lives in a shared utility and is exposed as a pure pipe: `[class]="'hue-' + (pet.id | identityHue)"`.

### 14.3 PrimeNG preset (`frontend/src/app/theme/pethaven-preset.ts`)

- Keep `definePreset(Aura, ...)`. Map `semantic.primary` to the olive scale, and `semantic.colorScheme.light` / `.dark` surfaces, text and border colors to the tokens in 2.2.
- `app.config.ts` already sets `darkModeSelector: '.app-dark'`. Tailwind's `dark` variant uses the same class, so PrimeNG and Tailwind always switch together.
- The preset's surface scale carries the neutrals: in light theme 50 is the card surface, 100 sunken, 200 border, 500 strong border, 600 muted ink and 950 ink; in dark theme 950 is the ground, 900 the card surface, 800 sunken, 700 border, 500 strong border, 400 muted ink and 50 ink.
- Override component tokens to match section 10: border radii (4.2), control heights (4.5), 1.5px form borders, no card shadow, focus ring (2.5px, 2px offset, `accent`), toast and dialog shapes.
- Prefer preset tokens over `::ng-deep` or global CSS overrides. Use a component's pass-through (`pt`) options for structural class changes.

### 14.4 Component structure

New components follow the conventions the frontend already has. There is no separate `shared/ui` folder and no `ph-` prefix: sections 9 to 11 use names such as `ph-card` as conceptual labels only.

| Kind | Location | Selector |
|---|---|---|
| Shared presentational components (card, block, tag, avatar, stock meter, empty state, resource list, detail panel, shell) | `pages/shared/<name>/` | `app-<name>` |
| Components used by one area only | `pages/<area>/.../components/<name>/` | `app-<name>` |
| Pure functions, pipes, services without UI | `core/<topic>/` | n/a |
| Demo-only data services | `services/demo/` | n/a |
| Pages | `pages/<area>/` | `app-<name>` |

- Presentational components take inputs and emit outputs; they do not call services. Pages fetch data and compose them.
- Standalone components, signals, `input()` and `output()`, separate `.html` template files, as in the existing code.
- Existing shared components (`page-banner`, `stat-card`, `area-layout`) are replaced by the block, KPI and shell components and removed once nothing uses them.
- Stickers are an SVG sprite loaded once; the sticker component renders `<use>` with the coat kit as CSS variables.

### 14.5 Dependencies to add

| Package | Reason |
|---|---|
| `@lucide/angular` | Icon set (section 5) |
| `chart.js` | Required by PrimeNG `p-chart` (10.18) |

### 14.6 Theme bootstrapping

An inline script in `index.html` sets the `app-dark` class before Angular starts, from `localStorage['pethaven.theme']` or `prefers-color-scheme`. A `ThemeService` exposes the current theme as a signal, toggles the class, persists the choice and notifies charts to re-read their colors.

---

## 15. Governance

### 15.1 Implementation constraints set by the owner

- New code follows the architecture and patterns the frontend already uses. Code comments are short and added only when they are really needed.
- No dependency may be added without the owner's approval. Approved so far: `@lucide/angular` and `chart.js`. If another library would improve something, propose it and wait for approval.
- Build small and large reusable components so markup and logic are not repeated.
- Design for desktop first, and give mobile layouts real attention so the experience is good there too.
- The backend is not changed. Demo-only features get their backend later. A backend change is acceptable only if it is small and low impact, and only with the owner's approval.
- Every screen is verified in the browser, in both themes and at the three breakpoints, with temporary test data that exercises every state (loading, empty, error, long values, edge cases).

### 15.2 Process

- **Adding a token:** only when an existing token cannot express the role. Add it here first, with light and dark values and measured contrast.
- **Adding a component:** check PrimeNG first. A custom component needs an entry in section 10 with anatomy, variants and states.
- **Adding an illustration:** follow 6.1 and register it in 6.2.
- **Review checklist for any UI pull request:**
  1. Uses semantic tokens only; no raw hex, no default Tailwind colors.
  2. Works in light and dark theme.
  3. Works at `compact`, `medium` and `expanded` widths.
  4. Has loading (skeleton), empty and error states.
  5. Keyboard operable with visible focus.
  6. One accent, radii from the scale, one primary action per view.
  7. Copy follows section 12.
- **Slop pre-flight (must all be true):** one accent hue; one neutral family; radii only from the scale; no emoji in chrome; no gradients; no card shadows; no duplicate labels for one intent.

---

## 16. Decision log and open items

### 16.1 Decisions made by the owner

| Topic | Decision |
|---|---|
| Direction | "Bloques": color blocks with geometry and sticker illustrations |
| Hue tones | The intermediate tones of "Ilustrado vivo", in both themes |
| Dark theme | Neutral black (the "Bloques" dark theme); the olive-tinted dark theme is discarded |
| Card treatment | 1px border; bottom "volume" shadows discarded |
| Primary navigation | Top tabs in the pill style of "Pastel denso"; icon rail discarded |
| Buttons and controls | 12px radius |
| Contextual sidebar | Fixed left, collapsible, drawer on smaller widths |
| Mobile navigation | Bottom tab bar |
| Accent | Current olive |
| Typography | Fraunces, Nunito Sans, Caveat for accents |
| Illustration | Sticker style, larger and overflowing in hero blocks |
| Identity color | Permanent hue by ID for pets, drugs, veterinarians and owners |
| Pet photo | Photo when present, sticker as fallback |
| Icons | Lucide |
| Charts | Chart.js through PrimeNG `p-chart` |
| Theme selection | System preference first, manual toggle persisted |
| Lists | Card and table views with pagination; default by resource |
| Detail | Side panel on every resource list, plus a full detail page with its own URL |
| Loading | Skeletons instead of spinners |
| Scope | Authentication and the three portals. Landing redesign deferred; the current landing keeps its blobs, waves, marquee and paw trail |
| Dashboard period | "Mes", "Trimestre", "Año", "Todo" |

### 16.2 Additional decisions confirmed by the owner

Proposed by the author and approved in review. They are final.

| Topic | Decision |
|---|---|
| Dark theme hues | Exactly the light theme values, no dimming |
| Neutral values | `ground` `#efe8da`, `surface` `#fffaf0`, `border` `#ddd0b9` |
| Round controls | Only primary navigation, tags, avatars, meters, toggle and toast are fully round; filter chips and the segmented control use the 12px radius |
| Identity hue order | Honey, sage, sky, rose, lilac, peach, with no per-resource-type offset |
| Appointment color | The pet's identity hue, not the veterinarian's |
| Page size | 12 in both card and table views |
| Toast position | Bottom center |
| Forms | Create and edit forms of up to six fields open in dialogs; larger ones are pages |
| Charts | No pie or donut charts |
| Accessibility | WCAG 2.2 AA |
| Density | One density, no compact mode toggle |

### 16.3 Open items

| Item | Notes |
|---|---|
| Landing page redesign | Deferred. Section 9.10 will define marketing surfaces when it starts |
| Chart series 4 to 6 | Not contrast-tested against both surfaces |
| Muted text on hues in dark theme | Derived value not measured |
| Demo-only features | These ship in the UI with example data and are marked with a "Demo" tag; their backend and real implementation are pending: appointments (client booking wizard and veterinarian agenda), daily care plan, weight curve, vaccine card, minimum stock and inventory alerts, dashboard period selector with sales, profit and occupancy charts, global search and notifications. Replenishment orders and the live waiting room are out of scope |
