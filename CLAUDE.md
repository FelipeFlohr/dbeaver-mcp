# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DBeaver MCP is a Spring Boot 4.0.2 application that provides MCP (Model Context Protocol) server capabilities for DBeaver database connections. It reads DBeaver's configuration files to discover database connections and allows executing read-only SQL queries against them.

## Supported Databases

Currently three databases are implemented:

| Database   | Compatibility | Driver         |
| ---------- | ------------- | -------------- |
| PostgreSQL | Any version   | org.postgresql |
| Oracle     | 11+           | ojdbc11        |
| Firebird   | 2.5+          | Jaybird 6      |

To add support for a new database:

1. Add the enum value in `DatabaseType` with the provider name and driver class
2. Create a `*HikariDriverConfig` implementation in `connection/factory/config/driver/`
3. Create a `*QueryServiceImpl` extending `GenericQueryServiceImpl` in `query/service/`
4. Register the new `QueryService` bean in `QueryServiceFactory`

## Build Commands

All commands should be run from the `dbeaver-mcp/` directory using the Gradle wrapper.

## Architecture

The codebase follows a modular structure under `dev.felipeflohr.dbeavermcp.module`:

- **mcp**: MCP service layer exposing tools via `@McpTool` annotations
  - `QueryMCPService`: Exposes `list_available_connections` and `execute_read_only_query` tools

- **query**: Query execution services with database-specific implementations
  - `QueryService` interface with `PostgresQueryServiceImpl`, `OracleQueryServiceImpl`, `FirebirdQueryServiceImpl`
  - `QueryServiceFactory`: Routes queries to appropriate implementation based on connection type

- **connection**: Database connection pooling using HikariCP
  - `ConnectionPoolManager`: Creates DataSources from DBeaver connection names
  - Driver-specific configurations in `factory/config/driver/`

- **dbeaver**: DBeaver configuration file parsing
  - `DBeaverFilesService`: Locates DBeaver config files (data-sources.json, credentials-config.json)
  - `DBeaverDataSourceService`: Parses connection configurations
  - `DBeaverCipherService`: Decrypts DBeaver stored credentials

- **system**: OS detection for locating DBeaver config paths

## Key Patterns

- Interfaces with `*Impl` naming convention for implementations
- `@NullMarked` annotations for null-safety
- Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Getter`) for boilerplate reduction
- Spring constructor injection (no `@Autowired`)

## Testing

Tests use Testcontainers with Oracle, PostgreSQL, and Firebird containers. The `TestcontainersConfiguration` class sets up database containers and mock DBeaver configurations.

Key test utilities:

- `MockDBeaverFilesUtil`: Creates mock DBeaver config files
- `TestcontainersService`: Helper for test database operations
- `AssertionUtil`: Database-agnostic assertion helpers

## Configuration

Application properties in `src/main/resources/application.properties`:

- `dbeavermcp.dbeaver.config.data-sources-file-path`: Override DBeaver data sources path
- `dbeavermcp.dbeaver.config.credentials-config-file-path`: Override DBeaver credentials path
- `dbeavermcp.dbeaver.cipher.key/iv`: DBeaver credential decryption keys

The application runs in STDIO mode (`spring.ai.mcp.server.stdio=true`) with virtual threads enabled.

## Build & Release

The `bootJar` task generates `dbeaver-mcp.jar` (configured in `build.gradle` with no version suffix). Run from the `dbeaver-mcp/` directory:

```bash
./gradlew bootJar
```

### Distribution Scripts

The `scripts/` directory contains launcher scripts for end users:

- `run.sh` (Linux): Downloads Adoptium JRE 25 on first run, then executes the jar
- `run.bat` (Windows): Same behavior using PowerShell for download/extraction

Both scripts accept application parameters via command-line arguments (e.g., `./run.sh --dbeavermcp.dbeaver.config.data-sources-file-path=/path`).

### Release Workflow

The GitHub Actions workflow (`.github/workflows/release.yml`) triggers on pre-release creation:

1. Builds the jar with `./gradlew bootJar`
2. Packages `dbeaver-mcp-linux.tar.gz` (jar + `run.sh`) and `dbeaver-mcp-windows.zip` (jar + `run.bat`)
3. Uploads both artifacts to the GitHub release
4. Promotes the pre-release to a full release
