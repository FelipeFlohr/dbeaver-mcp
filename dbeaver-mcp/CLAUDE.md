# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DBeaver MCP Server - A Spring Boot application that bridges DBeaver database configurations with the Model Context Protocol (MCP). It reads DBeaver datasource definitions, decrypts stored credentials, and allows AI models to execute SQL queries against configured database connections.

## Build Commands

```bash
./gradlew build                  # Build the project
./gradlew test                   # Run all tests
./gradlew test --tests "*.ClassName"  # Run specific test class
./gradlew test --tests "*.ClassName.methodName"  # Run single test method
./gradlew nativeCompile          # Compile to native image (requires GraalVM 25+)
./gradlew nativeTest             # Run tests in native image
./gradlew bootBuildImage         # Build Docker container
./gradlew bootRun                # Run the application
```

## Architecture

The application follows a modular architecture with these key modules:

### Module Structure

- **module.dbeaver** - DBeaver integration (reads config files, decrypts credentials)
- **module.connection** - Database connection pool management via HikariCP
- **module.query** - SQL query execution and result handling
- **module.system** - OS detection for platform-specific paths

### Key Services

**DBeaverFilesService** - Resolves DBeaver config file paths based on OS:
- Windows: `%APPDATA%/DBeaverData/workspace6/General/.dbeaver/`
- Mac: `~/Library/DBeaverData/workspace6/General/.dbeaver/`
- Linux: `~/.local/share/DBeaverData/workspace6/General/.dbeaver/`

**DBeaverCipherService** - Decrypts `credentials-config.json` using AES/CBC/PKCS5Padding with configurable key/IV.

**ConnectionPoolManager** - Creates and caches HikariDataSource instances per connection (keyed by `provider|jdbcUrl|username`).

**HikariConfigFactory** - Factory pattern with driver-specific implementations:
- `PostgresHikariDriverConfig`
- `OracleHikariDriverConfig`
- `FirebirdHikariDriverConfig`

**QueryService** - Executes SQL statements against named connections, returns results as `List<Map<String, Object>>`.

### Data Flow

1. `DBeaverDataSourceService` reads `data-sources.json` → parsed connection definitions
2. `DBeaverCipherService` decrypts `credentials-config.json` → username/password per connection
3. `ConnectionPoolManager` creates HikariCP pool for requested connection
4. `QueryService` executes SQL using the pooled connection

## Technology Stack

- Java 25 with virtual threads enabled
- Spring Boot 4.0.2 with Spring AI MCP Server
- HikariCP for connection pooling
- Database drivers: PostgreSQL, Oracle JDBC 11, Firebird Jaybird 6.0.4
- GraalVM native image support
- TestContainers (Oracle Free, PostgreSQL) for integration tests

## Configuration

Key properties in `application.properties`:
- `spring.ai.mcp.server.stdio=true` - MCP communication over stdio
- `dbeavermcp.dbeaver.config.data-sources-file-path` - Override datasources path
- `dbeavermcp.dbeaver.config.credentials-config-file-path` - Override credentials path
- `dbeavermcp.dbeaver.cipher.key` / `dbeavermcp.dbeaver.cipher.iv` - Encryption keys

## Testing

Tests use TestContainers for real database integration testing. Test resources in `src/test/resources/` include sample DBeaver config files (`data-sources.json`, `credentials-config.json`).

`MockDBeaverFilesUtil` provides utilities for mocking DBeaver file paths in tests.
