package com.nuix.proserv.t3k.detections;

public class UnknownDetectionData implements DetectionData<String>{
    private static final long serialVersionUID = 1L;

    private final String data;

    public UnknownDetectionData(String data) {
        this.data = data;
    }

    @Override
    public String getData() {
        return data;
    }
}
