package com.codename1.server.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.Locale;

@Component
public class Jdk8ManagerFromResource {
    private static final Logger LOG = LoggerFactory.getLogger(Jdk8ManagerFromResource.class);
    private final GlobalExtractor extractor;
    private final String linuxArchiveResource;
    private final String macArchiveUrl;
    private final String windowsArchiveUrl;
    private final String rootMarker; // e.g. "release"

    @Autowired
    public Jdk8ManagerFromResource(GlobalExtractor extractor,
                                   @Value("${cn1.jdk8.linuxResourcePath:${cn1.jdk8.resourcePath}}") String linuxArchiveResource,
                                   @Value("${cn1.jdk8.macUrl:}") String macArchiveUrl,
                                   @Value("${cn1.jdk8.windowsUrl:}") String windowsArchiveUrl,
                                   @Value("${cn1.jdk8.rootMarker}") String rootMarker) {
        this.extractor = extractor;
        this.linuxArchiveResource = linuxArchiveResource;
        this.macArchiveUrl = macArchiveUrl;
        this.windowsArchiveUrl = windowsArchiveUrl;
        this.rootMarker = rootMarker;
    }

    public Jdk8ManagerFromResource(GlobalExtractor extractor,
                                   String linuxArchiveResource,
                                   String rootMarker) {
        this(extractor, linuxArchiveResource, "", "", rootMarker);
    }

    /** Ensures the bundled JDK 8 archive is extracted, returns path to .../bin/javac */
    public Path ensureJavac8() throws IOException {
        String os = System.getProperty("os.name", "linux").toLowerCase(Locale.ENGLISH);
        LOG.info("Resolving bundled JDK8 for operating system {}", os);
        if (os.contains("win")) {
            if (windowsArchiveUrl != null && !windowsArchiveUrl.isBlank()) {
                return ensureFromUrl(windowsArchiveUrl, true, "Windows");
            }
            LOG.warn("No Windows JDK8 archive configured, falling back to bundled Linux resource");
            return ensureFromResource(linuxArchiveResource);
        }
        if (os.contains("mac") || os.contains("darwin")) {
            if (macArchiveUrl != null && !macArchiveUrl.isBlank()) {
                return ensureFromUrl(macArchiveUrl, false, "macOS");
            }
            LOG.warn("No macOS JDK8 archive configured, falling back to bundled Linux resource");
            return ensureFromResource(linuxArchiveResource);
        }
        return ensureFromResource(linuxArchiveResource);
    }

    private Path ensureFromResource(String resource) throws IOException {
        if (resource == null || resource.isBlank()) {
            throw new IOException("No bundled JDK8 resource configured for Linux");
        }
        String fileName = Path.of(resource).getFileName().toString();
        String folderName = stripArchiveExtension(fileName);
        GlobalExtractor.ArchiveType type = GlobalExtractor.ArchiveType.fromName(fileName);
        LOG.info("Ensuring Linux JDK8 resource {} -> folder {}", resource, folderName);
        Path jdkRoot = extractor.ensureArchiveExtracted(resource, folderName);
        Path root = findJdkRoot(jdkRoot);
        return resolveJavac(root, type == GlobalExtractor.ArchiveType.ZIP);
    }

    private Path ensureFromUrl(String url, boolean windows, String label) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("No JDK8 download URL configured for " + label);
        }
        String fileName = fileName(url);
        String folderName = stripArchiveExtension(fileName);
        LOG.info("Ensuring remote JDK8 {} for {} -> folder {}", url, label, folderName);
        Path jdkRoot = extractor.ensureArchiveExtractedFromUrl(url, folderName);
        Path root = findJdkRoot(jdkRoot);
        return resolveJavac(root, windows);
    }

    private Path findJdkRoot(Path base) throws IOException {
        // If base has 'release' file, it's the root. Otherwise, look one level down.
        if (Files.exists(base.resolve(rootMarker))) return base;
        try (var stream = Files.list(base)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(p) && Files.exists(p.resolve(rootMarker))) return p;
            }
        }
        return base; // fallback
    }

    private Path resolveJavac(Path root, boolean windows) throws IOException {
        Path javac = root.resolve(windows ? "bin/javac.exe" : "bin/javac");
        if (!Files.exists(javac)) throw new IOException("javac not found at " + javac);
        if (!windows && !Files.isExecutable(javac)) throw new IOException("javac not executable at " + javac);
        LOG.debug("Resolved javac executable at {}", javac);
        return javac;
    }

    private static String stripArchiveExtension(String name) {
        if (name.toLowerCase(Locale.ENGLISH).endsWith(".tar.gz")) {
            return name.substring(0, name.length() - ".tar.gz".length());
        }
        if (name.toLowerCase(Locale.ENGLISH).endsWith(".tgz")) {
            return name.substring(0, name.length() - ".tgz".length());
        }
        if (name.toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
            return name.substring(0, name.length() - ".zip".length());
        }
        return name;
    }

    private static String fileName(String url) throws MalformedURLException {
        return Path.of(new URL(url).getPath()).getFileName().toString();
    }
}