package com.nuix.proserv.t3k.conn;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.*;

public class SourceIdTests {
    private static final String RESOURCE_FOLDER = "build/resources/test";
    private static final String SOURCE_FILE = "t3k_data_id.json";
    private static final String TEST_FILE = "test_data_id.json";

    @Before
    public void copySourceIdFile() throws IOException {
        Files.copy(Path.of(RESOURCE_FOLDER, SOURCE_FILE).toAbsolutePath(), Path.of(RESOURCE_FOLDER, TEST_FILE).toAbsolutePath());
    }

    @After
    public void removeCopiedIdFile() throws IOException {
        Files.delete(Path.of(RESOURCE_FOLDER, TEST_FILE));
    }

    @Test
    public void SourceIdIsRead() {
        SourceId sourceId = new SourceId(Path.of(RESOURCE_FOLDER, TEST_FILE).toAbsolutePath().toString());

        assertEquals(588, sourceId.getNextId());
    }

    @Test
    public void SourceIdIsCached() {
        SourceId sourceId = new SourceId(Path.of(RESOURCE_FOLDER, TEST_FILE).toAbsolutePath().toString());

        assertEquals(588, sourceId.getNextId());

        assertEquals(589, sourceId.getNextId());
    }

    @Test
    public void SourceIdsUniqueAcrossThreads() {
        final CopyOnWriteArrayList<Long> results = new CopyOnWriteArrayList<>();
        final Object signal = new Object();
        final boolean[] wasTripped = {false};

        Runnable readIdTask = new Runnable() {
            @Override
            public void run() {
                // Each thread gets their own SourceId instance to simulate workers in different processes
                SourceId sourceId = new SourceId(Path.of(RESOURCE_FOLDER, TEST_FILE).toAbsolutePath().toString());

                // Wait until triggered - get all threads doing work close to the same time
                synchronized (signal) {
                    while (!wasTripped[0]) {
                        try {
                            signal.wait();
                        } catch (InterruptedException e) {
                            //keep waiting;
                        }
                    }
                }

                // waiting is done, get id and put it in results
                long id = sourceId.getNextId();
                results.add(id);
                System.out.printf("Adding %d%n", id);
            }
        };

        Thread[] workerSims = new Thread[30];
        for (int i = 0; i < workerSims.length; i++) {
            workerSims[i] = new Thread(readIdTask);
            workerSims[i].start();
        }

        // Signal all threads to get ids...
        wasTripped[0] = true;
        synchronized (signal) {
            signal.notifyAll();
        }

        // Wait for them to finish
        for (Thread thread : workerSims) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // Keep waiting;
                }
            }
        }

        assertEquals(30, results.size());
        for (long i = 0L; i < 30L; i++) {
            assertTrue(results.contains((588L + i)));
        }
    }
}
