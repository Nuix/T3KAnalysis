package com.nuix.proserv.t3k.detections;

import com.google.gson.*;
import com.nuix.proserv.t3k.T3KApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

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
public class DetectionTypeMap implements JsonDeserializer<Detection> {
    private static final Logger LOG = LogManager.getLogger(T3KApi.LOGGER_NAME);

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
    public static Detection getDetection(Map<String, Object> detectionData) {
        LOG.debug("Detection Map: {}", DETECTION_TYPE_MAP);

        String type = (String) detectionData.get(Detection.TYPE);
        LOG.debug("Type: {}", type);

        Class<? extends Detection> detectionClass = DETECTION_TYPE_MAP.getOrDefault(type, UnknownDetection.class);
        LOG.debug("Class: {}", detectionClass);

        try {
            Method[] methods = detectionClass.getMethods();
            Arrays.stream(methods).forEach(method -> LOG.debug("Found: {}", method));

            Method parseMethod = detectionClass.getMethod("parseDetection", Map.class);
            LOG.debug("Debug Method: {}", parseMethod);

            Object detection = parseMethod.invoke(null, detectionData);
            LOG.debug("Detection Parsed: {}", detection);
            return (Detection)parseMethod.invoke(null, detectionData);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOG.error("{} caused by {}: {}", e.getMessage(), e.getCause().getMessage(), Arrays.stream(e.getCause().getStackTrace()).map(String::valueOf));
            LOG.warn("The type returned for the detection \"{}\" does not have an accessible parseDetection method.  Proving an UnknownDetection instead.", type);
            return UnknownDetection.parseDetection(detectionData);
        }

    }

    private static UnknownDetection jsonToUnknownDetection(JsonObject jsonObject) {
        return UnknownDetection.parseDetection(jsonObject.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public Detection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement typeElement = jsonObject.get("type");
        LOG.debug("Read Type: {}", typeElement);

        if (null == typeElement) {
            LOG.error("Detection type could not be read from json.  Using a generic detection type. {}", json.toString());
            return jsonToUnknownDetection(jsonObject);
        }

        String type = typeElement.getAsString();
        Class<? extends Detection> detectionImplementation = DETECTION_TYPE_MAP.getOrDefault(type, UnknownDetection.class);
        if(UnknownDetection.class.equals(detectionImplementation)) {
            LOG.error("Detection type is not an expected type {}.  Using a generic detection type.", type);
            return jsonToUnknownDetection(jsonObject);
        }

        if(MD5Detection.class.equals(detectionImplementation)) {
            JsonElement hit = jsonObject.get(MD5Detection.HIT);
            return context.deserialize(hit, detectionImplementation);
        }

        if (TextDetection.class.equals(detectionImplementation)) {
            return new TextDetection.Deserializer().deserialize(json, typeOfT, context);
        }

        return context.deserialize(json, detectionImplementation);

    }
}
