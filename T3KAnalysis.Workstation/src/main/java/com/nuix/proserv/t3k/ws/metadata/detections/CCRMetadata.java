package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.CCRDetection;
import com.nuix.proserv.t3k.detections.Detection;
import nuix.CustomMetadataMap;

/**
 * Generate custom metadata for Curated Content Result (CCR) detections.
 * <p>
 *     This class is responsible for generating custom metadata for detections with the type "CCR".  This class behaves
 *     a lot like {@link ObjectMetadata} except it uses a {@code similarity} value instead of {@code score}, its
 *     'classification' is stored in {@code info} instead of {@code class_name}, and it provides an ID for the type
 *     of CCR it matches, and the results apply to the item (Image, Video Frame, or Document Image) in general and not
 *     to a specific portion of the image (so no box or color properties).
 * </p>
 * <p>
 *     Although this seems like a lot of differences, for custom metadata it behaves nearly identical.  The name of
 *     the detection will be derived from the type of the match (the {@code info}), and there will be one important
 *     numeric value to show (the {@code similarity}).  As such, the custom metadata will appear the same and similar
 *     means can be used to extract it (for example a shared method for scripted metadata).
 * </p>
 */
public class CCRMetadata extends DetectionMetadata {
    /**
     * The CCR custom metadata label follows the same format as the T3K_TEMPLATE.  The name of the detection is derived
     * from the information field.  To use it, append the information field's value to this template to get a Format
     * String that includes a single numerical field for the deteciotion's index.
     */
    private static final String T3K_INFORMATION = T3K_TEMPLATE;

    /**
     * Label for the Similarity metadata.  Append this directly to the detection's label to generate the child metadata.
     */
    private static final String T3K_SIMILARITY = METADATA_LEVEL_SEPARATOR + "Similarity";

    /**
     * Label for this ID metadata.  Append this directrly to the detection's label to generate the child metadata.
     */
    private static final String T3K_CCR_ID = METADATA_LEVEL_SEPARATOR + "ID";

    /**
     * Create a CCRMetadata with the {@link CustomMetadataMap} instance to store to
     * @param metadataMap the {@link CustomMetadataMap} to store data in
     */
    protected CCRMetadata(CustomMetadataMap metadataMap) {
        super(metadataMap);
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} to generate a custom metadata label for.
     * @return A format string with the detection's label and a single numeric field for the detection's index.
     */
    @Override
    public String getTypeTemplate(Detection detection) {
        if (CCRDetection.class.isAssignableFrom(detection.getClass())) {
            return T3K_INFORMATION + ((CCRDetection)detection).getInfo();
        } else {
            LOG.warn("The detection provided is not a CCR detection, providing a generic template for the metadata label.  Type: {}", detection.getType());
            return T3K_INFORMATION + detection.getType();
        }
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} with the data to add to the custom metadata
     * @param detectionIndex The index in the detections list for this detection
     */
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
