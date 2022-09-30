package com.nuix.proserv.t3k.detections;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Map;

public class PersonDetection extends Detection implements DetectionWithData, DetectionWithLocation, DetectionWithScore {
    public static final String TYPE = "age/gender";
    public static final String AGE = "age";
    public static final String GENDER = "gender";
    public static final String SYMBOL = "gender_string";

    @Getter @Setter
    private DetectionData<?> data;

    @Getter
    private Rectangle2D.Double box;

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
                " Location: " + String.valueOf(box.x) + "x" + String.valueOf(box.y) +
                        " - " + String.valueOf(box.width) + "x" + String.valueOf(box.height) +
                " Data: " + data.toString();
    }

    public static boolean isPersonDetection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }

    public static PersonDetection parseDetection(Map<String, Object> detectionData) {
        if (isPersonDetection(detectionData)) {
            PersonDetection detection = new PersonDetection();

            detection.age = (int)detectionData.get(AGE);
            detection.gender = (String)detectionData.get(GENDER);
            detection.score = (double)detectionData.get(SCORE);

            double[] boxParams = (double[])detectionData.get(BOX);
            detection.box = new Rectangle2D.Double(boxParams[0], boxParams[1], boxParams[2], boxParams[3]);

            Detection.fillSharedValues(detection, detectionData);
            return detection;
        } else {
            return null;
        }
    }
}
