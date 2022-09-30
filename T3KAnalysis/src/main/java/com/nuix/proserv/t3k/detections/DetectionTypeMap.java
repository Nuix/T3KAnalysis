package com.nuix.proserv.t3k.detections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * This is a factory for Detection object.  Use the {@link #getDetection(Map)} method to get a Detection
 * implementation specific to the data provided.
 *
 * When making new Detection implementations, add it to the factory by adding to the DETECTION_TYPE_MAP.
 * The DETECTION_TYPE_MAP maps the type string that will be present in the data to the class that
 * implements the detection.  The class must extend {@link Detection}, and must have a
 * <code>public static Detection parseDetection(Map detectionData)</code> method.
 *
 * The map provided here internally uses the Map.of(...) construct.  This construct has a limit of 10
 * key/value pairs.  It is conceivable to reach this limit in which case the initialization of the
 * map will need to change.
 */
public class DetectionTypeMap {
    private static final Logger LOG = LogManager.getLogger(DetectionTypeMap.class.getCanonicalName());
    private static final Map<String, Class<? extends Detection>> DETECTION_TYPE_MAP = Collections.unmodifiableMap(
            Map.of(PersonDetection.TYPE, PersonDetection.class,
                    ObjectDetection.TYPE, ObjectDetection.class,
                    MD5Detection.TYPE, MD5Detection.class,
                    TextDetection.TYPE, TextDetection.class)
    );

    /**
     * Create a new instance of a Detection using the provided detection data.  The type of the detection is
     * inferred from the detection data (from the value of the "type" key) and an appropriate class is used to
     * parse and initialize the Detection instance.  If the type can not be deduced from the data or if the
     * means to parse the data is not found then an instance of {@link UnknownDetection} will be provided with
     * the data (copied) in it.
     *
     * @param detectionData A Map of all the data needed to create the detection.  The map must have a
     *                      "type" key that determines the type of the detection to make.
     * @return A Detection instance whose type is specific to the data provided.
     */
    private static Detection getDetection(Map<String, Object> detectionData) {
        String type = (String) detectionData.get(Detection.TYPE);

        Class<? extends Detection> detectionClass = DETECTION_TYPE_MAP.getOrDefault(type, UnknownDetection.class);

        try {
            Method parseMethod = detectionClass.getMethod("parseDetection", Map.class);
            return (Detection)parseMethod.invoke(null, detectionData);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOG.warn("The type returned for the detection \"{}\" does not have an accessible parseDetection method.  Proving an UnknownDetection instead.", type);
            return UnknownDetection.parseDetection(detectionData);
        }

    }
}
