package com.nuix.proserv.t3k.detections;

import com.nuix.proserv.t3k.T3KApi;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Map;

/**
 * Base class for all detections.  To create detections look up the correct detection using the
 * {@link DetectionTypeMap} class:
 * <pre>
 *     Map&lt;String, Object&gt; detectionData = ...
 *     Detection detection = DetectionTypeMap.getDetection(detectionData);
 * </pre>
 *
 * Each type of detection will have its own data and so the end consumer will need to know how to use each
 * one.
 */
public abstract class Detection implements Serializable {
    private static final long serialVersionUID = 1L;

    protected static final Logger LOG = LogManager.getLogger(T3KApi.LOGGER_NAME);
    
    public static final String TYPE = "type";
    public static final String INFO = "info";

    @Getter
    private String type;

    @Getter
    private String info;

    protected Detection() {}

    @Override
    public String toString() {
        return String.format("Type: %s, Info: %s", type, info);
    }


    protected static void fillSharedValues(Detection detection, Map<String, Object> detectionData) {
        detection.type = (String)detectionData.get(TYPE);

        detection.info = (String)detectionData.getOrDefault(INFO, "");
    }
}
