package com.nuix.proserv.t3k.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuix.proserv.t3k.detections.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;


public class PersonResultTests {
    @Test
    public void personInDocumentParses() {
        try {
            String json = IOUtils.resourceToString("/com/nuix/proserv/t3k/results/PersonInDocument.json", StandardCharsets.UTF_8);
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(AnalysisResult.class, new AnalysisResult.Deserializer());
            builder.registerTypeAdapter(Detection.class, new DetectionTypeMap());
            builder.registerTypeAdapter(DetectionData.class, new DetectionDataDeserializer());
            Gson gson =builder.create();

            AnalysisResult result = gson.fromJson(json, AnalysisResult.class);

            assertEquals(DocumentResult.class, result.getClass());
            assertEquals(1, result.getDetectionCount());

            result.forEachDetection(detection ->  {
                System.out.println(detection);
                assertEquals(PersonDetection.class, detection.getClass());

                PersonDetection person = (PersonDetection)detection;
                assertEquals("female", person.getGender());
                assertEquals(2, person.getAge());
                assertEquals(19.0, person.getScore(), 0.2);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
