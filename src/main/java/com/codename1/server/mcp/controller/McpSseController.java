package com.codename1.server.mcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpSseController {
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        SseEmitter emitter = new SseEmitter(0L); // never timeout
        // send a greeting / capabilities event
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(mapper.writeValueAsString(Map.of(
                            "jsonrpc","2.0",
                            "method","server/ready",
                            "params", Map.of("tools", List.of(
                                    Map.of("name","cn1_lint_code","description","Lint Java for Codename One","input_schema",Map.of("type","object","properties",Map.of("code",Map.of("type","string")))),
                                    Map.of("name","cn1_compile_check","description","Verify code compiles in Codename One","input_schema",Map.of("type","object","properties",Map.of("files",Map.of("type","array"))))
                            ))
                    ))));
        } catch (IOException e) { emitter.completeWithError(e); }
        return emitter;
    }
}