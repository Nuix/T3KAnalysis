package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.DetectionWithData;
import com.nuix.proserv.t3k.detections.VideoDetectionData;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata;
import nuix.CustomMetadataMap;

/**
 * An {@link AnalysisMetadata} implementation to handle {@link com.nuix.proserv.t3k.results.VideoResult}s.
 * <p>
 *     The main function of this class is to add Frame indexes to the generated custom metadata.
 * </p>
 */
public class VideoMetadata extends AnalysisMetadata {
    /**
     * Custom metadata field for the video frame a detection was found on.  This is a Format String.  The initial
     * placeholder should be replaced with the parent detection label (for example "T3K Detections|1|army_tank") such
     * that the frame is added as a child to the original detection.
     */
    private static final String T3K_DATA = METADATA_LEVEL_SEPARATOR + "Frame";

    /**
     * Each detection added to the custom metadata will be assigned an index, with the index added to the top level
     * metadata and the detection added as a child to the index (like a list).  This counter tracks the detection count
     * on the item represented by this object.  Applied indexes will start at 1.
     */
    private int detectionCounter = 0;

    /**
     * Create a VideoMetadata instance, deferring all work to the super-class.
     * @param metadataMap The {@link CustomMetadataMap} to store the data in
     * @param analysisResult The {@link AnalysisResult} that is the source of data to store.
     */
    public VideoMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        super(metadataMap, analysisResult);
    }

    /**
     * {@inheritDoc}
     * <p>
     *     The {@link com.nuix.proserv.t3k.results.VideoResult} can add {@link VideoDetectionData} to certain
     *     Detection types, so if the detection implements {@link DetectionWithData}, then this method will also
     *     retrieve that data and apply it to the custom metadata for this instance.
     * </p>
     * @param detection the {@link Detection} whose values should be added to the custom metadata.
     */
    @Override
    protected void addDetection(Detection detection) {
        detectionCounter++;

        DetectionMetadata detectionMetadata = DetectionMetadata.getMetadataForDetection(detection, getMetadataMap());
        detectionMetadata.applyDetection(detection, detectionCounter);

        if (detection instanceof DetectionWithData) {
            DetectionWithData dataDetection = (DetectionWithData)detection;

            String detectionBase = String.format(detectionMetadata.getTypeTemplate(detection), detectionCounter);
            String frameLabel = detectionBase + T3K_DATA;

            getMetadataMap().putInteger(frameLabel, ((VideoDetectionData)dataDetection.getData()).getFrame());
        }
    }
}
