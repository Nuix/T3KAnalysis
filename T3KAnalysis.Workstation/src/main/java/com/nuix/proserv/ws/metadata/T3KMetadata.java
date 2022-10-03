package com.nuix.proserv.ws.metadata;

public interface T3KMetadata {
    String T3K_DETECTION = "T3K Detections";
    String T3K_COUNT = String.format("%s|%s", T3K_DETECTION, "Count");
    String T3K_TEMPLATE = T3K_DETECTION + "|%s|%s";

    String T3K_MATCH_FOUND = "Match Detected";
    String T3K_NO_MATCHES = "No Matches Detected";
}
