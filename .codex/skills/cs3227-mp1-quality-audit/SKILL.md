---
name: cs3227-mp1-quality-audit
description: Perform an on-demand CS3227 MP1 release and submission audit of this repository, covering features, code, documentation, tests, AI-assisted development evidence, logs, and repository compliance.
---

# CS3227 MP1 Quality Audit

Use this skill only when explicitly invoked for a quality or final-submission audit. It is an audit-first workflow, not a default development check and not permission to push, rewrite Git history, or make broad refactors.

## Operating modes

Support both requests:

- `Use the cs3227-mp1-quality-audit skill.` — perform a comprehensive audit and report findings before making broad changes.
- `Use the cs3227-mp1-quality-audit skill for a final submission check.` — perform the strict release audit and finish with `READY`, `NEEDS ATTENTION`, and `BLOCKING SUBMISSION`.

In either mode, begin with inspection. Do not silently change reflections, fabricate development history, alter Git history, push, or rewrite stable architecture. Make only low-risk factual/documentation fixes automatically unless the user separately authorizes remediation. Ask before changes that could alter behavior, architecture, or the author's reflections.

## Repository and instructions

Read the root `AGENTS.md` completely and follow it. Inspect the current branch, `git status --short`, repository name/configuration, `.gitignore`, tracked files, and required paths. Distinguish pre-existing user changes and tournament data from changes made by the audit. Never use destructive cleanup commands.

Required deliverables to verify:

- source code
- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`
- `docs/Reflections.md`
- `logs/`

Do not assume the product matches old prompts or documentation. Inspect the current source, tests, resources, and persistence data.

## Audit procedure

### Feature and workflow audit

Trace the real end-to-end workflow: Tournament Home, create/open/delete, Setup roster and seed order, pool generation, pool matrices and result editing, Pool Result, DE generation/tableau/results/editing, completion/final results, navigation back to Setup/reset confirmation, autosave, theme/appearance, and reload. Inspect validation, empty states, byes, incomplete/invalid transitions, multiple tournaments, and dead ends. Identify implemented, partial, broken, stale-documented, and undocumented user-facing behavior. Do not invent features to increase a score.

### Code-quality audit

Review production code for readability, naming, duplication, method/class size, magic values, stale/dead code, exception handling, validation consistency, and public API clarity. Check the intended layering:

`JavaFX UI/controllers → application/service → domain/rules → repository/persistence`

Pay particular attention to UI/domain coupling, persistence boundaries, downstream invalidation, autosave atomicity, and theme/token consistency. Run any configured formatter, checkstyle, static analysis, or equivalent checks; if none exists, say so. Avoid cosmetic refactors near submission.

### Documentation audit

Check `docs/UserGuide.md` against the actual UI, terminology, validation rules, setup/running/testing instructions, and peer-tester workflow. Check `docs/DeveloperGuide.md` against the current architecture, persistence, workflows, testing approach, design decisions, and acknowledgement section. Check `docs/Reflections.md` for at least three detailed, evidence-supported AI-assisted development examples; do not write personal feelings or claims on the author's behalf. Suggest questions or structures where evidence is insufficient.

### Tests and SWE-practice audit

Inspect the complete test suite and classify meaningful coverage gaps: workflows, validation, persistence/reload, multiple-tournament isolation, score editing/invalidation, completion, setup reset, themes, and UI state/event regressions. Run the complete suite and report exact test totals, failures, and environmental limitations. Do not add meaningless count-inflating tests.

### AI-interaction logs audit

Inspect every useful file under `logs/`. Check whether significant interactions record the task, prompt decisions, assumptions, outcome, failures, verification, and follow-up. Use repository evidence only when improving factual summaries. Flag major missing history and distinguish generated suggestions from the author's own reflection.

### Submission-compliance audit

Verify project structure, required documentation paths, logs, build/launch viability where practical, test status, tracked build/distribution artifacts, secrets, absolute machine-specific paths, TODO/FIXME/debug output, placeholder text, accidental demo data, `.gitignore`, and branch/repository state when available. Do not push or change Git history.

## Findings and priorities

Classify every finding:

- **CRITICAL** — broken functionality, data corruption, failing build/tests, submission violation, or materially misleading UserGuide.
- **HIGH** — major missing tests, stale architecture documentation, serious separation/error-handling issue, or missing reflection/log evidence.
- **MEDIUM** — maintainability, documentation, UX, or validation inconsistencies with meaningful impact.
- **LOW** — cosmetic or low-impact style issues.

Report:

1. Executive summary
2. Rubric assessment: Features 20%, Code Quality 25%, Documentation 10%, Basic SWE Practices 20%, AI-assisted Reflection 25%
3. Critical/high/medium/low findings with evidence and file paths
4. Estimated grading risks (qualitative; do not invent a precise grade)
5. Files/components affected
6. Prioritized remediation plan
7. Items requiring the user's personal input or verification
8. Changes safe for Codex to make automatically

For final mode, end with a concise checklist:

- `READY`
- `NEEDS ATTENTION`
- `BLOCKING SUBMISSION`

State what evidence supports each item. If a GUI smoke test cannot be performed in the environment, say exactly what remains manual.

## Safe remediation boundary

After the audit report, wait for authorization before broad fixes. If explicitly authorized, make narrowly scoped changes, preserve unrelated work, add/update meaningful tests, run the relevant/full suite, and re-audit affected documentation. Never silently rewrite `docs/Reflections.md`; only make factual structural or spelling fixes that do not claim personal experience, or ask the user for the missing content.
