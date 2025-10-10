package com.codename1.server.mcp.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Discovers the markdown guides bundled with the MCP server and exposes simple metadata and
 * content loading helpers so they can be surfaced via guide/intro mode.
 */
@Service
public class GuideService {
    private static final Logger LOG = LoggerFactory.getLogger(GuideService.class);
    private final List<GuideDoc> guides;
    private final Map<String, GuideDoc> guidesById;

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Fail fast when bundled guides cannot be enumerated")
    public GuideService(ResourcePatternResolver resolver) {
        List<GuideDoc> discovered = new ArrayList<>();
        try {
            for (Resource resource : resolver.getResources("classpath:/static/docs/*.md")) {
                if (resource == null || !resource.exists()) {
                    continue;
                }
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                String id = toId(filename);
                String title = toTitle(filename);
                String description = title + " (Codename One guide)";
                GuideDoc guide = new GuideDoc(id, title, description, resource);
                LOG.debug("Registered guide {} from resource {}", id, resource);
                discovered.add(guide);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to discover bundled guides", e);
        }

        discovered.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
        this.guides = Collections.unmodifiableList(discovered);
        Map<String, GuideDoc> map = new LinkedHashMap<>();
        for (GuideDoc guide : discovered) {
            map.put(guide.id(), guide);
        }
        this.guidesById = Collections.unmodifiableMap(map);
        LOG.info("Discovered {} bundled guide(s)", this.guides.size());
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Guides list is created as an unmodifiable snapshot")
    public List<GuideDoc> listGuides() {
        return guides;
    }

    public Optional<GuideDoc> findGuide(String id) {
        return Optional.ofNullable(guidesById.get(id));
    }

    public String loadGuide(String id) throws IOException {
        GuideDoc guide = guidesById.get(id);
        if (guide == null) {
            throw new IllegalArgumentException("Unknown guide: " + id);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                guide.resource().getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String toId(String filename) {
        String base = stripExtension(filename);
        String slug = base.replaceAll("[^A-Za-z0-9]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        return slug.toLowerCase(Locale.ROOT);
    }

    private static String toTitle(String filename) {
        String base = stripExtension(filename);
        return base.replace('_', ' ').replace('-', ' ');
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    public record GuideDoc(String id, String title, String description, Resource resource) {}
}

