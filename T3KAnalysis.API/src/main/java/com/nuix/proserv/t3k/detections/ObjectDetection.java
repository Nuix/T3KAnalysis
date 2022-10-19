package com.nuix.proserv.t3k.detections;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Map;

public class ObjectDetection extends Detection implements DetectionWithData {
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "object";
    public static final String CLASSIFICATION = "class_name";

    @Getter @Setter
    private DetectionData<?> data;

    @Getter
    private double[] box = new double[4];

    @Getter
    private double score;

    @Getter
    private String class_name;

    protected ObjectDetection() {}

    @Override
    public String toString() {
        return super.toString() +
                " Classification: " + class_name +
                " Score: " + String.valueOf(score) +
                " Location: " + String.valueOf(box[0]) + "x" + String.valueOf(box[1]) +
                          " - " + String.valueOf(box[2]) + "x" + String.valueOf(box[3]) +
                " Data: " + ((null == data) ? "none" : data.toString());
    }


    public static boolean isObjectDetection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }
}
