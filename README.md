# CN1 MCP

CN1 MCP is a Spring Boot based Model Context Protocol (MCP) server that packages Codename One tooling so it can be consumed by modern AI assistants and IDE agents. The project ships both native binaries (built with GraalVM) and a cross-platform JVM bundle so you can run it on macOS, Windows, or Linux in STDIO or HTTP server modes.

## Download the latest release

Every published release automatically attaches the following ready-to-run artifacts:

| Platform | Artifact name | Description |
| --- | --- | --- |
| Linux (x86_64) | [`cn1-mcp-stdio-linux`](../../releases/latest/download/cn1-mcp-stdio-linux) | Native GraalVM binary for Linux that communicates over STDIO. |
| macOS (arm64/x86_64) | [`cn1-mcp-stdio-macos`](../../releases/latest/download/cn1-mcp-stdio-macos) | Native GraalVM binary for macOS that communicates over STDIO. |
| Windows (x86_64) | [`cn1-mcp-stdio-windows.exe`](../../releases/latest/download/cn1-mcp-stdio-windows.exe) | Native GraalVM binary for Windows that communicates over STDIO. |
| Any platform with Java 21+ | [`cn1-mcp-stdio.jar`](../../releases/latest/download/cn1-mcp-stdio.jar) | Cross-platform executable JAR that can run the STDIO or HTTP server profiles. |

> **Tip:** The `releases/latest/download/...` URLs always resolve to the freshest release, so you can script downloads without hard-coding version numbers.

## Quick start

### Native STDIO binaries (macOS & Linux)

1. Download the binary for your platform from the table above.
2. Mark it as executable: `chmod +x ./cn1-mcp-stdio-linux` or `chmod +x ./cn1-mcp-stdio-macos`.
3. Run it directly. The process reads STDIN and writes STDOUT/STDERR in the MCP protocol:
   ```bash
   SPRING_PROFILES_ACTIVE=stdio ./cn1-mcp-stdio-linux
   ```
4. Configure your MCP-compatible client to launch the binary (see integration guides below).

### Native STDIO binary (Windows)

1. Download `cn1-mcp-stdio-windows.exe`.
2. Place it somewhere stable (e.g., `C:\Tools\cn1-mcp\cn1-mcp-stdio-windows.exe`).
3. Run it from a terminal or allow your MCP client to spawn it:
   ```powershell
   set SPRING_PROFILES_ACTIVE=stdio
   .\cn1-mcp-stdio-windows.exe
   ```

### Cross-platform JVM package

1. Install Java 21 or newer (Temurin distributions work well on all platforms).
2. Download `cn1-mcp-stdio.jar` from the latest release.
3. Launch it in STDIO mode with the packaged Spring Boot launcher:
   ```bash
   java \
     -Dorg.springframework.boot.logging.LoggingSystem=none \
     -Dloader.main=com.codename1.server.stdiomcp.StdIoMcpMain \
     -Dspring.profiles.active=stdio \
     -jar cn1-mcp-stdio.jar
   ```
4. For HTTP server mode, change `-Dspring.profiles.active=stdio` to `server` and provide an application port via `SERVER_PORT` or `--server.port` if required.

## MCP client integration guide

The MCP ecosystem is still evolving, but CN1 MCP has been validated with a range of agents and IDEs. The following sections walk through the most common setups. Each assumes you have already downloaded one of the binaries above.

### Claude Desktop (macOS & Windows)

1. Open `Claude Desktop` and choose **Settings → MCP**.
2. Click **Add MCP Server** and choose **Custom**.
3. Enter a unique name, e.g., `CodenameOneServer`.
4. Set **Command** to the path of the native binary (recommended) or your Java executable. Examples:
   * macOS/Linux native: `/Users/<you>/Tools/cn1-mcp-stdio-macos`
   * Windows native: `C:\\Tools\\cn1-mcp\\cn1-mcp-stdio-windows.exe`
   * JVM: `/usr/lib/jvm/temurin-21/bin/java`
5. If you choose the JVM option, add the following arguments exactly (adjust the jar path if you store it elsewhere):
   ```json
   [
     "-Dorg.springframework.boot.logging.LoggingSystem=none",
     "-Dloader.main=com.codename1.server.stdiomcp.StdIoMcpMain",
     "-Dspring.profiles.active=stdio",
     "-jar",
     "/path/to/cn1-mcp-stdio.jar"
   ]
   ```
6. Save the configuration. Claude will spawn the process and show its health status in the MCP settings page.

### Cursor IDE

1. Update to Cursor v0.45 or later (MCP support is in beta builds).
2. Open **Settings → MCP Servers**.
3. Click **Add Server** and choose **Executable**.
4. Point Cursor to the platform-specific binary from the latest release (recommended) or to the `java` command with the arguments listed in the JVM section above.
5. Ensure **Communication Mode** is set to **STDIO**.
6. Save and restart Cursor; CN1 MCP should appear in the MCP panel and expose Codename One commands and resources.

### Continue.dev (VS Code / JetBrains)

1. Install the latest Continue extension (>= v0.10) and enable MCP features in the extension settings.
2. Locate the `continue.config.json` (VS Code) or `~/.continue/config.json` (JetBrains) file and add a new MCP entry:
   ```json
   {
     "mcpServers": {
       "cn1": {
         "command": "/absolute/path/to/cn1-mcp-stdio-linux",
         "args": [],
         "env": {
           "SPRING_PROFILES_ACTIVE": "stdio"
         }
       }
     }
   }
   ```
   Replace the command with the appropriate binary or `java` command (with the arguments shown earlier).
3. Reload Continue. The CN1 MCP tools will be available inside the sidebar and chat completions.

### Generic MCP-compatible clients

For clients that let you register a custom STDIO MCP server manually:

1. Choose the binary or jar launch command from the quick start section.
2. Ensure the environment variable `SPRING_PROFILES_ACTIVE=stdio` is set (the native binaries default to STDIO, but the variable is harmless if already set).
3. Register the command with your client, making sure it starts the process in STDIO mode.
4. Optionally, set `CN1_MCP_CACHE_DIR` to customise where downloaded SDKs and cached assets are stored.

## Running CN1 MCP as an HTTP server (experimental)

While most MCP clients rely on STDIO, CN1 MCP can run as an HTTP server for cloud deployments:

1. Launch the cross-platform jar with the `server` profile:
   ```bash
   SERVER_PORT=8080 java \
     -Dorg.springframework.boot.logging.LoggingSystem=none \
     -Dloader.main=com.codename1.server.mcp.McpApplication \
     -Dspring.profiles.active=server \
     -jar cn1-mcp-stdio.jar
   ```
2. Expose the `/mcp` endpoint to your MCP client or gateway. Ensure TLS and authentication are handled by your hosting environment.

## Development

* Build the project locally with `./mvnw -B -ntp verify`.
* Native images are produced with `./mvnw -Pnative -DskipTests native:compile`.
* GitHub Actions builds run on every push, pull request, and published release. Release builds automatically attach the native binaries and cross-platform jar to the GitHub release page.

