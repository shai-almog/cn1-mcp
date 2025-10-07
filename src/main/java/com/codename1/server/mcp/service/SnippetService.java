package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.ExplainResponse;
import com.codename1.server.mcp.dto.Snippet;
import com.codename1.server.mcp.dto.SnippetsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class SnippetService {
    private static final Logger LOG = LoggerFactory.getLogger(SnippetService.class);
    private static final Map<String, List<Snippet>> DB = Map.of(
            "rest", List.of(new Snippet("REST GET",
                    "Use Rest with async onComplete; no blocking on EDT.",
                    """
                    import com.codename1.io.rest.*;
                    Rest.get("https://api.example.com/items")
                        .acceptJson()
                        .onComplete(res -> {
                          // This runs off EDT; update UI via callSerially
                          com.codename1.ui.Display.getInstance().callSerially(() -> {
                             // update UI
                          });
                        }).send();
                    """)),
                "storage", List.of(new Snippet("Storage save/read",
                                                       "Use Storage or FileSystemStorage.",
                                                       """
                  import com.codename1.io.Storage;
                  byte[] data = "hello".getBytes();
                  Storage.getInstance().writeObject("greeting", data);
                  byte[] out = (byte[]) Storage.getInstance().readObject("greeting");
                  """))
            );

    public SnippetsResponse get(String topic) {
        var snippets = DB.getOrDefault(topic, List.of());
        LOG.info("Fetching snippets for topic {} -> {} matches", topic, snippets.size());
        return new SnippetsResponse(snippets);
    }

    public ExplainResponse explain(String ruleId) {
        var response = switch (ruleId) {
            case "CN1_EDT_RULE" -> new ExplainResponse(
                    "UI changes must run on the Event Dispatch Thread (EDT).",
                    "form.show(); // anywhere",
                    "Display.getInstance().callSerially(() -> form.show());"
            );
            case "CN1_FORBIDDEN_IMPORT" -> new ExplainResponse(
                    "AWT/Swing/JavaFX are not supported on CN1.",
                    "import javax.swing.JButton; new JButton();",
                    "import com.codename1.ui.Button; new Button();"
            );
            default -> new ExplainResponse("No summary for "+ruleId, "", "");
        };
        LOG.info("Explain lookup for rule {} -> summaryLength={}", ruleId, response.summary().length());
        return response;
    }
}