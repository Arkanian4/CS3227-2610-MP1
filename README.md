# Fencing Tournament Manager

A local JavaFX desktop application for organising small internal fencing club tournaments.

The application currently supports local tournament creation, fencer registration, and JSON
save/load. Pool generation, results, standings, and elimination workflows are planned for later
iterations.

## Prerequisites

- JDK 21 or later. The build targets Java 21.
- Internet access the first time Gradle resolves JavaFX and test dependencies.

## Build and test

From the project root, run:

```powershell
.\gradlew.bat clean test
```

## Run the desktop skeleton

```powershell
.\gradlew.bat run
```

The application opens the registration screen. Create or load a tournament, add/remove fencers,
and save the current tournament to a local JSON file.

## Structure

- `src/main/java/.../ui` — JavaFX application entry point and future presentation code.
- `src/main/java/.../application` — application-service and repository boundary.
- `src/main/java/.../domain` — framework-independent tournament concepts and rules.
- `src/main/java/.../persistence` — local JSON persistence implementation.
- `src/test/java` — automated tests.
