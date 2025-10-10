package com.codename1.server.mcp;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class McpApplicationTest {

  @Test
  void mainDelegatesToSpringApplication() {
    String[] args = {"--debug"};
    try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
      McpApplication.main(args);
      spring.verify(() -> SpringApplication.run(McpApplication.class, args));
    }
  }
}
