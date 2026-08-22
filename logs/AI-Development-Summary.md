# AI-Assisted Development Summary

These entries are factual summaries for developer verification and later reflection.

## Initial architecture and registration

- **Task:** Establish JavaFX/Gradle structure, registration, and local JSON persistence.
- **Decisions:** Keep the app offline, omit club affiliation, retain UUID identity, and separate UI,
  application, domain, and persistence.
- **Outcome:** Registration, validation, seeding state, and JSON persistence were implemented.
- **Notable issue:** Fencer-name uniqueness changed after feedback and required a regression test.

## Pool workflow

- **Task:** Define pool rules, generate pools, record results, and calculate standings.
- **Decisions:** Use manual seed order, balanced snake distribution, five-touch pool bouts, and
  tie-breaks of victory ratio, indicator, touches scored, then seed.
- **Outcome:** Pool generation, result validation, matrix presentation, and Pool Result placing were
  implemented and tested.
- **Notable issue:** Matrix and result-entry UI needed repeated refinements after visual feedback.

## Direct elimination and final results

- **Task:** Generate a seeded DE tableau, handle byes, enter results, and show final standings.
- **Decisions:** Use a next-power-of-two bracket, automatic bye advancement, and deterministic final
  placement based on DE progression then Pool Result.
- **Outcome:** Tableau progression, completion state, and final results were implemented.
- **Notable issue:** Tableau geometry and labels underwent repeated visual refinements.

## Multiple tournaments and navigation

- **Task:** Support Tournament Home, unique names, and switching between tournaments.
- **Decisions:** Names are unique user-facing identifiers; UUIDs remain internal.
- **Outcome:** Ongoing/completed tournament lists and phase-specific opening behavior were added.
- **Notable issue:** A later-phase tournament retained the previous tab; phase-based selection was
  added with a regression test.

## Result correction

- **Task:** Safely edit recorded pool and DE scores.
- **Decisions:** Preserve downstream state when a DE winner is unchanged; otherwise invalidate only
  dependent results after explicit confirmation.
- **Outcome:** Pool and DE edit flows, invalidation logic, and domain tests were added.
- **Notable issue:** Completed-result edit controls required explicit work to be reachable.

## Autosave

- **Task:** Remove reliance on manual saving and restore tournaments on restart.
- **Decisions:** Autosave after successful service mutations and use temporary-file replacement.
- **Outcome:** Tests cover independent tournaments, pool/DE corrections, invalidation, completion,
  and persistence failures.
- **Notable issue:** Derived JSON properties initially broke repeated saves after pool data existed;
  field-based Jackson visibility and reload tests corrected this.

## Emerald & Teal visual theme

- **Task:** Replace repeated component colours with a cohesive, extensible visual theme.
- **Decisions:** Define semantic JavaFX CSS tokens on an Emerald & Teal root class, keep result
  colours distinct from interaction accents, and apply the stylesheet to dialogs as well as the
  main scene.
- **Outcome:** The shared stylesheet now uses theme tokens across the shell, home, inputs, grids,
  matrix, tableau, results, and dialogs.
- **Notable issue:** The visual work intentionally did not change tournament workflow or scoring
  rules.

## Workspace UX composition

- **Task:** Redesign the application composition around compact tournament operations rather than
  isolated JavaFX forms.
- **Decisions:** Keep stage navigation and the existing domain/service calls; introduce reusable
  workspace, pool-panel, result-entry, and toolbar patterns in the shared view and token stylesheet.
- **Outcome:** Registration and seeding use focused action-and-roster workspaces, Direct Elimination
  keeps its editor beside the tableau, and the Pools stage renders all pool matrices in a responsive
  dashboard. Matrix selections carry their pool identity so score entry still targets the correct bout.
- **Notable issue:** The dashboard intentionally wraps and scrolls only when the number of pools
  exceeds the available desktop viewport; matrices are not compressed into unreadable cells.

## Registration workspace refinement

- **Task:** Replace the visually oversized registration form/list composition with a genuine
  operational workflow layout.
- **Decisions:** Keep the existing `ListView` selection model for controller compatibility, but
  render it as a numbered roster ledger and size it to its actual contents. Make adding a fencer a
  one-line operation and put the progression action in a separate footer.
- **Outcome:** Registration now has title, compact add control, contextual roster management, and
  page-level progression without changing registration domain logic.
