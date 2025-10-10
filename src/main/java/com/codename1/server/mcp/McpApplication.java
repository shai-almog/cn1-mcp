package com.codename1.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * Entrypoint for the Codename One MCP Spring Boot application.
 */
public class McpApplication {

    private static final Logger LOG = LoggerFactory.getLogger(McpApplication.class);

    public static void main(String[] args) {
        LOG.info("Starting Codename One MCP application with {} arguments",
                args == null ? 0 : args.length);
        SpringApplication.run(McpApplication.class, args);
    }
}
