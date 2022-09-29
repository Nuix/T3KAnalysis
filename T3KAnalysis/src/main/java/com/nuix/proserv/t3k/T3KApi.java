package com.nuix.proserv.t3k;

import com.nuix.proserv.restclient.RestClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class T3KApi {
    private static final Logger LOG = LogManager.getLogger(T3KApi.class.getCanonicalName());
    private final RestClient client;
    private final int batchSize;
    private final int retryCount;
    private final float retryDelay;

    public T3KApi(String t3kCOREHost, int t3kCOREPort, int batchSize, int retryCount, float retryDelayInSeconds) {
        this.client = new RestClient(t3kCOREHost, t3kCOREPort);
        this.batchSize = batchSize;
        this.retryCount = retryCount;
        this.retryDelay = retryDelayInSeconds;
    }

    public void doWithRetries(Callable<Boolean> action) {
        int currentAttempt = 0;

        boolean doTry = true;

        while(doTry && ((retryCount < 0) || (currentAttempt <= retryCount))) {
            LOG.debug(String.format("Try #%d", currentAttempt+1));

            try {
                doTry = !action.call();
            } catch (Exception e) {
                LOG.error(String.format("Received %s when trying an action.  Assuming failure and trying again.",
                        e.getMessage()), e);
            }

            if(doTry) {
                try {
                    Thread.sleep((long)(retryDelay * 1000));
                } catch (InterruptedException e) {
                    // Don't care just keep going
                }

                currentAttempt += 1;

                LOG.warn(String.format("Retrying task (%d / %d)", currentAttempt, retryCount));
            }
        }

        if(doTry) {
            // Ended the loop because we ran out of retries...
            throw new T3KApiException(
                    String.format("Exceeded %d retries when performing an action.  Task is not complete.", retryCount)
            );
        }

        LOG.trace("Finished action.");
    }

    public long upload(long sourceId, String serverPath) {
        // Using arrays to hold the mutable values as the outer scope to lambdas is considered final.
        long[] sourceIdHolder = { sourceId };
        long[] resultIdHolder = { 0L };

        doWithRetries(() -> {

            Map<String, Object> uploadBody = Map.of(String.valueOf(sourceIdHolder[0]), serverPath);
            Map<String, Object> uploadRequestResults = this.client.post(Endpoint.UPLOAD.get(),
                    null, null, uploadBody, false, null );

            int resultCode = (Integer)uploadRequestResults.get("code");

            switch (resultCode) {
                case 400:
                    // Malformed request.  Fail
                    throw new T3KApiException(String.format(
                            "The Upload request was malformed and cannot be repaired.  [POST]: %s B: %s",
                            Endpoint.UPLOAD.get(),
                            uploadBody.toString()
                            ));
                case 434:
                    // The source ID is invalid, increment and try again
                    sourceIdHolder[0] = sourceIdHolder[0] + 1;
                    return false;
                case 200:
                    // Success, finish up
                    resultIdHolder[0] = Long.parseLong(
                            ((Map<String, Object>)uploadRequestResults.get("body"))
                                    .keySet().stream().findFirst().get()
                    );
                    return true;
                default:
                    if (resultCode >= 500 && resultCode <= 599) {
                        // Server error.  Try again.
                        LOG.info(String.format(
                                "Server Error: [%s] %d: %s",
                                uploadRequestResults.get("message"),
                                resultCode,
                                uploadRequestResults.get("body")
                        ));

                        return false;
                    } else {
                        // Unexpected value.  Assume it is bad and retry
                        LOG.info(String.format(
                           "Unexpected return value.  Trying again.  [%s] %d: %s",
                           uploadRequestResults.get("message"),
                           resultCode,
                           uploadRequestResults.get("body")
                        ));
                    }
            }

            // Shouldn't be able to get here.
            return false;
        });

        return resultIdHolder[0];
    }

    public Map<Long, Long> batchUpload(Map<Long, String> itemsToUpload) {
        Map<Long, Long> sourceIdToResultIdMap = new HashMap<>();

        Map<String, Object> body = new HashMap<>();

        for(Long sourceId : itemsToUpload.keySet()) {
            String path = itemsToUpload.get(sourceId);
            body.put(String.valueOf(sourceId), path);
        }

        doWithRetries(() -> {
            Map<String, Object> uploadRequestResults = this.client.post(
                    Endpoint.UPLOAD.get(),
                    null, null, body, false, null
            );

            int returnCode = (int)uploadRequestResults.get("code");
            switch (returnCode) {
                case 434:
                    // Invalid source id.  No easy fix for this in batch mode so throw an error.
                    throw new T3KApiException(String.format(
                            "Uploading a batch of items found invalid source ids and needs to be redone with unique ids. %s",
                            itemsToUpload
                    ));
                case 400:

            }

            // Shouldn't get here
            return false;
        });

        return sourceIdToResultIdMap;
    }
}
