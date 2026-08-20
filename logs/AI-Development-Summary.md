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
