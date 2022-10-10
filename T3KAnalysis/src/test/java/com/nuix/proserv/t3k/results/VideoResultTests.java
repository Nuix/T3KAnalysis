package com.nuix.proserv.t3k.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuix.proserv.t3k.detections.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class VideoResultTests {

    @Test
    public void videoResultsParse() {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/ThingsInVideo.json", StandardCharsets.UTF_8);

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(AnalysisResult.class, new AnalysisResult.Deserializer());
            builder.registerTypeAdapter(Detection.class, new DetectionTypeMap());
            builder.registerTypeAdapter(DetectionData.class, new DetectionDataDeserializer());
            Gson gson =builder.create();

            AnalysisResult result = gson.fromJson(json, AnalysisResult.class);

            assertEquals(VideoResult.class, result.getClass());
            assertEquals(68, result.getDetectionCount());

            int[] counter = new int[] { 0 };

            result.forEachDetection(detection -> {
                System.out.println(detection);

                if (0 == counter[0]) {
                    assertEquals(ObjectDetection.class, detection.getClass());
                    ObjectDetection obj = (ObjectDetection) detection;
                    assertEquals("gun", obj.getClass_name());
                    assertEquals(71.0, obj.getScore(), 0.2);
                    DetectionData data = obj.getData();
                    assertEquals(VideoDetectionData.class, data.getClass());
                    assertEquals(85, ((VideoDetectionData)data).getFrame());
                }

                if (8 == counter[0]) {
                    assertEquals(PersonDetection.class, detection.getClass());
                    PersonDetection pers = (PersonDetection) detection;
                    assertEquals("male", pers.getGender());
                    assertEquals(1, pers.getAge());
                    assertEquals(5.5, pers.getScore(), 0.2);
                    DetectionData data = pers.getData();
                    assertEquals(VideoDetectionData.class, data.getClass());
                    assertEquals(4659, ((VideoDetectionData)data).getFrame());
                }

                if (11 == counter[0]) {
                    // CCRs are unknonwn as of yet
                    assertEquals(UnknownDetection.class, detection.getClass());
                }

                counter[0] = counter[0] + 1;
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
