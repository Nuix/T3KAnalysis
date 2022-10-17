package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.ws.metadata.T3KMetadata;

/**
 * Non-functional interface to provide common custom metadata labels for detections that have a Score component.
 */
public interface MetadataWithScore {

    /**
     * Several types of detections have a score which describes how strongly probable the detection was matched.  This
     * provides a common metadata label to apply to those cases.  It already has a METADATA_LEVEL_SEPARATOR and should
     * be appended directly to the end of a detection's name/type label.
     */
    String T3K_SCORE = T3KMetadata.METADATA_LEVEL_SEPARATOR + "Score";

}
