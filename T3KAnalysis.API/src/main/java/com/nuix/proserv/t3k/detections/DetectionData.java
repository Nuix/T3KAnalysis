package com.nuix.proserv.t3k.detections;

import java.io.Serializable;
import java.util.Map;

public interface DetectionData<T> extends Serializable {
    T getData();

}
