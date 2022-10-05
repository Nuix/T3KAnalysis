package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.*;
import com.nuix.proserv.t3k.ws.ScriptingBase;
import com.nuix.proserv.t3k.ws.metadata.T3KMetadata;
import lombok.Getter;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class DetectionMetadata implements T3KMetadata {
    protected static final Logger LOG = LogManager.getLogger(ScriptingBase.LOGGING_NAME);

    private static final Map<String, Class<? extends DetectionMetadata>> detectionMetadataMap = Map.of(
            PersonDetection.TYPE, PersonMetadata.class,
            ObjectDetection.TYPE, ObjectMetadata.class,
            MD5Detection.TYPE, MD5Metadata.class,
            TextDetection.TYPE, TextMetadata.class
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
