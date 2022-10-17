package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata;
import nuix.CustomMetadataMap;

/**
 * An {@link AnalysisMetadata} implementation to handle {@link com.nuix.proserv.t3k.results.ImageResult} objects.
 * <p>
 *     Images do not apply specific data to their detections, and so this implementation just passes the the work on
 *     to the correct {@link DetectionMetadata} implementation.
 * </p>
 */
public class ImageMetadata extends AnalysisMetadata {

    /**
     * Each detection added to the custom metadata will be assigned an index, with the index added to the top level
     * metadata and the detection added as a child to the index (like a list).  This counter tracks the detection count
     * on the item represented by this object.  Applied indexes will start at 1.
     */
    private int detectionCounter = 0;

    /**
     * Create an ImageMetadata instance, deffering all work to the super-class.
     * @param metadataMap The {@link CustomMetadataMap} to store the data in
     * @param analysisResult The {@link AnalysisResult} that is the source of data to store.
     */
    public ImageMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        super(metadataMap, analysisResult);
    }

    /**
     * {@inheritDoc}
     * <p>
     *     The {@link com.nuix.proserv.t3k.results.ImageResult} does not add any
     *     {@link com.nuix.proserv.t3k.detections.DetectionData} to detections so this implementation simply gets
     *     the correct {@link DetectionMetadata} type and uses it to apply the custom metadata.
     * </p>
     * @param detection the {@link Detection} whose values should be added to the custom metadata.
     */
    @Override
    protected void addDetection(Detection detection) {
        detectionCounter++;

        DetectionMetadata detectionMetadata = DetectionMetadata.getMetadataForDetection(detection, getMetadataMap());
        detectionMetadata.applyDetection(detection, detectionCounter);
    }
}
