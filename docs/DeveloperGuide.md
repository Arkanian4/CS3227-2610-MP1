# Developer Guide

## Architecture

The application uses a small local layered architecture:

```text
JavaFX view and controller
        ↓
TournamentService
        ↓
Tournament aggregate and rule calculators
        ↓
TournamentRepository
        ↓
JsonTournamentRepository
```

`ui` renders presentation state and routes user actions to `TournamentController`. Controllers do
not calculate standings, bracket placement, or score validity.

`application.TournamentService` coordinates workflows, owns the active tournament and tournament
collection, invokes rule calculators, updates the active aggregate's `lastModified` time after
successful mutations, records `completedAt` on the first transition to a complete DE bracket, and
autosaves after successful mutations. Opening or viewing is read-only and does not refresh either
timestamp. If a correction makes a completed bracket incomplete, the same mutation path clears
`completedAt`. It also owns
tournament deletion: it removes the UUID-keyed collection entry, clears a matching active reference,
and delegates removal of the corresponding local JSON file to the repository.

`domain.Tournament` is the aggregate root. It protects setup, pool, and DE phase
invariants. `domain.rules` contains pool generation, standings, bracket generation, and final
placement calculations.

`persistence.JsonTournamentRepository` reconstructs a `Tournament` through its constructor so
persisted data must satisfy domain invariants. It persists `lastModified` and optional
`completedAt` as ISO-8601 instants; older JSON without either field falls back to the save file's
modification time where the tournament state requires one, so legacy data remains loadable and
receives useful Tournament Home ordering and metadata.

## Visual theme architecture

`ui/tournament.css` combines two independent token layers: Light/Dark appearance classes define
neutral surfaces, text, borders, disabled states, and readable semantic states; four colour-theme
classes define accent, accent-hover, accent-subtle, and focus values for Emerald & Teal, Deep Navy
& Electric Blue, Royal Purple & Violet, and Ice Blue & Cerulean. Component rules consume tokens
rather than repeating raw component colours. `UiTheme` persists both selections through Java's
local preference store, independently of tournament JSON files. The appearance layer defines
primary, secondary, muted, disabled, on-accent, success, success-text, danger, and danger-text
tokens; theme families do not replace the success/danger meanings. Shared Label, input, tooltip,
button, and menu rules consume these tokens so Dark mode does not depend on JavaFX's
light-oriented default text paint.

`UiTheme` applies the selected root class and stylesheet to both the main JavaFX scene and dialog
panes, so confirmation dialogs remain visually consistent with the workspace.

## Workspace composition

`TournamentView` owns reusable presentation patterns: a persistent tournament header and circular
stage-progress rail, compact operational setup workspaces, grid-based result tables, a shared result-entry
panel, and the Direct Elimination tableau. The visible stage navigation is a header-level progress
tracker; the existing `TabPane` is retained only as a non-visible selection model for compatible
phase/controller state.
`TournamentController` maps domain state to small UI
records and routes interactions back to `TournamentService`; it does not calculate tournament
rules.

Score form feedback is local to `TournamentView` result-entry panels. The controller only parses
the textual score fields and translates known domain validation outcomes into concrete messages;
`BoutScore`, `Pool`, and `EliminationBracket` remain the sources of score-rule validation. Global
status feedback remains reserved for persistence, file, and other application-wide outcomes.

The same `inline-validation-error` and `input-invalid` presentation pattern is reused for
tournament-name creation, fencer registration, roster selection, and the pool-size selector.
Controllers classify expected form errors before rendering them beside the initiating control;
autosave and file/persistence failures remain global.

The shell gives each category of information one visual home: the top bar holds application identity,
an optional current-tournament context, global file import, and the open tournament's stage
tracker; the sidebar holds Home; pages own their titles and stage-specific actions.

The controller can render a tournament before the JavaFX stage is visible, when `ScrollPane`
viewports report zero bounds. `FencingTournamentApp` invokes
`TournamentView.initializeAfterStageShown` after `Stage.show()`, rebuilding the active
size-dependent Pool or DE board with real dimensions. Stage switches use the same refresh path so
first-render hit regions do not depend on a user resize.

