package com.nuix.proserv.t3k.detections;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Map;

public class ObjectDetection extends Detection implements DetectionWithData, DetectionWithLocation, DetectionWithScore {
    public static final String TYPE = "object";
    public static final String CLASSIFICATION = "class_name";

    @Getter @Setter
    private DetectionData<?> data;

    @Getter
    private Rectangle2D.Double box;

    @Getter
    private double score;

    @Getter
    private String classification;

    private ObjectDetection() {}

    @Override
    public String toString() {
        return super.toString() +
                " Classification: " + classification +
                " Score: " + String.valueOf(score) +
                " Location: " + String.valueOf(box.x) + "x" + String.valueOf(box.y) +
                          " - " + String.valueOf(box.width) + "x" + String.valueOf(box.height) +
                " Data: " + data.toString();
    }


    public static boolean isObjectDetection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }

    public static ObjectDetection parseDetection(Map<String, Object> detectionData) {
        if (isObjectDetection(detectionData)) {

            ObjectDetection detection = new ObjectDetection();
            detection.classification = (String)detectionData.get(CLASSIFICATION);
            detection.score = (double) detectionData.get(SCORE);

            double[] boxParams = (double[]) detectionData.get(BOX);
            detection.box = new Rectangle2D.Double(boxParams[0], boxParams[1], boxParams[2], boxParams[3]);

            Detection.fillSharedValues(detection, detectionData);

            return detection;
        } else {
            return null;
        }
    }
}
