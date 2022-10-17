package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.results.AnalysisResult;
import nuix.CustomMetadataMap;

/**
 * An implementation of {@link AnalysisMetadata} for when the specific type of the {@link AnalysisResult} can not be
 * translated to another AnalysisMetadata implementation.
 * <p>
 *     This class will simply defer all actions to appropriate
 *     {@link com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata} implementations and adds no additional
 *     output.
 * </p>
 */
public class UnknownAnalysisTypeMetadata extends ImageMetadata {
    public UnknownAnalysisTypeMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        super(metadataMap, analysisResult);
    }
}
