package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.MD5Detection;
import nuix.CustomMetadataMap;

/**
 * Translate {@link MD5Detection}s to custom metadata for an item.
 */
public class MD5Metadata extends DetectionMetadata {

    /**
     * Format String for the MD5 Detection.  There is a single numeric placeholder for the detection's ID.
     */
    private static final String T3K_MD5_HIT = T3K_TEMPLATE + "MD5 Hit";

    /**
     * An MD5 hit matches a specific, preconfigured MD5 - this is the type used to describe the matched hit.
     */
    private static final String T3K_MD5_TYPE = METADATA_LEVEL_SEPARATOR + "Type";

    /**
     * The MD5 Hash that was matched.
     */
    private static final String T3K_MD5_HASH = METADATA_LEVEL_SEPARATOR +  "HASH";

    /**
     * Each MD5 hit matches a specific, preconfigured MD5 - this is a description provided by that matching MD5.
     */
    private static final String T3K_MD5_DESCRIPTION = METADATA_LEVEL_SEPARATOR +  "Description";

    /**
     * Each MD5 hit matches a specific, preconfigured MD5 - this is the ID of that metching MD5.
     */
    private static final String T3K_MD5_ID = METADATA_LEVEL_SEPARATOR + "ID";

    protected MD5Metadata(CustomMetadataMap map) {
        super(map);
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} to generate a custom metadata label for.
     * @return the String format fit for an MD5 hit.
     */
    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_MD5_TYPE;
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} with the data to add to the custom metadata
     * @param detectionIndex The index in the detections list for this detection
     */
    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof MD5Detection)) {
            LOG.error("The detection is not an MD5 hit but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        MD5Detection hit = (MD5Detection)detection;
        String baseLabel = String.format(T3K_MD5_HIT, detectionIndex);
        getMetadataMap().putInteger(baseLabel + T3K_MD5_ID, hit.getId());
        getMetadataMap().putText(baseLabel + T3K_MD5_TYPE, hit.getType());
        getMetadataMap().putText(baseLabel + T3K_MD5_DESCRIPTION, hit.getDescription());
        getMetadataMap().putText(baseLabel + T3K_MD5_HASH, hit.getHash());

    }
}
