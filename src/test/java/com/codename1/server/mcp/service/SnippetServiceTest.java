package com.codename1.server.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.codename1.server.mcp.dto.ExplainResponse;
import com.codename1.server.mcp.dto.SnippetsResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

class SnippetServiceTest {

  @Test
  void loadsSnippetsAndSupportsExplainQueries() throws Exception {
    ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
    Resource valid = byteResource("lint.md", """
            ---
            topic: Lint
            title: EDT rule
            description: Explains EDT rule
            ---
            Body text
            """);
    Resource invalid = byteResource("invalid.md", "no front matter");
    doReturn(new Resource[] {valid, invalid})
        .when(resolver)
        .getResources("classpath*:static/docs/snippets/**/*.md");

    SnippetService service = new SnippetService(resolver);

    SnippetsResponse response = service.get(" lint ");
    assertThat(response.snippets()).hasSize(1);
    assertThat(response.snippets().getFirst().description()).contains("EDT");

    ExplainResponse explain = service.explain("CN1_EDT_RULE");
    assertThat(explain.summary()).contains("Event Dispatch Thread");

    ExplainResponse fallback = service.explain("unknown");
    assertThat(fallback.summary()).contains("No summary");
  }

  private static Resource byteResource(String name, String text) {
    return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8), name) {
      @Override
      public String getFilename() {
        return name;
      }
    };
  }
}
