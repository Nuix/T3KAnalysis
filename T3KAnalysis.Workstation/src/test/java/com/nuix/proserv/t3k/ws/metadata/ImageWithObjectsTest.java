package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.*;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.ImageResult;
import com.nuix.proserv.t3k.results.ResultMetadata;
import nuix.CustomMetadataMap;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ImageWithObjectsTest {
    static class ImageResultMock extends ImageResult {
        static class ImageResultMetadataMock extends com.nuix.proserv.t3k.results.ImageMetadata {
            @Override public String getMode() { return "RGB"; }
            @Override public long getFile_size() { return 663910L; }
            @Override public String getSize() { return "1024x680"; }
            @Override public String getPhotoDNA() { return "(31, 32, 28, 44, 20, 43, 32, 81, 33, 51, 60, 60, 32, 51, 63, 29, 79, 216, 153, 1, 142, 21, 237, 2, 82, 37, 40, 72, 36, 95, 90, 82, 76, 74, 84, 93, 75, 61, 48, 92, 78, 62, 102, 34, 63, 17, 69, 14, 153, 13, 61, 51, 62, 147, 50, 43, 105, 64, 84, 58, 43, 77, 101, 25, 144, 57, 54, 56, 52, 36, 61, 30, 164, 26, 19, 72, 107, 163, 29, 68, 132, 27, 82, 18, 17, 115, 56, 32, 122, 99, 12, 123, 139, 38, 31, 42, 95, 17, 21, 22, 122, 126, 24, 43, 148, 24, 16, 56, 8, 204, 28, 112, 157, 138, 20, 103, 205, 19, 6, 52, 70, 21, 11, 24, 78, 143, 10, 71, 159, 51, 1, 109, 12, 220, 6, 83, 120, 46, 15, 70, 128, 13, 2, 115)"; }
            @Override public String getMd5() { return "4c89332ff7b6cf7d773ddb8b3e80092d"; }
            @Override public String getSha1() { return "99ffcce52a546f48706ace0599352095df78afb3"; }
            @Override public int getWidth() { return 1024; }
            @Override public int getHeight() { return 680; }
            @Override public long getId() { return 4; }
            @Override public String getFile_path() { return "/CORE/resources/data/828cbc6a-214f-41ca-9bd2-328d8688814b.jpg"; }
        }

        static class ObjectDetectionMock extends ObjectDetection {
            @Override public double[] getBox() { return new double[] { 0.37767186760902407, 0.38570255041122439,
                                                                       0.8346562385559082, 0.6717167496681213}; }
            @Override public DetectionData<?> getData() { return null; }
            @Override public double getScore() { return 78.0; }
            @Override public String getClass_name() { return "gun"; }
            @Override public String getType() { return "object"; }
            @Override public String getInfo() { return "gun"; }
        }

        public ImageResultMock() {
            getDetections().add(new ObjectDetectionMock());
        }

        public ResultMetadata getMetadata() { return new ImageResultMetadataMock(); }
    }


    @Test
    public void metadataInstanceMade() {
        AnalysisResult result = new ImageResultMock();

        AnalysisMetadata metadata = AnalysisMetadata.getInstance(result, null);

        assertEquals(ImageMetadata.class, metadata.getClass());
    }


    @Test
    public void metadataIsAssigned() {
        AnalysisResult result = new ImageResultMock();
        CustomMetadataMap metadataMap = new CustomMetadataMapMock();

        AnalysisMetadata metadata = AnalysisMetadata.getInstance(result, metadataMap);
        metadata.applyResults();

        for(Map.Entry<String, Object> entry : metadataMap.entrySet()) {
            System.out.printf("%s: %s%n", entry.getKey(), entry.getValue());
        }

        assertEquals(T3KMetadata.T3K_MATCH_FOUND ,metadataMap.get(T3KMetadata.T3K_DETECTION));
        assertTrue(metadataMap.containsKey(T3KMetadata.T3K_COUNT));
        assertEquals(1 ,((Number)metadataMap.get(T3KMetadata.T3K_COUNT)).intValue());
    }

}
