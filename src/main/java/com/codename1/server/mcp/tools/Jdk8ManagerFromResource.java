package com.codename1.server.mcp.tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Component
public class Jdk8ManagerFromResource {
    private final GlobalExtractor extractor;
    private final String jdkArchiveResource;
    private final String rootMarker; // e.g. "release"

    public Jdk8ManagerFromResource(GlobalExtractor extractor,
                                   @Value("${cn1.jdk8.resourcePath}") String jdkArchiveResource,
                                   @Value("${cn1.jdk8.rootMarker}") String rootMarker) {
        this.extractor = extractor;
        this.jdkArchiveResource = jdkArchiveResource;
        this.rootMarker = rootMarker;
    }

    /** Ensures the bundled JDK 8 archive is extracted, returns path to .../bin/javac */
    public Path ensureJavac8() throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
            // this is a local environment test
            return Path.of("/Users/shai/Library/Java/JavaVirtualMachines/azul-1.8.0_372/Contents/Home/bin/javac");
        }
        // The folder name is derived from the archive file name (without extension)
        String fileName = Path.of(jdkArchiveResource).getFileName().toString();          // temurin8-linux-x64.tar.gz
        String folderName = fileName.replace(".tar.gz", "");
        Path jdkRoot = extractor.ensureArchiveExtracted(jdkArchiveResource, folderName);

        // Some archives contain a top-level directory (e.g., jdk8uXXX-.../)
        Path root = findJdkRoot(jdkRoot);
        Path javac = root.resolve("bin/javac");
        if (!Files.isExecutable(javac)) throw new IOException("javac not found at " + javac);
        return javac;
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
}