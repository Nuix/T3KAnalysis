package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApiException;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class SourceId {
    private static final String DEFAULT_FILE_NAME = "t3k_data_id.json";

    private static final Object threadLock = new Object();

    private final Path cacheFile;

    public SourceId(String pathToCacheFile) {
        if (null == pathToCacheFile)
            throw new IllegalArgumentException("The path to the cache for the SourceId must not be null.");

        Path filePath;
        if (Files.isDirectory(Path.of(pathToCacheFile))) {
            // This is a directory, use default file name
            filePath = Path.of(pathToCacheFile, DEFAULT_FILE_NAME);
        } else {
            // Use the full provided path
            filePath = Path.of(pathToCacheFile);
        }

        // Create parent directories if not yet made
        Path parentFolder = filePath.getParent();
        if (Files.notExists(parentFolder)) {
            try {
                Files.createDirectories(parentFolder);
            } catch (IOException e) {
                throw new T3KApiException("Unable to create directories needed for the SourceId Cache File.", e);
            }
        }

        this.cacheFile = filePath;
    }

    private void cacheSourceId(long id, FileChannel cacheChannel) {
        try { // (FileWriter writer = new FileWriter(cacheFile.toFile())) {
            cacheChannel.truncate(0);
            Writer writer = Channels.newWriter(cacheChannel, StandardCharsets.UTF_8);
            Map<String, Long> cache = Map.of("id", id);
            String json = new Gson().toJson(cache);
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            throw new T3KApiException(String.format("Error writing SourceId to %s", cacheFile.toString()), e);
        }
    }

    private long readSourceId(FileChannel cacheChannel) {
        if (Files.exists(cacheFile)) {
            Reader reader = Channels.newReader(cacheChannel, StandardCharsets.UTF_8);
            Map cache = new Gson().fromJson(reader, Map.class);
            return ((Number) cache.get("id")).longValue();
        } else {
            // No cache, initialize a new source id
            return 0;
        }
    }

    public long getNextId() {
        // Synchronized, so multiple threads on the same JVM are run through 1 at a time
        synchronized (threadLock) {

            try (FileChannel cacheChannel = FileChannel.open(cacheFile, StandardOpenOption.READ,
                                                                        StandardOpenOption.WRITE,
                                                                        StandardOpenOption.CREATE,
                                                                        StandardOpenOption.SYNC)) {
                // File locked so multiple instances on different JVMs run through 1 at a time
                FileLock cacheLock = cacheChannel.lock(0, Long.MAX_VALUE, false);

                long id;
                id = readSourceId(cacheChannel) + 1;
                cacheSourceId(id, cacheChannel);
                return id;

            } catch (IOException e) {
                throw new T3KApiException(String.format("Error reading to the SourceId cache file: %s", cacheFile), e);
            }
        }
    }
}
