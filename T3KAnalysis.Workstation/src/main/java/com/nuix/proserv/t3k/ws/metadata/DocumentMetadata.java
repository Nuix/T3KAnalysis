package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.DetectionWithData;
import com.nuix.proserv.t3k.detections.DocumentDetectionData;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata;
import nuix.CustomMetadataMap;

/**
 * An {@link AnalysisMetadata} implementation to handle {@link com.nuix.proserv.t3k.results.DocumentResult}s.
 * <p>
 *     The main function of this class is to add Page and Image indexes to the generated custom metadata.
 * </p>
 */
public class DocumentMetadata extends AnalysisMetadata {
    /**
     * Custom metadata field for the document page a detection belongs to.  This is a Format String.  The initial
     * placeholder should be replaced with the parent detection label (for example "T3K Detections|1|army_tank") such
     * that the page is added as a child to the original detection.
     */
    private static final String T3K_PAGE = METADATA_LEVEL_SEPARATOR + "Page";

    /**
     * Custom metadata field for the document image a detection belongs to.  This is a Format String.  The initial
     * placeholder should be replaced with the parent detection label (for example "T3K Detections|1|army_tank") such
     * that the image is added as a child to the original detection.
     */
    private static final String T3K_IMAGE = METADATA_LEVEL_SEPARATOR + "Image";

    /**
     * Each detection added to the custom metadata will be assigned an index, with the index added to the top level
     * metadata and the detection added as a child to the index (like a list).  This counter tracks the detection count
     * on the item represented by this object.  Applied indexes will start at 1.
     */
    private int detectionCounter = 0;


    /**
     * Create a DocumentMetadata instance, deferring all work to the super-class.
     * @param metadataMap The {@link CustomMetadataMap} to store the data in
     * @param analysisResult The {@link AnalysisResult} that is the source of data to store.
     */
    public DocumentMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        super(metadataMap, analysisResult);
    }

    /**
     * {@inheritDoc}
     * <p>
     *     The {@link com.nuix.proserv.t3k.results.DocumentResult} can add {@link DocumentDetectionData} to certain
     *     Detection types, so if the detection implements {@link DetectionWithData}, then this method will also
     *     retrieve that data and apply it to the custom metadata for this instance.
     * </p>
     * @param detection the {@link Detection} whose values should be added to the custom metadata.
     */
    @Override
    protected void addDetection(Detection detection) {
        detectionCounter++; // Note: applied indexes start at 1.

        DetectionMetadata detectionMetadata = DetectionMetadata.getMetadataForDetection(detection, getMetadataMap());
        detectionMetadata.applyDetection(detection, detectionCounter);

        if (detection instanceof DetectionWithData) {
            DetectionWithData dataDetection = (DetectionWithData)detection;

            String detectionBase = String.format(detectionMetadata.getTypeTemplate(detection), detectionCounter);
            String pageLabel = detectionBase + T3K_PAGE;
            String imageLabel = detectionBase + T3K_IMAGE;

            getMetadataMap().putInteger(pageLabel, ((DocumentDetectionData)dataDetection.getData()).getPageNumber());
            getMetadataMap().putInteger(imageLabel, ((DocumentDetectionData)dataDetection.getData()).getImageNumber());
        }

    }
}
