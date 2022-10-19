package com.nuix.proserv.t3k.results;

import lombok.Getter;

import java.util.Map;

public class VideoResult extends AnalysisResult {
    private static final long serialVersionUID = 1L;

    public static final String FPS = "fps_video";

    private VideoMetadata metadata;

    @Getter
    private String nalvis;

    protected VideoResult() {}

    @Override
    public ResultMetadata getMetadata() {
        return this.metadata;
    }

    @Override
    public String toString() {
        return super.toString() + " nalvis: " + getNalvis();
    }

    public static boolean isVideoResult(Map<String, Object> metadata) {
        return metadata.containsKey(FPS);
    }
}
