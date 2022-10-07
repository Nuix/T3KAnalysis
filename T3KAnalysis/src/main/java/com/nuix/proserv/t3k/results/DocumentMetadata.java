package com.nuix.proserv.t3k.results;

import lombok.Getter;

import java.util.Arrays;
import java.util.function.IntConsumer;

public class DocumentMetadata extends ResultMetadata {
    private static final long serialVersionUID = 1L;

    @Getter
    private int document_total_page_number;

    @Getter
    private int document_total_image_number;

    @Getter
    private boolean document_has_embedded_text;

    private int[] document_analyzed_image_ids = new int[0];

    @Getter
    private String md5;

    @Getter
    private String sha1;

    public int getAnalyzedImageIdCount() {
        return document_analyzed_image_ids.length;
    }

    public void forEachAnalyzedImageId(IntConsumer consumer) {
        Arrays.stream(document_analyzed_image_ids).forEach(consumer);
    }
}
