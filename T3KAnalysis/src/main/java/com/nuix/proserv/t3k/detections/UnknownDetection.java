package com.nuix.proserv.t3k.detections;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * This is a generic detection to handle cases where the specific type of the Detection cannot be identified or
 * parsed.  The original data will be stored in an instance of this class and will be accessible via the
 * {@link #forEachDetail(BiConsumer)} method.
 */
public class UnknownDetection extends Detection {
    private static final long serialVersionUID = 1L;

    private Map<String, Object> details;

    protected UnknownDetection() {}

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

        // Reduce the source data to any entries without null values and store them in an immutable copy
        // The null removal is a pre-requisite for the immutable copy
        detection.details = Map.copyOf(detectionData.entrySet().stream().reduce(new HashMap<String, Object>(),
                (content, entry) -> {
                    // Only accumulate if there are no nulls in the entry
                    if (entry.getKey() != null && entry.getValue() != null) {
                        content.put(entry.getKey(), entry.getValue());
                    }
                    return content;
                }, (map1, map2) -> {
                    // Then combine into a single map
                    map1.putAll(map2);
                    return map1;
                }));

        return detection;
    }
}
