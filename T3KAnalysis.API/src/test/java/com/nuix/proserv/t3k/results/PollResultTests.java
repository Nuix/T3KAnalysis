package com.nuix.proserv.t3k.results;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class PollResultTests {

    @Test
    public void pollFinished() {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/PollFinished.json", StandardCharsets.UTF_8);
            Gson gson = new Gson();

            PollResults results = gson.fromJson(json, PollResults.class);
            System.out.println(results);
            assertTrue(results.isFinished());
            assertFalse(results.isPending());
            assertEquals(9L, results.getId());
            assertEquals("/CORE/resources/data/828cbc6a-214f-41ca-9bd2-328d8688814b.jpg", results.getFilepath());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void pollPending() {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/PollPending.json", StandardCharsets.UTF_8);
            Gson gson = new Gson();

            PollResults results = gson.fromJson(json, PollResults.class);
            System.out.println(results);
            assertFalse(results.isFinished());
            assertTrue(results.isPending());
            assertEquals(9L, results.getId());
            assertEquals("/CORE/resources/data/828cbc6a-214f-41ca-9bd2-328d8688814b.jpg", results.getFilepath());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
