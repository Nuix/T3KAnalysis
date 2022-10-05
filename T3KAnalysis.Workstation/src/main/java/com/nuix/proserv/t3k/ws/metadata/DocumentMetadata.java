package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.DetectionWithData;
import com.nuix.proserv.t3k.detections.DocumentDetectionData;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata;
import nuix.CustomMetadataMap;

public class DocumentMetadata extends AnalysisMetadata {
    private static final String T3K_PAGE = "%s/Page";
    private static final String T3K_IMAGE = "%s/Image";

    private int detectionCounter = 0;


    protected DocumentMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
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
            String pageLabel = String.format(T3K_PAGE, detectionBase);
            String imageLabel = String.format(T3K_IMAGE, detectionBase);

            getMetadataMap().putInteger(pageLabel, ((DocumentDetectionData)dataDetection.getData()).getPageNumber());
            getMetadataMap().putInteger(imageLabel, ((DocumentDetectionData)dataDetection.getData()).getImageNumber());
        }

    }
}
