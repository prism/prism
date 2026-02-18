# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Prism is an event logging and grief management plugin for Paper-based Minecraft servers (1.21.4+). It tracks 40+ in-game action types (block break/place, entity events, player actions, etc.) and provides rollback, restore, and preview capabilities. This is v4, a complete rewrite.

## Build Commands

```bash
# Build all modules
./gradlew build

# Run checkstyle linting (required before PRs)
./gradlew check

# Run a dev Minecraft server with the plugin loaded
./gradlew -p prism-paper-loader runServer

# Check for dependency updates
./gradlew dependencyUpdates

# Optional: format markdown/config files (requires pnpm)
pnpm run format
```

There are no unit tests in this project. The `check` task runs Checkstyle only.

## Module Architecture

Six Gradle modules with a layered separation between API, core logic, platform implementation, and loader:

```
prism-api          → Public interfaces for third-party consumers (actions, services, storage)
prism-paper-api    → Paper-specific API extensions
prism-core         → Shared implementation (storage adapters, action registry, caching)
prism-loader       → Platform-agnostic bootstrap infrastructure
prism-paper        → Main Paper plugin (commands, listeners, services, DI wiring)
prism-paper-loader → Lightweight Paper loader entry point, runs dev server
```

**Dependency flow**: `prism-api` ← `prism-core` ← `prism-paper` ← `prism-paper-loader`. The API modules define contracts; implementations live in core and paper modules.

## Key Architectural Patterns

**Dependency Injection**: Google Guice with constructor `@Inject`. Services are defined as interfaces in `prism-api` with implementations in `prism-core` or `prism-paper`. Factory pattern via `guice-assistedinject` for complex object creation.

**Event Recording Pipeline**: Bukkit event listeners in `prism-paper/listeners/` (organized by category: `block/`, `entity/`, `player/`, `hanging/`, `vehicle/`, etc.) → `RecordingService` → `StorageAdapter` → database.

**Storage Layer**: `StorageAdapter` interface with SQL implementations (MySQL, MariaDB, PostgreSQL, H2, SQLite). Uses jOOQ for type-safe query building, HikariCP for connection pooling, and P6Spy for query profiling. Database objects in `prism-core/storage/dbo/`.

**Command System**: TriumphTeam command library with `@Command`, `@Permission`, `@CommandFlags`, and `@NamedArguments` annotations. Commands in `prism-paper/commands/`.

**Translation**: Moonshine library for translatable text components (requires `-parameters` compiler flag, configured in build).

## Code Style

- **Java 21** target
- **Checkstyle** enforced (Google-style variant, see `config/checkstyle/checkstyle.xml`)
- **Lombok** used extensively: `@Getter`, `@SuperBuilder`, `@Inject`, etc.
- All Java files require a GPL-3 license header (some MIT files from LuckPerms are also present)
- Import order: static imports first, then third-party packages, alphabetically sorted with blank line between groups
- Annotations must be on their own lines (never inline with declarations)
- Dot operators wrap to new line; commas stay at end of line

## Dependency Management

All dependency versions are centralized in `gradle.properties` as `deps.*` entries. All dependencies are relocated to `org.prism_mc.prism.libs.*` in the final JAR to avoid classpath conflicts with other plugins.

## Dev Server Setup

Plugin dependencies must be placed in `prism-paper-loader/run/plugins/`:
- **Required**: NBT-API plugin
- **Optional**: WorldEdit, LuckPerms

Version is derived from git tags (`vX.Y.Z` format, `v` prefix stripped). Commits after a tag get a `-devN` suffix.
