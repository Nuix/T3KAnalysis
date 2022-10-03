package com.nuix.proserv.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.DetectionWithData;
import com.nuix.proserv.t3k.detections.VideoDetectionData;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.ws.metadata.detections.DetectionMetadata;
import nuix.CustomMetadataMap;

public class VideoMetadata extends AnalysisMetadata {
    private static final String T3K_DATA = "%s/Frame";

    private int detectionCounter = 0;

    protected VideoMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        super(metadataMap, analysisResult);
    }

    @Override
    protected void applyDetection(Detection detection) {
        detectionCounter++;

        DetectionMetadata detectionMetadata = DetectionMetadata.getMetadataForDetection(detection, getMetadataMap());
        detectionMetadata.applyDetection(detection, detectionCounter);

        if (detection instanceof DetectionWithData) {
            DetectionWithData dataDetection = (DetectionWithData)detection;

            String detectionBase = String.format(detectionMetadata.getTypeTemplate(detection), detectionCounter);
            String frameLabel = String.format(T3K_DATA, detectionBase);

            getMetadataMap().putInteger(frameLabel, ((VideoDetectionData)dataDetection.getData()).getFrame());
        }
    }
}
