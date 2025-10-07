package com.codename1.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpApplication {

        private static final Logger LOG = LoggerFactory.getLogger(McpApplication.class);

        public static void main(String[] args) {
                LOG.info("Starting Codename One MCP application with {} argument(s)", args.length);
                SpringApplication.run(McpApplication.class, args);
                LOG.info("Codename One MCP application started");
        }

}
