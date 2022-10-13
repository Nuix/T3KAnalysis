package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.CCRDetection;
import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.ObjectDetection;
import com.nuix.proserv.t3k.ws.metadata.T3KMetadata;
import nuix.CustomMetadataMap;

public class CCRMetadata extends DetectionMetadata {
    private static final String T3K_INFORMATION = String.format(T3KMetadata.T3K_TEMPLATE, "%s", "%s");
    private static final String T3K_SIMILARITY = "|Similarity";
    private static final String T3K_CCR_ID = "|ID";

    protected CCRMetadata(CustomMetadataMap metadataMap) {
        super(metadataMap);
    }

    @Override
    public String getTypeTemplate(Detection detection) {
        if (CCRDetection.class.isAssignableFrom(detection.getClass())) {
            return String.format(T3K_INFORMATION, "%d", ((CCRDetection)detection).getInfo());
        } else {
            LOG.warn("The detection provided is not a CCR detection, providing a generic template for the metadata label.  Type: {}", detection.getType());
            return String.format(T3K_INFORMATION, "%d", detection.getType());
        }
    }

    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(!CCRDetection.class.isAssignableFrom(detection.getClass())) {
            LOG.error("The detection provided is not a CCR detection, skipping it.  {}", detection);
            return;
        }

        CCRDetection ccrDetection = ((CCRDetection) detection);
        String baseLabel = String.format(getTypeTemplate(detection), detectionIndex);
        String similarityLabel =  baseLabel + T3K_SIMILARITY;
        String idLabel = baseLabel + T3K_CCR_ID;
        getMetadataMap().putFloat(similarityLabel, ccrDetection.getSimilarity());
        getMetadataMap().putInteger(idLabel, ccrDetection.getId());
    }
}
