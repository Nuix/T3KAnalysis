package com.nuix.proserv.t3k.results;

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

    @Override
    public String toString() {
        final StringBuilder output = new StringBuilder(super.toString())
                .append(" Mode: ").append(mode)
                .append(" Image Size: ").append(imageSize)
                .append(" File Size: ").append(fileSize)
                .append(" MD5: ").append(md5)
                .append(" SHA1: ").append(sha1)
                .append(" photoDNA: ").append(photoDNA);

        return output.toString();
    }

    public static boolean isImageResults(Map<String, Object> metadata) {
        return metadata.containsKey(MODE);
    }

    public static ImageResult parseResult(Map<String, Object> metadata) {
        if(isImageResults(metadata)) {
            ImageResult result = new ImageResult();

            result.mode = (String)metadata.get(MODE);
            result.width = (int)metadata.get(WIDTH);
            result.height = (int)metadata.get(HEIGHT);
            result.fileSize = (long)metadata.get(BYTES);
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
