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
collection, invokes rule calculators, and autosaves after successful mutations.

`domain.Tournament` is the aggregate root. It protects registration, seeding, pool, and DE phase
invariants. `domain.rules` contains pool generation, standings, bracket generation, and final
placement calculations.

`persistence.JsonTournamentRepository` reconstructs a `Tournament` through its constructor so
persisted data must satisfy domain invariants.

## Key decisions

Tournament names are unique case-insensitively after trimming. UUIDs remain internal identities for
reliable references and persistence.

The phase is derived from aggregate state rather than persisted separately:

`REGISTRATION → SEEDING → POOL_PHASE → ELIMINATION_PHASE → COMPLETE`.

The UI selects the tab matching the opened tournament phase. This avoids retaining a previous
tournament tab when switching from Tournament Home.

Pool generation uses manual seed order, balanced snake distribution, and round-robin bouts. Overall
placing uses victory ratio, indicator, touches scored, and original seed. The top 16 Pool Result
places advance to a next-power-of-two DE bracket with automatic byes for higher seeds.

## Autosave and correction

The application composition root configures `tournaments/` as the autosave directory.
`TournamentService.mutate` performs the domain action first and saves the full local tournament
collection only if the operation succeeds. Failed saves raise `TournamentPersistenceException` and
are shown in the UI without discarding the in-memory state.

JSON writes use a temporary file followed by replacement. Editing a pool result may reset DE.
Editing a DE result preserves later results when the winner is unchanged; otherwise only the
dependent downstream path is invalidated after confirmation.

## Testing

JUnit 5 tests cover registration invariants, pools, score validation, standings tie-breaks,
brackets/byes, final standings, JSON persistence, autosave/reload, corrections/invalidation, and
selected JavaFX controller behavior. Run the suite with:

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
