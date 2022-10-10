package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.MD5Detection;
import com.nuix.proserv.t3k.ws.metadata.T3KMetadata;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MD5Metadata extends DetectionMetadata {

    private static final String T3K_MD5_HIT = String.format(T3KMetadata.T3K_TEMPLATE, "%d", "MD5 Hit");

    private static final String T3K_MD5_TYPE = String.format("%s|%s", T3K_MD5_HIT, "Type");

    private static final String T3K_MD5_HASH = String.format("%s|%s", T3K_MD5_HIT, "HASH");

    private static final String T3K_MD5_DESCRIPTION = String.format("%s|%s", T3K_MD5_HIT, "Description");

    private static final String T3K_MD5_ID = String.format("%s|%s", T3K_MD5_HIT, "ID");

    protected MD5Metadata(CustomMetadataMap map) {
        super(map);
    }

    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_MD5_TYPE;
    }

    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof MD5Detection)) {
            LOG.error("The detection is not an MD5 hit but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        MD5Detection hit = (MD5Detection)detection;
        getMetadataMap().putInteger(String.format(T3K_MD5_ID, detectionIndex), hit.getId());
        getMetadataMap().putText(String.format(T3K_MD5_TYPE, detectionIndex), hit.getType());
        getMetadataMap().putText(String.format(T3K_MD5_DESCRIPTION, detectionIndex), hit.getDescription());
        getMetadataMap().putText(String.format(T3K_MD5_HASH, detectionIndex), hit.getHash());

    }
}
