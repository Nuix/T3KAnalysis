package com.nuix.proserv.t3k.results;

import lombok.Getter;

import java.util.Map;

public class ImageResult extends AnalysisResult {
    private static final long serialVersionUID = 1L;

    private static final String MODE = "mode";
    private static final String NALVIS = "nalvis";

    private ImageMetadata metadata;

    @Getter
    private String nalvis;

    protected ImageResult() {}

    @Override
    public ResultMetadata getMetadata() {
        return this.metadata;
    }

    @Override
    public String toString() {

        return super.toString() +
                " nalvis: " + getNalvis();
    }

    public static boolean isImageResults(Map<String, Object> metadata) {
        return metadata.containsKey(MODE);
    }
}
