package com.codename1.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Entry point that defaults the application to the STDIO transport profile. */
public final class StdIoMcpApplication {

  private static final Logger LOG = LoggerFactory.getLogger(StdIoMcpApplication.class);
  private static final String STDIO_PROFILE = "stdio";

  private StdIoMcpApplication() {}

  /** Launches the MCP server with the {@code stdio} Spring profile active. */
  public static void main(String[] args) {
    LOG.info(
        "Starting Codename One MCP STDIO application with {} arguments",
        args == null ? 0 : args.length);
    new SpringApplicationBuilder(McpApplication.class)
        .web(WebApplicationType.NONE)
        .profiles(STDIO_PROFILE)
        .run(args);
  }
}
