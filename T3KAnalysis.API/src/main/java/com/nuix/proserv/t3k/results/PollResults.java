package com.nuix.proserv.t3k.results;

import lombok.Getter;

public class PollResults {
    private static final long serialVersionUID = 1L;

    @Getter
    private long id;
    @Getter
    private String filepath;
    @Getter
    private String result_type;
    @Getter
    private boolean finished;
    @Getter
    private boolean pending;
    @Getter
    private boolean error;
    @Getter
    private boolean BROKEN_MEDIA;
    @Getter
    private boolean ID_NOT_FOUND;
    @Getter
    private boolean FILE_NOT_FOUND;
    @Getter
    private boolean VALID_MEDIA_OBJECT;

    protected PollResults() {}

    public String toString() {
        return filepath + " (ID " + id + ") of Type " + result_type +
                ":: Finished=" + finished + " [Valid: " + VALID_MEDIA_OBJECT + "]" +
                " Pending=" + pending +
                " Error=" + error + " [Broken=" + BROKEN_MEDIA +
                ", No ID=" + ID_NOT_FOUND +
                ", No File=" + FILE_NOT_FOUND + "]";
    }
}
