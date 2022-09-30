package com.nuix.proserv.t3k.detections;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public interface DetectionWithLocation {
    String BOX = "box";

    Rectangle2D.Double getBox();
}
