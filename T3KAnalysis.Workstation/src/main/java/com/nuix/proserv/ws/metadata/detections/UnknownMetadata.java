package com.nuix.proserv.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import nuix.CustomMetadataMap;

public class UnknownMetadata extends DetectionMetadata {
    private static final String T3K_UNKNOWN = String.format(T3K_TEMPLATE, "%d", "Unknown");
    private static final String T3K_DETAILS = String.format("%s|%s", T3K_UNKNOWN, "Details");

    protected UnknownMetadata(CustomMetadataMap map) {
        super(map);
    }

    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_UNKNOWN;
    }

    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        getMetadataMap().putText(String.format(T3K_DETAILS, detectionIndex), detection.toString());
    }
}
