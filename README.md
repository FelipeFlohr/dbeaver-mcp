# DBeaver MCP

An [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) server that connects AI tools to your databases through [DBeaver](https://dbeaver.io/). It reads your existing DBeaver connection configurations and lets you run read-only SQL queries directly from AI assistants like Claude Code and Gemini CLI.

Built with Spring Boot and Spring AI. Communicates via STDIO.

## Supported Databases

| Database   | Compatibility | Driver         |
|------------|---------------|----------------|
| PostgreSQL | Any version   | org.postgresql |
| Oracle     | 11+           | ojdbc11        |
| Firebird   | 2.5+          | Jaybird 6      |

## Limitations

- **SSH: password authentication only.** Connections that use SSH key-based authentication (public/private key) are not supported and will be automatically filtered out. Only SSH connections configured with password authentication in DBeaver will work.
- **Read-only queries only.** All queries run inside a read-only transaction that is always rolled back. No data can be modified.
- **DBeaver connections only.** The server discovers databases from DBeaver's configuration files. You cannot provide manual connection strings.

## Getting Started

### Prerequisites

- [DBeaver](https://dbeaver.io/) installed with at least one database connection configured.
- No Java installation required — the launcher scripts automatically download a JRE on first run.

### 1. Download

Go to the [Releases](https://github.com/FelipeFlohr/dbeaver-mcp/releases) page and download the archive for your platform:

- **Linux:** `dbeaver-mcp-linux.tar.gz`
- **Windows:** `dbeaver-mcp-windows.zip`

Extract the archive to a directory of your choice. It contains two files:

```
dbeaver-mcp.jar
run.sh (Linux) / run.bat (Windows)
```

### 2. Add the MCP Server to Your AI Tool

Pick your tool below and follow the instructions.

---

#### Claude Code

##### Project scope (shared with your team via `.mcp.json`)

**Linux:**

```bash
claude mcp add --scope project --transport stdio dbeaver-mcp -- /path/to/dbeaver-mcp-linux/run.sh
```

**Windows:**

```bash
claude mcp add --scope project --transport stdio dbeaver-mcp -- cmd /c C:\path\to\dbeaver-mcp-windows\run.bat
```

##### User scope (available across all your projects)

**Linux:**

```bash
claude mcp add --scope user --transport stdio dbeaver-mcp -- /path/to/dbeaver-mcp-linux/run.sh
```

**Windows:**

```bash
claude mcp add --scope user --transport stdio dbeaver-mcp -- cmd /c C:\path\to\dbeaver-mcp-windows\run.bat
```

> Replace `/path/to/` or `C:\path\to\` with the actual path where you extracted the archive.

---

#### Gemini CLI

Add the server to your `~/.gemini/settings.json` file. Create the file if it does not exist.

**Linux:**

```json
{
  "mcpServers": {
    "dbeaver-mcp": {
      "command": "/path/to/dbeaver-mcp-linux/run.sh"
    }
  }
}
```

**Windows:**

```json
{
  "mcpServers": {
    "dbeaver-mcp": {
      "command": "cmd",
      "args": ["/c", "C:\\path\\to\\dbeaver-mcp-windows\\run.bat"]
    }
  }
}
```

> Replace the paths with the actual path where you extracted the archive.

---

### 3. Verify

Once configured, you can verify the connection by asking your AI tool to list the available database connections. For example:

```
List my available database connections.
```

## Available Tools

The server exposes two MCP tools:

| Tool                         | Description                                                                                                            |
|------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `list_available_connections` | Lists all database connections configured in DBeaver, including connection name, database type, URL, and SSH details.   |
| `execute_read_only_query`    | Executes one or more read-only SQL statements against a named connection. Results are returned as rows of column/value maps. |

## Configuration

By default, the server reads DBeaver's configuration files from the standard location for your operating system:

| OS      | Default path                                                              |
|---------|---------------------------------------------------------------------------|
| Linux   | `~/.local/share/DBeaverData/workspace6/General/.dbeaver/`                 |
| macOS   | `~/Library/DBeaverData/DBeaverData/workspace6/General/.dbeaver/`          |
| Windows | `%APPDATA%\DBeaverData\workspace6\General\.dbeaver\`                      |

If your DBeaver configuration is in a different location, you can override the paths by passing arguments to the launcher script:

```bash
./run.sh --dbeavermcp.dbeaver.config.data-sources-file-path=/custom/path/data-sources.json \
         --dbeavermcp.dbeaver.config.credentials-config-file-path=/custom/path/credentials-config.json
```

To pass these arguments through an MCP configuration, add them to the command args. For example, in Claude Code:

```bash
claude mcp add --scope user --transport stdio dbeaver-mcp -- /path/to/run.sh \
  --dbeavermcp.dbeaver.config.data-sources-file-path=/custom/path/data-sources.json
```

Or in Gemini CLI (`~/.gemini/settings.json`):

```json
{
  "mcpServers": {
    "dbeaver-mcp": {
      "command": "/path/to/dbeaver-mcp-linux/run.sh",
      "args": [
        "--dbeavermcp.dbeaver.config.data-sources-file-path=/custom/path/data-sources.json"
      ]
    }
  }
}
```

## Building from Source

Requirements: Java 25.

```bash
cd dbeaver-mcp
./gradlew bootJar
```

The output jar will be at `dbeaver-mcp/build/libs/dbeaver-mcp.jar`.
