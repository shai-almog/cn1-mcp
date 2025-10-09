package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.NativeStubRequest;
import com.codename1.server.mcp.dto.NativeStubResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class NativeStubService {
    private static final Logger LOG = LoggerFactory.getLogger(NativeStubService.class);

    public NativeStubResponse generate(NativeStubRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.interfaceName() == null || request.interfaceName().isBlank()) {
            throw new IllegalArgumentException("interfaceName is required");
        }
        if (request.files() == null || request.files().isEmpty()) {
            throw new IllegalArgumentException("At least one source file is required");
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK is required to generate native stubs");
        }

        Path sourceDir;
        Path classesDir;
        try {
            sourceDir = Files.createTempDirectory("cn1-native-src");
            classesDir = Files.createTempDirectory("cn1-native-classes");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to allocate temporary workspace", e);
        }

        try {
            Map<String, FileEntry> provided = new LinkedHashMap<>();
            for (FileEntry entry : request.files()) {
                if (entry.path() == null || entry.path().isBlank()) {
                    throw new IllegalArgumentException("File path is required");
                }
                Path dest = resolveRelativePath(sourceDir, entry.path());
                Files.createDirectories(dest.getParent());
                Files.writeString(dest, entry.content(), StandardCharsets.UTF_8);
                provided.put(entry.path(), entry);
            }

            ensureStub(sourceDir, provided, "com/codename1/system/NativeInterface.java",
                    "package com.codename1.system;\n\npublic interface NativeInterface {\n    boolean isSupported();\n}\n");
            ensureStub(sourceDir, provided, "com/codename1/ui/PeerComponent.java",
                    "package com.codename1.ui;\n\npublic class PeerComponent { }\n");

            List<Path> sources = new ArrayList<>();
            try (var stream = Files.walk(sourceDir)) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(sources::add);
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sources);
                List<String> options = List.of("-classpath", System.getProperty("java.class.path"), "-d", classesDir.toString());
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
                Boolean ok = task.call();
                if (!Boolean.TRUE.equals(ok)) {
                    StringBuilder message = new StringBuilder("Compilation failed:\n");
                    diagnostics.getDiagnostics().forEach(d -> message.append(d.toString()).append('\n'));
                    throw new IllegalArgumentException(message.toString());
                }
            }

            try (URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, getClass().getClassLoader())) {
                Class<?> iface;
                try {
                    iface = Class.forName(request.interfaceName(), true, loader);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Interface not found after compilation: " + request.interfaceName(), e);
                }
                NativeStubGenerator generator = new NativeStubGenerator(iface);
                String validation = generator.verify();
                if (validation != null) {
                    throw new IllegalArgumentException(validation);
                }
                Map<String, String> generated = generator.generate();
                LOG.info("Generated {} native stub files for interface {}", generated.size(), request.interfaceName());
                List<FileEntry> files = generated.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new FileEntry(e.getKey(), e.getValue()))
                        .toList();
                return new NativeStubResponse(files);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate native stubs", e);
        } finally {
            cleanup(sourceDir);
            cleanup(classesDir);
        }
    }

    private static void ensureStub(Path sourceDir, Map<String, FileEntry> provided, String relativePath, String content) throws IOException {
        if (provided.containsKey(relativePath)) {
            return;
        }
        Path dest = resolveRelativePath(sourceDir, relativePath);
        Files.createDirectories(dest.getParent());
        Files.writeString(dest, content, StandardCharsets.UTF_8);
    }

    private static Path resolveRelativePath(Path sourceDir, String requestedPath) {
        Path relative = Path.of(requestedPath);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("File path must be relative: " + requestedPath);
        }
        Path normalized = sourceDir.resolve(relative).normalize();
        if (!normalized.startsWith(sourceDir)) {
            throw new IllegalArgumentException("File path escapes workspace: " + requestedPath);
        }
        return normalized;
    }

    private static void cleanup(Path dir) {
        if (dir == null) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
