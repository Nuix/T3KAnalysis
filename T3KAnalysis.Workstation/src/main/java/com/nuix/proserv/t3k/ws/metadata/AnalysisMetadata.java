package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.DocumentResult;
import com.nuix.proserv.t3k.results.ImageResult;
import com.nuix.proserv.t3k.results.VideoResult;
import com.nuix.proserv.t3k.ws.ScriptingBase;
import lombok.Getter;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class AnalysisMetadata implements T3KMetadata {
    protected static final Logger LOG = LogManager.getLogger(ScriptingBase.LOGGING_NAME);

    private static final Map<String, Class<? extends AnalysisMetadata>> ANALYSIS_METADATA_TYPES = Map.of(
            ImageResult.class.getCanonicalName(), ImageMetadata.class,
            VideoResult.class.getCanonicalName(), VideoMetadata.class,
            DocumentResult.class.getCanonicalName(), DocumentMetadata.class
    );

    @Getter
    private final CustomMetadataMap metadataMap;

    @Getter
    private final AnalysisResult analysisResult;

    protected AnalysisMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        this.metadataMap = metadataMap;
        this.analysisResult = analysisResult;
    }

    public void applyResults() {
        int detectionCount = analysisResult.getDetectionCount();

        if(0 == detectionCount) {
            getMetadataMap().putText(T3K_DETECTION, T3K_NO_MATCHES);
        } else {
            getMetadataMap().putText(T3K_DETECTION, T3K_MATCH_FOUND);
            analysisResult.forEachDetection(this::applyDetection);
        }
    }

    protected abstract void applyDetection(Detection detection);

    public static AnalysisMetadata getInstance(AnalysisResult analysisResult, CustomMetadataMap metadataMap) {
        Class<? extends AnalysisMetadata> metadataClass = ANALYSIS_METADATA_TYPES.getOrDefault(analysisResult.getClass().getCanonicalName(), UnknownAnalysisTypeMetadata.class);
        try {
            return metadataClass.getConstructor(CustomMetadataMap.class, AnalysisResult.class).newInstance(metadataMap, analysisResult);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOG.error("A metadata implementation for the provided analysis type ({}) cannot be found or created.  Using a default one.", analysisResult.getClass().getCanonicalName());
            return new UnknownAnalysisTypeMetadata(metadataMap, analysisResult);
        }
    }
}
