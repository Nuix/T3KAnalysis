package com.nuix.proserv.t3k.detections;

import com.nuix.proserv.t3k.T3KApiException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

public class DocumentDetectionData implements DetectionData<Integer[]> {
    private static final long serialVersionUID = 1L;

    public static final String PAGE = "document_page_number";
    public static final String IMAGE = "document_image_number";

    @Getter
    private final int pageNumber;

    @Getter
    private final int imageNumber;

    public DocumentDetectionData(int page, int image) {
        pageNumber = page;
        imageNumber = image;
    }

    @Override
    public String toString() {
        return String.format("Page: %d Image: %d", pageNumber, imageNumber);
    }

    @Override
    public Integer[] getData() {
        return new Integer[]{ pageNumber, imageNumber };
    }
}
