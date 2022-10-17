package com.nuix.proserv.t3k.ws.metadata;

/**
 * Non-functional interface for providing constants used for custom metadata labels.
 */
public interface T3KMetadata {
    /**
     * Top level label that represents the parent for all other custom metadata to add.
     */
    String T3K_DETECTION = "T3K Detections";

    /**
     * Delimiter to split nested metadata fields.  Those on the right of this delimiter will be seen as children to
     * those on the left.
     */
    String METADATA_LEVEL_SEPARATOR = "|";

    /**
     * Child metadata for the count of detections in this item.
     */
    String T3K_COUNT = T3K_DETECTION + METADATA_LEVEL_SEPARATOR + "Count";

    /**
     * Generic template for detections.  It will produce a three-level-deep family.  The first layer is the T3K
     * Detection top-level label.  The second layer is a Format String placeholder for the detection's index.  The
     * third layer should be affixed to the end of this template to provide the name for the custom metadata field.
     */
    String T3K_TEMPLATE = T3K_DETECTION + METADATA_LEVEL_SEPARATOR + "%d" + METADATA_LEVEL_SEPARATOR;

    /**
     * Text to display in the top level metadata when at least one detection was made on the item.
     */
    String T3K_MATCH_FOUND = "Match Detected";

    /**
     * Text to display in the top level metadata when no detections were found on the item.
     */
    String T3K_NO_MATCHES = "No Matches Detected";
}
