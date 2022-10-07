package com.nuix.proserv.t3k.results;

import java.util.Map;

public class DocumentResult extends AnalysisResult {
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "document_type";
    public static final String PAGE_COUNT = "document_total_page_number";

    private DocumentMetadata metadata;

    protected DocumentResult() {}

    @Override
    public ResultMetadata getMetadata() {
        return metadata;
    }

    public static boolean isDocumentResult(Map<String, Object> metadata) {
        return metadata.containsKey(PAGE_COUNT);
    }
}
