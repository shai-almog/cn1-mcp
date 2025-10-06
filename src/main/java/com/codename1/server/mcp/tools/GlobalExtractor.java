package com.codename1.server.mcp.tools;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class GlobalExtractor {
    private static final ConcurrentHashMap<String, ReentrantLock> LOCAL_LOCKS = new ConcurrentHashMap<>();

    private final Path cacheDir;
    private final String versionTag;

    public GlobalExtractor(String cacheDir, String versionTag) {
        this.cacheDir = Paths.get(Objects.requireNonNull(cacheDir));
        this.versionTag = Objects.requireNonNull(versionTag);
    }

    /** Make overridable for tests. */
    protected byte[] readResource(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new FileNotFoundException("Resource not found: " + path);
            return in.readAllBytes();
        }
    }

    public Path ensureFile(String resourcePath) throws IOException {
        byte[] bytes = readResource(resourcePath);
        String hash = sha256(bytes).substring(0, 12);

        Path base = cacheDir.resolve("libs").resolve(versionTag + "-" + hash);
        Files.createDirectories(base);

        Path out = base.resolve(Path.of(resourcePath).getFileName().toString());
        if (Files.exists(out)) return out;

        Path lockPath = base.resolve(".extract.lock");
        ReentrantLock local = LOCAL_LOCKS.computeIfAbsent(lockPath.toString(), k -> new ReentrantLock());

        local.lock();
        try (FileChannel ch = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = ch.lock()) { // OS-level lock for cross-process safety
            if (!Files.exists(out)) {
                Path tmp = Files.createTempFile(base, ".res", ".tmp");
                Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING);
                Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            local.unlock();
        }
        return out;
    }

    public Path ensureArchiveExtracted(String archiveResourcePath, String folderName) throws IOException {
        byte[] bytes = readResource(archiveResourcePath);
        String hash = sha256(bytes).substring(0, 12);

        Path parent = cacheDir.resolve("jdks").resolve(versionTag + "-" + hash);
        Path destRoot = parent.resolve(folderName);
        if (Files.exists(destRoot)) return destRoot;

        Path lockPath = parent.resolve(".extract.lock");
        ReentrantLock local = LOCAL_LOCKS.computeIfAbsent(lockPath.toString(), k -> new ReentrantLock());

        local.lock();
        try (FileChannel ch = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = ch.lock()) {
            if (Files.exists(destRoot)) return destRoot;

            Path tmpRoot = Files.createTempDirectory(parent, "tmp-extract-");
            try (InputStream in = new ByteArrayInputStream(bytes);
                 GzipCompressorInputStream gzi = new GzipCompressorInputStream(in);
                 TarArchiveInputStream tar = new TarArchiveInputStream(gzi)) {

                TarArchiveEntry e;
                while ((e = tar.getNextTarEntry()) != null) {
                    Path out = tmpRoot.resolve(e.getName()).normalize();
                    if (!out.startsWith(tmpRoot)) throw new IOException("Zip Slip detected: " + out);
                    if (e.isDirectory()) {
                        Files.createDirectories(out);
                    } else {
                        Files.createDirectories(out.getParent());
                        try (OutputStream os = Files.newOutputStream(out)) {
                            tar.transferTo(os);
                        }
                        if ((e.getMode() & 0100) != 0) out.toFile().setExecutable(true);
                    }
                }
            }
            Files.createDirectories(parent);
            Files.move(tmpRoot, destRoot, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            local.unlock();
        }
        return destRoot;
    }

    private static String sha256(byte[] d) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(d));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}