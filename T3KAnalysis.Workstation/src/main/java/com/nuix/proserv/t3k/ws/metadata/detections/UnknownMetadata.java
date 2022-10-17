package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import nuix.CustomMetadataMap;

/**
 * Apply custom metadata to an unknown type of {@link Detection}.
 * <p>
 *     The details of the detection aren't known, so it is simply translated into a string and added to a Details field.
 * </p>
 */
public class UnknownMetadata extends DetectionMetadata {
    /**
     * Name of the unknown detection metadata type.
     */
    private static final String T3K_UNKNOWN = T3K_TEMPLATE + "Unknown";

    /**
     * Capture the information for the detection into a Details field
     */
    private static final String T3K_DETAILS = T3K_UNKNOWN + METADATA_LEVEL_SEPARATOR + "Details";

    /**
     * Create an UnknownMetadata to store data in the provided {@link CustomMetadataMap}
     * @param map The {@link CustomMetadataMap} to store details in
     */
    protected UnknownMetadata(CustomMetadataMap map) {
        super(map);
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} to generate a custom metadata label for.
     * @return A Format String with a numeric placeholder for the detection's index.
     */
    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_UNKNOWN;
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} with the data to add to the custom metadata
     * @param detectionIndex The index in the detections list for this detection
     */
    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        getMetadataMap().putText(String.format(T3K_DETAILS, detectionIndex), detection.toString());
    }
}