- **Notable issue:** A JavaFX UI test verifies that a small roster does not retain the old oversized
  list height.

## Sidebar application shell

- **Task:** Replace the horizontal stage-tab presentation with a compact tournament-workspace shell.
- **Decisions:** Use a persistent sidebar for Tournament Home and phase-gated stages; retain the
  existing `TabPane` only as a non-visible selection model to avoid changing controller or domain
  workflow semantics.
- **Outcome:** The header now focuses on tournament context and utilities, while the centre content
  deck is dedicated to the active stage. Registration is the first migrated reference workspace.
- **Notable issue:** A focused UI test verifies central stage selection remains synchronised with
  the internal tab state.

## Header and navigation hierarchy

- **Task:** Remove duplicated Home, tournament-context, and creation hierarchy from the shell.
- **Decisions:** Hide tournament context and competition-stage navigation when no tournament is
  open; label the sole global import action as `Open File`; keep New Tournament only on Tournament Home.
- **Outcome:** The top bar is now application identity, optional tournament context, and utility
  action; page headers own page actions, including Direct Elimination generation on Pool Result.
- **Notable issue:** A JavaFX regression test covers the header context visibility transition.

## Eight-fencer pool format and projected board

- **Task:** Support the club's common two-pool, eight-fencer format and make both matrices visible
  together for projected use.
- **Decisions:** Extend the domain maximum to eight while retaining the previous 5--7 behavior;
  use an explicit compact two-pool layout mode instead of hardcoding tournament data into the view.
- **Outcome:** Sixteen fencers with maximum eight produce two balanced snake-seeded pools. The
  two-pool board uses compact cells, ellipsised/tooltip names, no board scrolling, and a collapsed
  result strip until a matchup is selected.
- **Notable issue:** Pool generation tests initially exposed an unintended change to the existing
  five-person target behavior; the pool-count ceiling is now conditional on selecting eight.

## Pool-board responsiveness and dismissal

- **Task:** Stop pool panels from stacking unnecessarily and make selected score entry dismissible.
- **Decisions:** Bind the FlowPane wrapping width to the visible viewport; use title-and-metadata
  pool headers; route click-outside and Escape dismissal to a controller-owned no-save cancellation path.
- **Outcome:** Pools fill available columns where readable, headers have clear hierarchy, and the
  result strip collapses when no bout is selected.
- **Notable issue:** Escape handling is covered by a JavaFX regression test that confirms a cleared
  selection is not dismissed repeatedly.

## Pool-board viewport-width correction

- **Task:** Correct a screenshot-confirmed case where two pools still stacked despite unused space.
- **Decisions:** Set the FlowPane's actual preferred width as well as its wrap length from the
  ScrollPane viewport. A subsequent screenshot revealed that JavaFX could still choose an
  inconsistent internal FlowPane width, so the exact two-pool case now uses an explicit `HBox` row;
  FlowPane remains the responsive fallback for one and three-or-more pools.
- **Outcome:** Two readable pool panels cannot be vertically stacked by FlowPane wrapping when the
  application is rendering exactly two pools.
- **Notable issue:** JavaFX regression tests cover both the explicit two-column row and the
  viewport-derived wrapping width used by the multi-pool fallback.

## Safe tournament deletion

- **Task:** Allow an organiser to remove an accidental ongoing or completed tournament from
  Tournament Home.
- **Decisions:** Keep confirmation and destructive styling in the UI, but make the UUID-based
  collection mutation and JSON-file removal a `TournamentService`/`TournamentRepository`
  responsibility. A missing identifier is a no-op and does not trigger persistence; deleting an
  active tournament clears the active reference.
- **Outcome:** Each Home row keeps Open/View Results primary and places Delete in a compact overflow
  menu with a named, irreversible-action warning. Successful deletion immediately removes exactly
  one JSON file and survives restart.
- **Notable issue:** Per-tournament files require an explicit repository delete operation; merely
  re-saving the remaining collection would otherwise leave the removed tournament on disk.
- **UI refinement:** The initial JavaFX `MenuButton` rendered an unwanted dropdown arrow, so the
  row action was changed to a plain icon-sized `…` button backed by a `ContextMenu`; the destructive
  menu item remains labeled and muted red.
- **Follow-up refinement:** The closed ellipsis now returns to a neutral background, and the
  context menu uses the shorter `Delete` label with compact spacing and width.

## Pool matrix lower-half result orientation

