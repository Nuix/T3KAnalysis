package com.nuix.proserv.t3k.detections;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class CCRDetection extends Detection implements DetectionWithData {
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "CCR";

    @Getter @Setter
    private DetectionData<?> data;

    @Getter
    private double similarity;

    @Getter
    private int id;

    protected CCRDetection() {}

    @Override
    public String toString() {
        return super.toString() +
                " Curated Content: (" + getId() + "): " + getInfo() +
                " Similarity: " + String.format("%.3f", getSimilarity()) +
                " Data: " + ((null == data) ? "none" : data.toString());
    }

    public static boolean isCCRDetection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }


}
