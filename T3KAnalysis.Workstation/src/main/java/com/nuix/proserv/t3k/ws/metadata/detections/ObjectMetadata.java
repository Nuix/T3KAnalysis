package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.ObjectDetection;
import nuix.CustomMetadataMap;

/**
 * Custom metadata translation for {@link ObjectDetection} instances.
 * <p>
 *     There are multiple different types of ObjectDetections, defined by their {@link ObjectDetection#getClass_name()}.
 *     The class_name is used to identify the object for displaying the detection, with all the other detection
 *     information provided as child metadata.
 * </p>
 */
public class ObjectMetadata  extends DetectionMetadata implements MetadataWithScore {

    /**
     * The key component of the ObjectDetection are its classification.  The classification becomes the name of the
     * custom metadata tree for this detection.  This constant provides the prefix for the custom metadata field name,
     * with a single embedded String Format input for the detection index.
     */
    private static final String T3K_CLASSIFICATION = T3K_TEMPLATE;

    /**
     * Create an instance of the ObjectMetadata to apply an {@link ObjectDetection} to the provided {@link CustomMetadataMap}
     * @param map The {@link CustomMetadataMap} to apply results to
     */
    protected ObjectMetadata(CustomMetadataMap map) {
        super(map);
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} to generate a custom metadata label for.
     * @return A formatted string for this detection classification to be used as the metadata label
     */
    @Override
    public String getTypeTemplate(Detection detection) {
        if (detection instanceof ObjectDetection) {
            return T3K_CLASSIFICATION + ((ObjectDetection)detection).getClass_name();
        } else {
            LOG.warn("The type of the detection is not an Object as expected, returning a generic template.");
            return T3K_CLASSIFICATION + detection.getType();
        }
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} with the data to add to the custom metadata
     * @param detectionIndex The index in the detections list for this detection
     */
    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof ObjectDetection)) {
            LOG.error("The detection is not an object but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        ObjectDetection object = (ObjectDetection)detection;
        String scoreLabel = String.format(getTypeTemplate(detection), detectionIndex) + T3K_SCORE;
        getMetadataMap().putFloat(scoreLabel, object.getScore());

    }
}
