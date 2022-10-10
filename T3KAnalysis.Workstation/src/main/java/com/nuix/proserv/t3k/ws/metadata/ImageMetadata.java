package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata;
import nuix.CustomMetadataMap;

public class ImageMetadata extends AnalysisMetadata {

    private int detectionCounter = 0;

    public ImageMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        super(metadataMap, analysisResult);
    }

    @Override
    protected void applyDetection(Detection detection) {
        detectionCounter++;

        DetectionMetadata detectionMetadata = DetectionMetadata.getMetadataForDetection(detection, getMetadataMap());
        detectionMetadata.applyDetection(detection, detectionCounter);
    }
}
