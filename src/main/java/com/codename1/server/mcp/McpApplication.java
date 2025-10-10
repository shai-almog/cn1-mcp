package com.codename1.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entrypoint for the Codename One MCP Spring Boot application. */
@SpringBootApplication
public class McpApplication {

  private static final Logger LOG = LoggerFactory.getLogger(McpApplication.class);

  /**
   * Bootstraps the Spring application context.
   *
   * @param args command-line arguments supplied by the JVM
   */
  public static void main(String[] args) {
    LOG.info(
        "Starting Codename One MCP application with {} arguments", args == null ? 0 : args.length);
    SpringApplication.run(McpApplication.class, args);
  }
}
