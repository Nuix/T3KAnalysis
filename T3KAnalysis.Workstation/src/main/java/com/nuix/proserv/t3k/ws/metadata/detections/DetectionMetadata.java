package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.*;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.ScriptingBase;
import com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata;
import com.nuix.proserv.t3k.ws.metadata.T3KMetadata;
import lombok.Getter;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class DetectionMetadata implements T3KMetadata {
    protected static final Logger LOG = LogManager.getLogger(ScriptingBase.LOGGING_NAME);

    private static final Map<String, Class<? extends DetectionMetadata>> DETECTION_METADATA_MAP = Map.of(
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

    private static Class<? extends DetectionMetadata> lookupImplementationOrDefault(Detection detection,
                                                                                   Class<? extends DetectionMetadata> defaultImpl) {

        Class<? extends DetectionMetadata> mdClass = null;

        for(Map.Entry<String, Class<? extends DetectionMetadata>> entry : DETECTION_METADATA_MAP.entrySet()) {
            try {
                Class<? extends Detection> resultClass = (Class<? extends Detection>)Class.forName(entry.getKey());

                if (resultClass.isAssignableFrom(detection.getClass())) {
                    mdClass = entry.getValue();
                    break;
                }
            } catch (ClassNotFoundException e) {
                LOG.error(e);
                LOG.error("The located class can not be loaded, ignoring and trying others. {}", entry.getKey());
            }
        }

        if (null == mdClass) {
            return defaultImpl;
        } else {
            return mdClass;
        }

    }

    public static DetectionMetadata getMetadataForDetection(Detection detection, CustomMetadataMap metadataMap) {
        String detectionType = detection.getType();

        if (null == detectionType) {
            LOG.warn("This is an unknown detection type.  Returning a generic metadata type");
            return new UnknownMetadata(metadataMap);
        }

        Class<? extends DetectionMetadata> metadataClass = DETECTION_METADATA_MAP.getOrDefault(detectionType, UnknownMetadata.class);
        try {
            //Constructor<?>[] constructors = metadataClass.getConstructors();
            Constructor<? extends DetectionMetadata> constructor = metadataClass.getDeclaredConstructor(CustomMetadataMap.class);
            constructor.setAccessible(true);
            DetectionMetadata detectionMetadata = constructor.newInstance(metadataMap);
            return detectionMetadata;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOG.warn("The class for the metadata for detection type {} could not be instantiated, using a default instance.", detectionType);
            return new UnknownMetadata(metadataMap);
        }
    }
}
