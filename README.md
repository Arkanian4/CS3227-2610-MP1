# Fencing Tournament Manager

A local JavaFX desktop application for running small internal epee tournaments in one fencing club.

It supports multiple tournaments, registration and manual seeding, balanced round-robin pools,
pool result entry and placing, a seeded direct-elimination tableau, final results, safe result
correction, and automatic local JSON saving.

## Prerequisites

- JDK 21 or later
- Internet access the first time Gradle resolves JavaFX and test dependencies

## Build and test

From the project root:

```powershell
.\gradlew.bat test
```

## Run

```powershell
.\gradlew.bat run
```

Tournament data is automatically saved to the local `tournaments/` directory after successful
changes and loaded on the next start. There is no normal manual Save action.

## Documentation

- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)
- [Reflection evidence and prompts](docs/Reflections.md)
- [AI-assisted development log](logs/AI-Development-Summary.md)

## Project structure

- `src/main/java/.../ui` — JavaFX views, controller, and application composition root
- `src/main/java/.../application` — workflow orchestration, autosave coordination, repository boundary
- `src/main/java/.../domain` — tournament aggregate, rules, pools, standings, and elimination model
- `src/main/java/.../persistence` — local JSON repository
- `src/test/java` — automated tests