- **Task:** Fix score-entry reversal after recording a bout selected from the lower-left half of a
  pool matrix.
- **Decisions:** Preserve the selected matrix row fencer while rebuilding the selected `PoolBoutRow`
  after a refresh, rather than recreating it only in the bout's scheduled order.
- **Outcome:** Result entry and later edits retain the row-versus-column order that the organiser
  clicked, while the domain score continues to be stored in scheduled-bout order.

## Tournament Home scrollbar policy

- **Task:** Remove a spurious Tournament Home scrollbar observed with exactly two tournaments.
- **Decisions:** Disable horizontal scrolling for the compact Home list and disable vertical
  scrolling for zero through two entries; larger lists retain normal vertical scrolling on demand.
- **Outcome:** Small tournament collections no longer display an unnecessary scrollbar, while
  longer histories remain scrollable.

## Seed ordering interaction

- **Task:** Remove the redundant revised-order confirmation and allow drag reordering.
- **Decisions:** Let JavaFX calculate a visual insertion target, but delegate each successful move
  to a service operation that validates and persists the pre-pool seed order.
- **Outcome:** Seeding can be reordered by drag or buttons without a separate revised-order action;
  **Generate pools** remains the action that advances the tournament and locks the order.

## Active direct-elimination round visibility

- **Task:** Ensure every bout in the current DE round, including all eight bouts in a Round of 16,
  is visible at a projected 1440×900 desktop size without vertical scrolling.
- **Decisions:** Replace the fixed opening-round spacing and fixed 500 px viewport cap with spacing
  derived from the opening-round match count. Keep the two-row bout cards readable, use horizontal
  overflow only for later rounds, and identify the earliest playable round as the active round.
- **Outcome:** A 16-fencer opening column now occupies approximately 628 px rather than 860 px;
  the active heading and cards receive a restrained accent, and the bracket scrolls horizontally to
  bring the active round into view after rendering.
- **Notable issue:** JavaFX's `ScrollPane` fitting width would compress the bracket canvas rather
  than expose intentional horizontal overflow, so the bracket viewport now preserves the board's
  calculated width.
- **Follow-up correction:** The initial calculated viewport height could still exceed the space left
  by the application shell on an 800 px-high desktop. The tableau now reads its actual post-layout
  `ScrollPane` viewport height and recomputes opening-round spacing on resize instead of imposing a
  minimum viewport height.

## Pool result hierarchy and shortened DE scores

- **Task:** Separate the pool identifier from the result-entry heading and allow time-limited DE
  bouts to finish below the standard 15-touch maximum.
- **Decisions:** Stack the pool context above `RECORD RESULT` in the existing view-only component.
  Keep DE validation in `EliminationBracket`, shared by recording and editing, and treat 15 as an
  inclusive ceiling rather than a mandatory winning score.
- **Outcome:** Pool context now reads as metadata, while results such as 10--7 and 5--4 advance the
  higher-scoring fencer and continue to use existing downstream invalidation rules.
- **Notable issue:** Existing tests encoded the old mandatory-15 assumption; they were updated to
  distinguish scores above 15 from valid shorter winning scores.

## Tournament stage-progress navigation

- **Task:** Replace generic stage navigation with a compact connected progression tracker.
- **Decisions:** Retain the hidden `TabPane` and existing stage buttons as the compatibility and
  interaction layer; render them as marker-only circles with labels and direct circle-to-circle
  connectors. Derive completed, current, and locked states exclusively from the existing phase
  and availability inputs. The wider step hit target forwards label-area clicks only when its
  underlying navigation button is already available.
- **Outcome:** Completed stages use filled teal circles, the current stage uses a larger outlined
  teal circle, and unavailable future stages use muted hollow circles. Accent connector segments
  extend through the current phase. The tracker conveys tournament flow without changing
  navigation permissions or controller logic; Tournament Home remains the only item in the sidebar.
- **Notable issue:** A JavaFX regression test verifies the Pool Phase mapping: one completed marker,
  one current marker, and three locked future markers.
- **Rendering correction:** The first marker-only implementation made labels unmanaged within a
  narrow `StackPane`, so the header could measure only connector height and clip the nodes. Each
  stage now uses a managed marker-slot-and-label column; connector slots extend only visually
  between the fixed marker edges. A regression test checks that all five markers and labels have
  visible, non-zero bounds after layout. A follow-up screenshot exposed missing circles: resetting
  a button's style list had also removed JavaFX's base `button` class, preventing the composed
  `.button.stage-marker` CSS selector from matching. The base class is now preserved.
