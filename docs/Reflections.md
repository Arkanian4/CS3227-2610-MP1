# Reflection Evidence and Prompts

This file deliberately does not claim personal reflections on behalf of the developer. It records
verifiable evidence and questions that can support a later individual reflection on AI-assisted
software engineering.

## Evidence available

- Git history records incremental milestones for pools, DE, final results, multiple tournaments,
  score correction, autosave, and UI bug fixes.
- `logs/AI-Development-Summary.md` records major task decisions and notable issues.
- Tests include examples where reported defects became regression tests, including duplicate names,
  DE correction, autosave/reload, and phase-appropriate tab selection.
- The source records boundaries between UI, application orchestration, domain rules, and persistence.

## Prompts for the developer

1. Which prompts had sufficiently precise acceptance criteria, and which allowed ambiguity?
2. Where did iterative UI feedback improve the product, and where did it create rework?
3. Which AI-generated assumptions were incorrect and how were they detected?
4. How did automated tests change confidence in score editing and autosave?
5. Which architectural decisions required personal judgement instead of accepting generated code?
6. What code-review practices would reduce large, dense UI/controller classes next time?

## Candidate concrete examples

- Duplicate fencer-name requirements changed during development and required a domain rule and
  regression test.
- Pool and DE result editing required explicit downstream invalidation rather than stale results.
- Autosave exposed a JSON serialisation issue with derived properties; reload tests protected the
  fix.
- Opening a tournament in another phase exposed stale JavaFX tab state and led to phase-based tab
  selection.
