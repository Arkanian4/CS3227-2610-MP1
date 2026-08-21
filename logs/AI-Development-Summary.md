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
