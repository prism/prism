# Contributing

We welcome PRs or bug reports/features requests through our github repository.

## Repository

Prism is broken out into several modules:

The API:

- `prism-api` - The Prism interfaces and core classes that may be of used to 3rd party API users.
- `prism-paper-api` - The paper-specific Prism API classes.

The core:

- `prism-core` - The core code shared by all plugins.

The loaders:

- `prism-loader` - The core code shared by all loader plugins.
- `prism-paper-loader` - Minimal code for a "loader" plugin that bootstraps prism on paper.

The Paper Plugin:

- `prism-paper` - The core code for the paper-based platform version of Prism.

This approach was chosen to allow future development of prism for other platforms.

A lot of `services` and related code inside the paper module will be eventually moved to core.

## Setup Databases

If testing with sqlite/h2, you don't need to do anything. Sqlite is the default for prism.

If working in MySQL/MariaDB/Postgres, install any or all.

We recommend using [docker](https://www.docker.com/) and the official images for each. This allows you to
start/stop/destroy/etc each service as needed.

## Setup Server

Install the required and any relevant plugins into `prism-paper-loader/run/plugins`

1. Required: [NBT-API](https://modrinth.com/plugin/nbtapi)
2. Optional: [LuckPerms](https://luckperms.net/) - For testing permissions
3. Optional: [WorldEdit](https://enginehub.org/worldedit) - For testing WorldEdit integration

## Development

Configure IntelliJ to run the `prism-paper-loader - runServer` task. It will build the plugin and run the server with it. 

Or, use gradle: `./gradlew -p prism-paper-loader runServer`.

The version of the paper server used can be set via `paper-server-version` in `gradle.properties`.

Server files and plugin configs will be inside `prism-paper-loader/run`.

## Pull Requests

Any changes to the repository by contributors should be opened as a Pull Request.

1. Look for any existing issues or discuss your ideas in our discord.
2. Fork the repo and create your branch from whichever branch you're working against.
3. Make sure your code lints (`./gradlew check`).
4. Open that pull request!

## Prettier Formatting

Prettier is an automatic code-formatting tool. Our repo will run this automatically when you open a PR, but it can be more convenient to run it yourself.

1. Install [NodeJS](https://nodejs.org) and [PNPM](https://pnpm.io/).
2. Run `pnpm install` once.
3. Run `pnpm run format` before opening your PR or making updates.

## Updating Dependencies

Dependency updates should be tested thoroughly. Some dependencies are downloaded by Prism at runtime
and require updating hashes for security.

To check which gradle dependencies have updates, use:

- `./gradlew dependencyUpdates`
