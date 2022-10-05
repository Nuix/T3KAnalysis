package com.nuix.proserv.t3k.results;

import com.nuix.proserv.t3k.detections.DetectionWithData;
import lombok.Getter;

import java.util.Map;

public class ImageResult extends AnalysisResult implements HashedResult, RasteredResult, ResultWithNalvis {
    public static final String MODE = "mode";
    public static final String BYTES = "file_size";
    public static final String IMAGE_SIZE = "size";
    public static final String PHOTODNA = "photoDNA";

    @Getter
    private String md5;

    @Getter
    private String sha1;

    @Getter
    private int width;

    @Getter
    private int height;

    @Getter
    private String nalvisString;

    @Getter
    private String mode;

    @Getter
    private long fileSize;

    @Getter
    private String imageSize;

    @Getter
    private String photoDNA;

    private ImageResult() {}

    @Override
    protected void addDataToDetection(DetectionWithData detection, Map<String, Object> detectionData) {
        // An ImageResult has no Data to add...
    }

    @Override
    public String toString() {

        return super.toString() +
                " Mode: " + mode +
                " Image Size: " + imageSize +
                " File Size: " + fileSize +
                " MD5: " + md5 +
                " SHA1: " + sha1 +
                " photoDNA: " + photoDNA;
    }

    public static boolean isImageResults(Map<String, Object> metadata) {
        return metadata.containsKey(MODE);
    }

    public static ImageResult parseResult(Map<String, Object> metadata) {
        if(isImageResults(metadata)) {
            ImageResult result = new ImageResult();

            result.mode = (String)metadata.get(MODE);
            result.width = ((Number)metadata.get(WIDTH)).intValue();
            result.height = ((Number)metadata.get(HEIGHT)).intValue();
            result.fileSize = ((Number)metadata.get(BYTES)).longValue();
            result.imageSize = (String)metadata.get(IMAGE_SIZE);
            result.md5 = (String)metadata.getOrDefault(MD5, "");
            result.sha1 = (String)metadata.getOrDefault(SHA1, "");
            result.photoDNA= (String)metadata.getOrDefault(PHOTODNA, "");
            result.nalvisString = (String)metadata.getOrDefault(NALVIS, "");

            AnalysisResult.fillSharedFields(result, metadata);
            return result;

        } else {
            return null;
        }
    }
}