- **State correction:** Progress-marker styling now represents only the tournament's actual phase.
  When an organiser navigates back to a completed page, that page receives a label underline rather
  than a second current-stage ring; a regression test covers the Direct Elimination / Pool Result
  case.

## Sidebar theme selection

- **Task:** Add lightweight, persistent appearance customisation without coupling it to tournament
  data or autosave.
- **Decisions:** Extend the existing looked-up-token stylesheet with four root classes and keep
  semantic win/loss/disabled tokens stable across them. `UiTheme` stores the selected enum value in
  the local Java preference store and applies it to both the workspace and future dialogs.
- **Outcome:** The bottom of the sidebar contains an `APPEARANCE` section with four accessible,
  tooltip-equipped swatches. Switching is immediate, selected state is ringed, and the chosen theme
  restores on the next launch without touching tournament JSON.

## Independent Light and Dark appearance

- **Task:** Add Light, Dark, and System appearance without multiplying the four colour themes.
- **Decisions:** Split CSS into appearance tokens (surfaces, text, borders, disabled and semantic
  states) and colour-family tokens (accent/focus). Persist appearance independently with the
  existing local preference mechanism. JavaFX provides no dependable cross-platform OS appearance
  API, so the persisted `System` choice currently resolves safely to Light.
- **Outcome:** The sidebar now contains a compact Light/Dark/System segmented control above the
  colour swatches. Dark mode uses charcoal surfaces and brighter controlled accent variants while
  preserving distinct win/loss/unavailable colours.

## Dark-mode text contrast audit

- **Task:** Correct dark-on-dark text in result-entry panels and other controls after introducing
  Dark appearance.
- **Decisions:** Define primary, secondary, muted, disabled, and on-accent text tokens on each
  appearance root. Use shared CSS defaults for Labels, inputs, ComboBoxes, list cells, tooltips,
  buttons, and menu items rather than patching individual result labels.
- **Outcome:** Dark mode now resolves all default and specialised body text to readable light
  neutral tones, while semantic green/red states remain distinct and fields retain readable text,
  prompt text, focus, and disabled states.

## DE availability and appearance-toggle refinement

- **Task:** Make a Direct Elimination bout available as soon as both entrants are known, simplify
  appearance selection to Light/Dark, and prevent colour themes from changing competition-state
  meanings.
- **Decisions:** Retain `EliminationMatch.isReady()` as the domain rule, but have the tableau derive
  its selectable card state explicitly from two known non-bye participants rather than applying an
  earliest-ready-round highlight. Replace the three-option appearance selector with a persisted,
  keyboard-accessible Light/Dark switch. Keep accent tokens in colour families and success/danger
  plus their foreground-text tokens in appearance layers.
- **Outcome:** A Quarter-final, Semi-final, or Final can be selected as soon as its two feeder
  winners are present, regardless of unrelated pending earlier bouts. The sidebar now offers only
  a Light/Dark switch, and Advanced/Eliminated and win/loss states retain their semantic colours
  across every colour family and appearance.

## Pool-result semantic-colour regression fix

- **Task:** Restore green/red Pool Result statuses after the shared dark-mode text rule masked
  their semantic foreground colours.
- **Decision:** Increase the semantic status selectors' specificity through the workspace root,
  rather than applying inline colours to individual rows.
- **Outcome:** `Advanced` and `Eliminated` reliably use their independent success/danger tokens;
  a JavaFX test verifies the rendered foreground paints remain distinct.

## Inline score-validation feedback

- **Task:** Make result-entry validation visible beside the relevant score fields instead of only
  in the global status bar, and replace implementation wording with concrete score messages.
- **Decisions:** Keep validation rules in `BoutScore`, `Pool`, and `EliminationBracket`. The
  controller maps their known score errors to local result-entry messages and uses the active
  tournament settings to display the real score limit. The view owns the inline error label,
  per-field error styling, focus, and clearing behaviour.
- **Outcome:** Pool and DE entry/edit panels preserve entered values after an invalid submission,
  display a theme-safe muted-red message beneath the controls, and mark only the relevant field
  where one is identifiable. Global status messages remain for persistence and file operations.

## Inline validation audit

- **Task:** Extend local validation feedback beyond scores and stop routing routine form mistakes
  solely to the bottom status bar.
