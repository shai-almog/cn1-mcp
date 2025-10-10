package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.ExplainResponse;
import com.codename1.server.mcp.dto.Snippet;
import com.codename1.server.mcp.dto.SnippetsResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {
  private static final Logger LOG = LoggerFactory.getLogger(SnippetService.class);
  private final Map<String, List<Snippet>> db;

  public SnippetService(ResourcePatternResolver resolver) {
    this.db = loadSnippets(resolver);
    LOG.info("Loaded {} snippet topics from classpath", db.size());
  }

  public SnippetsResponse get(String topic) {
    var snippets = db.getOrDefault(normalize(topic), List.of());
    LOG.info("Fetching snippets for topic {} -> {} matches", topic, snippets.size());
    return new SnippetsResponse(snippets);
  }

  public ExplainResponse explain(String ruleId) {
    var response =
        switch (ruleId) {
          case "CN1_EDT_RULE" -> new ExplainResponse(
              "UI changes must run on the Event Dispatch Thread (EDT).",
              "form.show(); // anywhere",
              "Display.getInstance().callSerially(() -> form.show());");
          case "CN1_FORBIDDEN_IMPORT" -> new ExplainResponse(
              "AWT/Swing/JavaFX are not supported on CN1.",
              "import javax.swing.JButton; new JButton();",
              "import com.codename1.ui.Button; new Button();");
          default -> new ExplainResponse("No summary for " + ruleId, "", "");
        };
    LOG.info("Explain lookup for rule {} -> summaryLength={}", ruleId, response.summary().length());
    return response;
  }

  private Map<String, List<Snippet>> loadSnippets(ResourcePatternResolver resolver) {
    var map = new LinkedHashMap<String, List<Snippet>>();
    try {
      Resource[] resources = resolver.getResources("classpath*:static/docs/snippets/**/*.md");
      Arrays.sort(
          resources,
          (a, b) -> {
            try {
              return a.getURL().toString().compareTo(b.getURL().toString());
            } catch (IOException e) {
              String aName = a.getFilename();
              String bName = b.getFilename();
              if (aName == null || bName == null) {
                // SpotBugs: fall back to stable ordering even when filenames are missing.
                return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
              }
              return aName.compareTo(bName);
            }
          });

      for (Resource resource : resources) {
        parseResource(resource)
            .ifPresent(
                entry -> {
                  var topicKey = normalize(entry.topic);
                  map.computeIfAbsent(topicKey, k -> new ArrayList<>()).add(entry.snippet);
                });
      }
    } catch (IOException e) {
      LOG.error("Failed to enumerate snippet resources", e);
    }
    return map;
  }

  private record SnippetEntry(String topic, Snippet snippet) {}

  private java.util.Optional<SnippetEntry> parseResource(Resource resource) {
    try (var reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      var raw = reader.lines().collect(Collectors.joining("\n"));
      raw = stripBom(raw);
      var normalized = raw.replace("\r\n", "\n").trim();
      if (!normalized.startsWith("---\n")) {
        LOG.warn("Snippet {} is missing front matter", resource.getFilename());
        return java.util.Optional.empty();
      }
      int end = normalized.indexOf("\n---\n");
      if (end < 0) {
        LOG.warn("Snippet {} front matter is not terminated", resource.getFilename());
        return java.util.Optional.empty();
      }
      String frontMatter = normalized.substring(4, end);
      String body = normalized.substring(end + 5).trim();
      var metadata = parseFrontMatter(frontMatter);
      var topic = metadata.get("topic");
      var title = metadata.get("title");
      var description = metadata.get("description");
      if (topic == null || title == null || description == null) {
        LOG.warn(
            "Snippet {} missing required metadata (topic/title/description)",
            resource.getFilename());
        return java.util.Optional.empty();
      }
      return java.util.Optional.of(new SnippetEntry(topic, new Snippet(title, description, body)));
    } catch (IOException e) {
      LOG.warn("Failed to parse snippet {}", resource.getFilename(), e);
      return java.util.Optional.empty();
    }
  }

  private Map<String, String> parseFrontMatter(String frontMatter) {
    var metadata = new LinkedHashMap<String, String>();
    for (String line : frontMatter.split("\n")) {
      var trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }
      int sep = trimmed.indexOf(':');
      if (sep < 0) {
        continue;
      }
      String key = trimmed.substring(0, sep).trim().toLowerCase(Locale.ROOT);
      String value = trimmed.substring(sep + 1).trim();
      metadata.put(key, value);
    }
    return metadata;
  }

  private String stripBom(String text) {
    if (text.startsWith("\uFEFF")) {
      return text.substring(1);
    }
    return text;
  }

  private String normalize(String topic) {
    return topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
  }
}
