package com.nuix.proserv.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.PersonDetection;
import com.nuix.proserv.ws.metadata.T3KMetadata;
import lombok.Getter;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class DetectionMetadata implements T3KMetadata {
    private static final Logger LOG = LogManager.getLogger(DetectionMetadata.class.getCanonicalName());

    private static final Map<String, Class<? extends DetectionMetadata>> detectionMetadataMap = Map.of(
            PersonDetection.TYPE, PersonMetadata.class
    );

    @Getter
    private final CustomMetadataMap metadataMap;

    protected DetectionMetadata(CustomMetadataMap map) {
        this.metadataMap = map;
    }


    public abstract String getTypeTemplate(Detection detection);

    public abstract void applyDetection(Detection detection, int detectionIndex);

    public static DetectionMetadata getMetadataForDetection(Detection detection, CustomMetadataMap metadataMap) {
        String detectionType = detection.getType();

        Class<? extends DetectionMetadata> metadataClass = detectionMetadataMap.getOrDefault(detectionType, UnknownMetadata.class);
        try {
            return metadataClass.getConstructor(CustomMetadataMap.class).newInstance(metadataMap);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOG.warn("The class for the metadata for detection type {} could not be instantiated, using a default instance.", detectionType);
            return new UnknownMetadata(metadataMap);
        }
    }
}
