package com.nuix.proserv.t3k.detections;

public interface DetectionWithData {
    String DATA = "data";

    DetectionData<?> getData();

    void setData(DetectionData<?> data);
}
