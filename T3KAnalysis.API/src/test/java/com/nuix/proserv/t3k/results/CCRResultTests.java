package com.nuix.proserv.t3k.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuix.proserv.t3k.detections.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

public class CCRResultTests {
    @Test
    public void CCROnImageParses () {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/CCROnImage.json", StandardCharsets.UTF_8);

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(AnalysisResult.class, new AnalysisResult.Deserializer());
            builder.registerTypeAdapter(Detection.class, new DetectionTypeMap());
            builder.registerTypeAdapter(DetectionData.class, new DetectionDataDeserializer());
            Gson gson =builder.create();

            AnalysisResult result = gson.fromJson(json, AnalysisResult.class);

            result.forEachDetection(detection ->  {
                System.out.println(detection);
                assertEquals(CCRDetection.class, detection.getClass());

                CCRDetection object = (CCRDetection)detection;
                String expectedMatch = "violence_weapons";
                assertEquals(expectedMatch, object.getInfo());
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
