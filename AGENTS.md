## Module deliverables

This repository is assessed not only on functionality but also on code quality and documentation.

Keep the following deliverables synchronized with the current product:

- `docs/UserGuide.md`
  - Must describe all current user-facing features accurately.
  - Must include setup and testing instructions.
  - Do not document planned or unimplemented behavior.
  - Treat documentation inaccuracies as product bugs.

- `docs/DeveloperGuide.md`
  - Must reflect the current architecture and design.
  - Document relevant software engineering decisions/processes.
  - Maintain an acknowledgement section for reused ideas, code, or documentation.

- `docs/Reflections.md`
  - Contains the developer's reflections on AI-assisted software engineering using LLMs and prompting.
  - Do not fabricate personal reflections.
  - When useful, identify interesting development interactions that may be worth reflecting on later.

- `logs/`
  - Maintain concise summaries of significant prompts and AI-assisted development interactions.
  - Summaries should record the task, important prompt decisions, outcome, and notable issues.
  - Generated summaries must remain factual and should be easy for the developer to verify.

## Development expectations

For every significant feature or bug fix:

1. Preserve clean architecture and existing coding conventions.
2. Add/update appropriate automated tests.
3. Run the relevant tests after implementation.
4. Identify whether `UserGuide.md`, `DeveloperGuide.md`, or logs need updating.
5. Do not claim functionality in documentation that does not exist in the current product.
6. Avoid unrelated refactors unless necessary.