The Pools stage receives a `PoolDashboardPanel` per pool and renders the matrices together in a
wrapping dashboard. A `PoolMatrixSelection` carries the pool ID as well as both fencer IDs, so a
matrix cell can select the correct pool and scheduled bout without relying on a visible navigator.
This preserves the existing service and domain APIs while making all pools scannable at once.

For exactly two pools, the dashboard uses one explicit equal-width `HBox` row rather than relying
on `FlowPane` wrapping. This prevents JavaFX from choosing a narrow internal width and stacking two
otherwise readable matrices. One pool and three or more pools use the `FlowPane`, whose preferred
width and wrap length follow the visible scroll viewport. The two-pool format uses compact matrix
dimensions and disables board scrolling; three or more pools retain normal wrapping and scroll only
after readable panels no longer fit.

Setup uses one JavaFX `ListView` for the authoritative pre-pool seed order. Its custom cells pair
a visible drag handle and one-based seed number with a compact row-level remove action. The view
reports drag insertion and removal intent only; the controller delegates those mutations to the
service, then refreshes the list from the aggregate so the UI cannot become a competing source of
order. The list has a bounded responsive viewport so its configuration footer does not move with
roster size. During a valid drag, a short JavaFX timer adjusts only the ListView's internal vertical
scrollbar while the pointer is within its top or bottom edge zone; standard dragging and dropping
remain responsible for insertion and cancellation.

## Key decisions

Tournament names are unique case-insensitively after trimming. UUIDs remain internal identities for
reliable references and persistence.

The phase is derived from aggregate state rather than persisted separately:

`REGISTRATION (Setup) → POOL_PHASE → ELIMINATION_PHASE → COMPLETE`.

The UI selects the tab matching the opened tournament phase. This avoids retaining a previous
tournament tab when switching from Tournament Home.

Pool generation uses manual seed order, balanced snake distribution, and round-robin bouts. The
existing 5--7 pool behavior is retained; selecting a maximum of 8 explicitly permits eight-person
pools and lets sixteen fencers form two 8-person pools. Setup has one authoritative `Seeding`
order: adding a fencer appends its ID, removing a fencer removes its ID, and dragging a seed row
delegates to `TournamentService.moveSeedFencer` for insertion-based reordering. The pre-pool
order remains editable until `generatePools` consumes it; older setup JSON with a null seeding
value is normalised to registration order on domain reconstruction. Overall placing uses victory ratio,
indicator, touches scored, and original seed. The top 16 Pool Result
places advance to a next-power-of-two DE bracket with automatic byes for higher seeds.

## Autosave and correction

The application composition root configures `tournaments/` as the autosave directory.
`TournamentService.mutate` performs the domain action first and saves the full local tournament
collection only if the operation succeeds. `deleteTournament` is a deliberate exception: it first
asks the repository to remove the targeted JSON file, then removes the tournament from the
collection and re-saves any remaining tournaments. Failed persistence raises
`TournamentPersistenceException`; the JavaFX controller refreshes Home and reports the error rather
than claiming deletion succeeded.

JSON writes use a temporary file followed by replacement. Editing a pool result may reset DE.
Editing a DE result preserves later results when the winner is unchanged; otherwise only the
dependent downstream path is invalidated after confirmation.

## Testing

JUnit 5 tests cover registration invariants, pools, score validation, standings tie-breaks,
brackets/byes, final standings, JSON persistence, autosave/reload, corrections/invalidation,
tournament deletion, and selected JavaFX controller behavior. Run the suite with:

```powershell
.\gradlew.bat test
```

Current test priorities are controller-level edit/cancel/confirmation flows, app-startup loading,
and an explicit safe-write failure test that verifies an old JSON file remains intact.

## Acknowledgements

- JavaFX provides the desktop UI.
- Jackson Databind provides local JSON persistence.
- JUnit Jupiter provides automated testing.
- AI-assisted development evidence is recorded in
  [`logs/AI-Development-Summary.md`](../logs/AI-Development-Summary.md). The developer remains
  responsible for reviewing generated code, tests, and documentation.
