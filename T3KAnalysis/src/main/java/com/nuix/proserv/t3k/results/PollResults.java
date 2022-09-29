package com.nuix.proserv.t3k.results;

import lombok.Getter;

import java.util.Map;

public class PollResults {
    public static final String ID = "id";
    public static final String FILEPATH = "filepath";
    public static final String FINISHED = "finished";
    public static final String PENDING = "pending";
    public static final String ERROR = "error";
    public static final String RESULT_TYPE = "result_type";
    public static final String BROKEN_MEDIA = "BROKEN_MEDIA";
    public static final String ID_NOT_FOUND = "ID_NOT_FOUND";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String VALID_MEDIA = "VALID_MEDIA_OBJECT";

    @Getter
    private final long id;
    @Getter
    private final String file;
    @Getter
    private final String type;
    @Getter
    private final boolean finished;
    @Getter
    private final boolean pending;
    @Getter
    private final boolean error;
    @Getter
    private final boolean brokenMedia;
    @Getter
    private final boolean idNotFound;
    @Getter
    private final boolean fileNotFound;
    @Getter
    private final boolean validMedia;

    private PollResults(long id, String file, String type, boolean finished, boolean pending, boolean error, boolean broken, boolean noId, boolean noFile, boolean valid) {
        this.id = id;
        this.file = file;
        this.finished = finished;
        this.pending = pending;
        this.error = error;
        this.type = type;
        this.brokenMedia = broken;
        this.idNotFound = noId;
        this.fileNotFound = noFile;
        this.validMedia = valid;
    }

    public String toString() {
        return file + " (ID " + id + ") of Type " + type +
                ":: Finished=" + finished + " [Valid: " + validMedia + "]" +
                " Pending=" + pending +
                " Error=" + error + " [Broken=" + brokenMedia +
                ", No ID=" + idNotFound +
                ", No File=" + fileNotFound + "]";
    }

    public static PollResults parseResults(Map<String, Object> body) {
        long id = (long)body.get(ID);
        String path = (String)body.get(FILEPATH);
        String type = (String)body.get(RESULT_TYPE);
        boolean finished = (boolean) body.get(FINISHED);
        boolean pending = !finished && (boolean) body.get(PENDING);
        boolean error = (boolean) body.get(ERROR);
        boolean valid = finished && body.containsKey(VALID_MEDIA);
        boolean broken = error && body.containsKey(BROKEN_MEDIA);
        boolean noId = error && body.containsKey(ID_NOT_FOUND);
        boolean noFile = error && body.containsKey(FILE_NOT_FOUND);

        return new PollResults(id, path, type, finished, pending, error, broken, noId, noFile, valid);
    }
}
