package com.nuix.proserv.t3k.results;

import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@ToString
public abstract class ResultMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    private long id;

    @Getter
    private String file_path;
}
