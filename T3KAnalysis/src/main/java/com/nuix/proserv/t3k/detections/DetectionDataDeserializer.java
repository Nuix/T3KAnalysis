package com.nuix.proserv.t3k.detections;

import com.google.gson.*;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.T3KApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;

public class DetectionDataDeserializer implements JsonDeserializer<DetectionData<?>> {
    private static final Logger LOG = LogManager.getLogger(T3KApi.LOGGER_NAME);

    @Override
    public DetectionData<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        JsonElement firstElement = jsonArray.get(0);

        if(firstElement.isJsonArray()) {
            // Document data, presumably
            // Document data: expect an array of 2 arrays.
            if (2 != jsonArray.size()) {
                throw new T3KApiException(String.format(
                        "Unexpected data.  Expected to have 2 values, each an array.  Found %d values.",
                        jsonArray.size()
                ));
            }

            JsonElement secondElement = jsonArray.get(1);
            if (!secondElement.isJsonArray()) {
                throw new T3KApiException(String.format(
                        "Unexpected data.  Expected to have 2 arrays in the data.  One element is not an array.  Found: %s",
                        secondElement.toString()
                ));
            }

            JsonArray firstArray = firstElement.getAsJsonArray();
            JsonArray secondArray = secondElement.getAsJsonArray();

            if (2 != firstArray.size() && 2 != secondArray.size()) {
                throw new T3KApiException(String.format(
                        "Unexpected data.  Expected to have 2 arrays each with 2 elements, a key and a value.  " +
                                "The arrays are the wrong size.  First array size: %d.  Second array size: %d",
                        firstArray.size(),
                        secondArray.size()
                ));
            }

            String firstKey = firstArray.get(0).getAsString();
            JsonElement firstData = firstArray.get(1);
            if (!firstData.isJsonPrimitive()) {
                throw new T3KApiException(String.format(
                        "Unexpected data.  Expected to the value in the data to be an integer, but it is not.  Found %s.",
                        firstData.toString()
                ));
            }

            String secondKey = secondArray.get(0).getAsString();
            JsonElement secondData = secondArray.get(1);
            if (!firstData.isJsonPrimitive()) {
                throw new T3KApiException(String.format(
                        "Unexpected data.  Expected to the value in the data to be an integer, but it is not.  Found %s.",
                        secondData.toString()
                ));
            }

            int pageNumber = -1;
            if (DocumentDetectionData.PAGE.equals(firstKey)) {
                pageNumber = firstData.getAsInt();
            } else if (DocumentDetectionData.PAGE.equals(secondKey)) {
                pageNumber = secondData.getAsInt();
            } else {
                throw new T3KApiException(String.format(
                        "Unexpected data.  Expected the key %s in one of the two data arrays, but it was not found.  Found %s and %s.",
                        DocumentDetectionData.PAGE,
                        firstKey,
                        secondKey
                ));
            }

            int imageNumber = -1;
            if (DocumentDetectionData.IMAGE.equals(firstKey)) {
                imageNumber = firstData.getAsInt();
            } else if (DocumentDetectionData.IMAGE.equals(secondKey)) {
                imageNumber = secondData.getAsInt();
            } else {
                throw new T3KApiException(String.format(
                        "Unexpected data.  Expected the key %s in one of the two data arrays, but it was not found.  Found %s and %s.",
                        DocumentDetectionData.IMAGE,
                        firstKey,
                        secondKey
                ));
            }

            return new DocumentDetectionData(pageNumber, imageNumber);
        } else if (firstElement.isJsonPrimitive()) {
            // Possibly a Video
            if(VideoDetectionData.VIDEO_FRAME.equals(firstElement.getAsString())) {
                // Video data: expect an array of length 2, the first element being the word "frame"
                if (2 != jsonArray.size()) {
                    throw new T3KApiException(String.format(
                            "Unexpected data.  Expected to have 2 values, \"frame\", and the frame number.  Found %d values.",
                            jsonArray.size()
                    ));
                }

                JsonElement secondElement = jsonArray.get(1);
                if (!secondElement.isJsonPrimitive()) {
                    throw new T3KApiException(String.format(
                            "Unexpected data.  Expected the frame to be of type int.  Found: %s",
                            secondElement.toString()
                    ));
                }

                int frame = jsonArray.get(1).getAsInt();
                return new VideoDetectionData(frame);
            } else {
                // Unexpected type of data
                LOG.warn("Unexpected data type found.  Returning a generic data value");
                return new UnknownDetectionData(json.getAsString());
            }
        } else {
            // Unexpected type of data
            LOG.warn("Unexpected data type found.  Returning a generic data value");
            return new UnknownDetectionData(json.getAsString());
        }
    }
}
