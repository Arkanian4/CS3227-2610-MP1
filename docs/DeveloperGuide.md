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
collection, invokes rule calculators, and autosaves after successful mutations. It also owns
tournament deletion: it removes the UUID-keyed collection entry, clears a matching active reference,
and delegates removal of the corresponding local JSON file to the repository.

`domain.Tournament` is the aggregate root. It protects registration, seeding, pool, and DE phase
invariants. `domain.rules` contains pool generation, standings, bracket generation, and final
placement calculations.

`persistence.JsonTournamentRepository` reconstructs a `Tournament` through its constructor so
persisted data must satisfy domain invariants.

## Visual theme architecture

`ui/tournament.css` defines the Emerald & Teal theme through looked-up semantic tokens on the
`theme-emerald-teal` root class. Component rules consume tokens such as app background, surface,
primary text, border, accent, focus, success, danger, and unavailable state rather than repeating
raw component colours. A future theme should define the same tokens under a different root class.

`UiTheme` applies the selected root class and stylesheet to both the main JavaFX scene and dialog
panes, so confirmation dialogs remain visually consistent with the workspace.

## Workspace composition

`TournamentView` owns reusable presentation patterns: a persistent tournament header and stage
navigation, compact operational setup workspaces, grid-based result tables, a shared result-entry
panel, and the Direct Elimination tableau. The visible stage navigation is a sidebar; the existing
`TabPane` is retained only as a non-visible selection model for compatible phase/controller state.
`TournamentController` maps domain state to small UI
records and routes interactions back to `TournamentService`; it does not calculate tournament
rules.

The shell gives each category of information one visual home: the top bar holds application identity,
an optional current-tournament context, and global file import; the sidebar holds Home and only the
stage group relevant to an open tournament; pages own their titles and stage-specific actions.

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

Registration deliberately retains a JavaFX `ListView` because its selection model is used by the
remove-fencer controller workflow. The view customises it into a numbered roster ledger and sets a
compact preferred height from the current roster size, rather than reserving a large empty list.

## Key decisions

Tournament names are unique case-insensitively after trimming. UUIDs remain internal identities for
reliable references and persistence.

The phase is derived from aggregate state rather than persisted separately:

`REGISTRATION → SEEDING → POOL_PHASE → ELIMINATION_PHASE → COMPLETE`.

The UI selects the tab matching the opened tournament phase. This avoids retaining a previous
tournament tab when switching from Tournament Home.

Pool generation uses manual seed order, balanced snake distribution, and round-robin bouts. The
existing 5--7 pool behavior is retained; selecting a maximum of 8 explicitly permits eight-person
pools and lets sixteen fencers form two 8-person pools. Dragging a seed row delegates to
`TournamentService.moveSeedFencer`, which validates and updates the pre-pool seeding before the
view is refreshed; `generatePools` then uses that service-owned order. Overall placing uses victory ratio,
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
