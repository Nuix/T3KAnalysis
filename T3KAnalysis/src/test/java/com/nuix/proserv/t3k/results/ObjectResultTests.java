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

public class ObjectResultTests {
    @Test
    public void ObjectOnImageParses () {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/ObjectFromImage.json", StandardCharsets.UTF_8);

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(AnalysisResult.class, new AnalysisResult.Deserializer());
            builder.registerTypeAdapter(Detection.class, new DetectionTypeMap());
            builder.registerTypeAdapter(DetectionData.class, new DetectionDataDeserializer());
            Gson gson =builder.create();

            AnalysisResult result = gson.fromJson(json, AnalysisResult.class);

            assertEquals(ImageResult.class, result.getClass());
            assertEquals(5, result.getDetectionCount());

            Queue<String> expectedClassifications = new LinkedList<>();
            expectedClassifications.add("gun");
            expectedClassifications.add("military_uniform");
            expectedClassifications.add("gun");
            expectedClassifications.add("military_uniform");
            expectedClassifications.add("military_uniform");

            result.forEachDetection(detection ->  {
                System.out.println(detection);
                assertEquals(ObjectDetection.class, detection.getClass());

                ObjectDetection object = (ObjectDetection)detection;
                String expectedClassification = expectedClassifications.remove();
                assertEquals(expectedClassification, object.getClass_name());
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
