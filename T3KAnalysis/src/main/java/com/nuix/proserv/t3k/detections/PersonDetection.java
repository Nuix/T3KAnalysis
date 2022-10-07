package com.nuix.proserv.t3k.detections;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Map;

public class PersonDetection extends Detection {
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "age/gender";

    @Getter @Setter
    private DetectionData<?> data;

    @Getter
    private double[] box = new double[4];

    @Getter
    private double score;

    @Getter
    private String gender;

    @Getter
    private int age;

    private PersonDetection() {}

    @Override
    public String toString() {
        return super.toString() +
                " Age: " + String.valueOf(age) +
                " Gender: " + gender +
                " Score: " + String.valueOf(score) +
                " Location: " + String.valueOf(box[0]) + "x" + String.valueOf(box[1]) +
                        " - " + String.valueOf(box[2]) + "x" + String.valueOf(box[3]) +
                " Data: " + data.toString();
    }

    public static boolean isPersonDetection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }

}