- **Decisions:** Classify blank/duplicate tournament names, blank/duplicate fencer names, missing
  roster selection, and missing maximum-pool-size selection as local form failures. Keep import,
  persistence, autosave, malformed-file, and unexpected workflow failures global.
- **Outcome:** Tournament Home, initial tournament creation, registration, and seeding reuse the
  existing inline error label and subtle invalid-control border. Text edits, successful actions,
  selection changes, and cancelled creation clear stale feedback.

## Initial board-layout correction

- **Task:** Fix Pool and Direct Elimination bouts that were not selectable until the application
  window was resized.
- **Cause:** Rendering occurred before `Stage.show()`, while the Pool `ScrollPane` viewport and
  DE workspace had zero bounds. The Pool board was assigned a one-pixel preferred width and the
  DE board used provisional geometry; resize listeners later corrected both.
- **Outcome:** The app opens maximized and refreshes the active size-dependent workspace after the
  stage is shown and after stage changes. A JavaFX regression test verifies Pool and DE bout
  handlers work after first layout without a manual resize.

## Unified Setup workflow

- **Task:** Merge the separate Registration and Seeding screens into one Setup workspace without
  changing pool-generation rules.
- **Decisions:** Make the persisted `Seeding` order authoritative throughout pre-pool setup.
  `Tournament` appends/removes IDs with roster changes, and `TournamentService.moveSeedFencer`
  continues to perform insertion-based moves. The JavaFX view renders only that ordered list,
  with drag handles and compact row-level removal actions; it no longer exposes a confirmation,
  Move Up/Down, or separate registration roster. Legacy setup saves with null seeding are
  reconstructed in registration order for compatibility.
- **Outcome:** Adding, removing, and reordering a fencer immediately updates the visible seed
  numbers. Selecting the pool-size option and generating pools uses the displayed order directly;
  autosave continues to occur through the existing service mutation path.

## Setup viewport and drag auto-scroll

- **Task:** Keep Setup configuration controls anchored while longer seed orders remain easy to
  reorder.
- **Decisions:** Replace roster-size-dependent ListView height with a bounded min/preferred/max
  viewport and reserve it even for the empty state. Add a lightweight JavaFX `AnimationTimer` that
  adjusts the ListView's own vertical scrollbar only during a valid drag in a top/bottom edge zone.
- **Outcome:** Long seed lists scroll internally, dragging can continue across off-screen rows,
  and maximum-pool-size/Generate pools controls no longer move down as fencers are added.

## Tournament modification timestamps

- **Task:** Record when each tournament's persistent state actually changes and use that time to
  order Tournament Home.
- **Decisions:** Store an `Instant` on the `Tournament` aggregate and refresh it centrally after a
  successful `TournamentService` mutation, never on open/view/navigation. Persist an ISO timestamp
  in JSON; legacy saves without it load with an epoch fallback. Order ongoing and completed Home
  sections newest-first with name as a stable tie-breaker.
- **Outcome:** Home rows show quiet human-friendly `Updated …` metadata, recent changes appear
  first in each section, and timestamp persistence/migration/order behaviour is covered by tests.

## Completed-tournament timestamps

- **Task:** Distinguish an ongoing tournament's latest saved change from the moment a tournament
  actually finished.
- **Decisions:** Add optional `completedAt` to the aggregate and persistence DTO. The central
  successful-mutation path sets it only when the DE bracket first becomes complete and clears it
  if a score correction invalidates completion. Tournament Home orders ongoing rows by
  `lastModified` and completed rows by `completedAt`; a Home-only minute timer only reformats
  relative `Updated …` text.
- **Outcome:** Completed cards show `Completed <date>` near View Results, ongoing cards retain
  `Updated …`, and completion timestamps survive reloads while reset/re-completion is covered by
  tests.

## Theme-aware scrollbars

- **Task:** Replace visually disconnected platform scrollbar styling with a reusable treatment
  across Seed Order, pool boards, result tables, and the DE tableau.
- **Decisions:** Define neutral track, thumb, and hover-thumb tokens in Light/Dark appearance
  layers, with restrained per-family tints. Apply one JavaFX selector set to shared scrollbars,
  tracks, thumbs, arrows, and corners rather than screen-specific rules.
- **Outcome:** Scrollbars remain slim, grabbable, and readable in every colour family and
  appearance without using the tournament accent as a permanently saturated control.
