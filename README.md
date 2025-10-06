## CN1 MCP
Basic MCP optimized for Linux at this time.

It can run in two modes:

* Server - untested. Requires deployment in the cloud and pointing the LLM/Agent/IDE to the URL `/mcp`
* Local STDIO - tested with Claude Desktop see notes below

Currently requires JDK 25 to run. To test it with Claude Desktop edit the file `claude_desktop_config.json` on your machine to the following:

```json
{
  "mcpServers": {
    "CodenameOneServer": {
      "command": "/path/to/java/25/java",
      "args": [
        "-Dorg.springframework.boot.logging.LoggingSystem=none",
        "-Dloader.main=com.codename1.server.stdiomcp.StdIoMcpMain",
        "-Dspring.profiles.active=stdio",
        "-jar",
        "/path/to/mcp-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```
