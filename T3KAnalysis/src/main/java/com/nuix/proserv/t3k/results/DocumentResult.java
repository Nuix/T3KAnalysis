package com.nuix.proserv.t3k.results;

import com.nuix.proserv.t3k.detections.DetectionWithData;
import com.nuix.proserv.t3k.detections.DocumentDetectionData;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.IntConsumer;

public class DocumentResult extends AnalysisResult implements HashedResult {
    public static final String TYPE = "document_type";
    public static final String PAGE_COUNT = "document_total_page_number";
    public static final String IMAGE_COUNT = "document_total_image_number";
    public static final String HAS_TEXT = "document_has_embedded_text";
    public static final String IMAGE_IDS = "document_analyzed_image_ids";

    @Getter
    private String md5;

    @Getter
    private String sha1;

    @Getter
    private String type;

    @Getter
    private int pageCount;

    @Getter
    private int imageCount;

    @Getter
    private boolean hasText;

    private int[] imageIds;

    private DocumentResult() {}

    public int imageIdCount() {
        if (null == imageIds) {
            return 0;
        } else {
            return imageIds.length;
        }
    }

    public void forEachImageId(IntConsumer consumer) {
        if (null != imageIds) {
            Arrays.stream(imageIds).forEach(consumer);
        }
    }

    @Override
    protected void addDataToDetection(DetectionWithData detection, Map<String, Object> detectionData) {
        Object[] data = (Object[])detectionData.getOrDefault(DetectionWithData.DATA, new Object[0]);
        DocumentDetectionData documentDetectionData = new DocumentDetectionData(data);
        detection.setData(documentDetectionData);
    }

    @Override
    public String toString() {
        final StringBuilder output = new StringBuilder(super.toString())
                .append(" Type: ").append(type)
                .append(" Has Text: ").append(hasText)
                .append(" MD5: ").append(md5)
                .append(" SHA1: ").append(sha1)
                .append(" Page: ").append(pageCount)
                .append(" Image: ").append(imageCount)
                .append(" Image IDs: [");

        forEachImageId(id -> {
            output.append(id).append(", ");
        });

        output.delete(output.length() - 2, output.length() - 1);
        output.append("]");

        return output.toString();
    }

    public static boolean isDocumentResult(Map<String, Object> metadata) {
        return metadata.containsKey(PAGE_COUNT);
    }

    public static DocumentResult parseResult(Map<String, Object> metadata) {
        if (isDocumentResult(metadata)) {

            DocumentResult result = new DocumentResult();

            result.pageCount = (int)metadata.get(PAGE_COUNT);
            result.imageCount = (int)metadata.get(IMAGE_COUNT);
            result.md5 = (String)metadata.get(MD5);
            result.sha1 = (String)metadata.get(SHA1);
            result.type = (String)metadata.get(TYPE);
            result.hasText = (boolean)metadata.get(HAS_TEXT);
            result.imageIds = (int[])metadata.get(IMAGE_IDS);

            DocumentResult.fillSharedFields(result, metadata);

            return result;

        } else {
            return null;
        }
    }
}
