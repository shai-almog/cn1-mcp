package com.codename1.server.mcp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class McpSseControllerTest {

  @Test
  void connectSendsReadyEvent() throws Exception {
    try (MockedConstruction<SseEmitter> construction = mockConstruction(SseEmitter.class)) {
      McpSseController controller = new McpSseController();
      ObjectMapper mapper = new ObjectMapper();
      ReflectionTestUtils.setField(controller, "mapper", mapper);

      controller.connect();

      SseEmitter emitter = construction.constructed().getFirst();
      verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }
  }

  @Test
  void connectCompletesWithErrorWhenSendFails() throws Exception {
    try (MockedConstruction<SseEmitter> construction =
        mockConstruction(
            SseEmitter.class,
            (mock, context) ->
                doThrow(new IOException("boom")).when(mock).send(any(SseEmitter.SseEventBuilder.class)))) {
      McpSseController controller = new McpSseController();
      controller.connect();
      SseEmitter emitter = construction.constructed().getFirst();
      verify(emitter).completeWithError(any(IOException.class));
    }
  }
}
