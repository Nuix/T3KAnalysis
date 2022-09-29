package com.nuix.proserv.t3k.results;

import java.util.Map;

public class DocumentResult extends AnalysisResult implements HashedResult {
    public static final String TYPE = "document_type";
    public static final String PAGE_COUNT = "document_total_page_number";
    public static final String IMAGE_COUNT = "document_total_image_number";
    public static final String HAS_TEXT = "document_has_embedded_text";
    public static final String IMAGE_IDS = "document_analyzed_image_ids";

    // TODO Implement isDocumentResult
    public static boolean isDocumentResult(Map<String, Object> resultsData) {
        return false;
    }
}
