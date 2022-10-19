package com.nuix.proserv.t3k.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

public class UploadResultTests {
    @Test
    public void singleUpload() {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/SingleUploadSuccess.json", StandardCharsets.UTF_8);

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(UploadResult.class, new UploadResult.Deserializer());
            Gson gson = builder.create();

            UploadResult results = gson.fromJson(json, UploadResult.class);

            assertEquals(1, results.getResultCount());

            results.forEachId(id -> {
                assertEquals(320L, id.longValue());
                assertEquals("/host/path/file", results.get(id));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void batchUpload() {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/BatchUploadSuccess.json", StandardCharsets.UTF_8);

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(UploadResult.class, new UploadResult.Deserializer());
            Gson gson = builder.create();

            UploadResult results = gson.fromJson(json, UploadResult.class);

            assertEquals(3, results.getResultCount());

            Queue<Long> expectedIds = new LinkedList<>();
            expectedIds.add(320L);
            expectedIds.add(321L);
            expectedIds.add(322L);

            Queue<String> expectedPaths = new LinkedList<>();
            expectedPaths.add("/host/path/file");
            expectedPaths.add("file2");
            expectedPaths.add("/host/path/file3");

            results.forEachId(id -> {
                assertEquals(expectedIds.remove().longValue(), id.longValue());
                assertEquals(expectedPaths.remove(), results.get(id));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
