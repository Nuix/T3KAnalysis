package com.nuix.proserv.t3k.detections;

import lombok.Getter;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * This is a generic detection to handle cases where the specific type of the Detection cannot be identified or
 * parsed.  The original data will be stored in an instance of this class and will be accessible via the
 * {@link #forEachDetail(BiConsumer)} method.
 */
public class UnknownDetection extends Detection {
    private Map<String, Object> details;

    private UnknownDetection() {}

    public int getDetailCount() {
        return details.size();
    }

    public void forEachDetail(BiConsumer<String, Object> consumer) {
        details.forEach(consumer);
    }

    @Override
    public String toString() {
        return super.toString() + " :Unknown Type: " + details.toString();
    }

    /**
     * Create a new UnknownDetection instance by copying (shallowly) the data inside the
     * provided detection data.
     * @param detectionData The details for the detection, accessible by the {@link #forEachDetail(BiConsumer)}
     *                      method.
     * @return An instance of UnknownDetection.
     */
    public static UnknownDetection parseDetection(Map<String, Object> detectionData) {
        UnknownDetection detection = new UnknownDetection();

        detection.details = Map.copyOf(detectionData);

        return detection;
    }
}
