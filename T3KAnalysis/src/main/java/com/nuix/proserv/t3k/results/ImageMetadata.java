package com.nuix.proserv.t3k.results;

import lombok.Getter;
import lombok.ToString;

@ToString
public class ImageMetadata extends ResultMetadata {
    private static final long serialVersionUID = 1L;

    @Getter
    private String mode;

    @Getter
    private long file_size;

    @Getter
    private  String size;

    @Getter
    private String photoDNA;

    @Getter
    private String md5;

    @Getter
    private String sha1;

    @Getter
    private int width;

    @Getter
    private int height;
}
