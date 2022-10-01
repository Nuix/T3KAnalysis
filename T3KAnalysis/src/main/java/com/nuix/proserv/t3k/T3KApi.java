package com.nuix.proserv.t3k;

import com.nuix.proserv.restclient.RestClient;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.PollResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

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
            LOG.debug("Try #{}", currentAttempt+1);

            try {
                doTry = !action.call();
            } catch (Exception e) {
                LOG.error("Received {} when trying an action.  Assuming failure and trying again.",
                        e.getMessage(), e);
            }

            if(doTry) {
                try {
                    Thread.sleep((long)(retryDelay * 1000));
                } catch (InterruptedException e) {
                    // Don't care just keep going
                }

                currentAttempt += 1;

                LOG.warn("Retrying task ({} / {})", currentAttempt, retryCount);
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
        LOG.trace("Uploading #{} at {}", sourceId, serverPath);

        // Using arrays to hold the mutable values as the outer scope to lambdas is considered final.
        long[] sourceIdHolder = { sourceId };
        long[] resultIdHolder = { 0L };

        doWithRetries(() -> {

            Map<String, Object> uploadBody = Map.of(String.valueOf(sourceIdHolder[0]), serverPath);
            LOG.debug("Upload Body: {}", uploadBody);

            Map<String, Object> uploadRequestResults = this.client.post(Endpoint.UPLOAD.get(), uploadBody);
            LOG.debug("Upload Results: {}", uploadRequestResults);

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
                    LOG.warn("The provided Source ID ({}) was already used.  Trying with the next one.",
                            sourceIdHolder[0]);
                    sourceIdHolder[0] = sourceIdHolder[0] + 1;
                    return false;
                case 200:
                    // Success, finish up
                    Map<String, Object> resultsBody = (Map<String, Object>)uploadRequestResults.get("body");
                    if (resultsBody.isEmpty()) {
                        resultIdHolder[0] = 0;
                    } else {
                        long resultId = Long.parseLong(resultsBody.keySet().iterator().next());
                        resultIdHolder[0] = resultId;
                    }
                    return true;
                default:
                    if (resultCode >= 500 && resultCode <= 599) {
                        // Server error.  Try again.
                        LOG.info("Server Error: [{}] {}: {}",
                                uploadRequestResults.get("message"),
                                resultCode,
                                uploadRequestResults.get("body")
                        );

                        return false;
                    } else {
                        // Unexpected value.  Assume it is bad and retry
                        LOG.info("Unexpected return value.  Trying again.  [{}] {}: {}",
                           uploadRequestResults.get("message"),
                           resultCode,
                           uploadRequestResults.get("body")
                        );

                        return  false;
                    }
            }
        });

        return resultIdHolder[0];
    }

    public Map<Long, Long> batchUpload(Map<Long, String> itemsToUpload) {
        LOG.trace("Uploading Batch: {}", itemsToUpload);

        Map<Long, Long> sourceIdToResultIdMap = new HashMap<>();

        Map<String, Object> uploadBody = new HashMap<>();

        for(Long sourceId : itemsToUpload.keySet()) {
            String path = itemsToUpload.get(sourceId);
            uploadBody.put(String.valueOf(sourceId), path);
        }

        doWithRetries(() -> {
            Map<String, Object> uploadRequestResults = this.client.post( Endpoint.UPLOAD.get(), uploadBody );
            LOG.debug("Batch Upload Response: {}", uploadRequestResults);

            int returnCode = (int)uploadRequestResults.get("code");
            switch (returnCode) {
                case 434:
                    // Invalid source id.  No easy fix for this in batch mode so throw an error.
                    throw new T3KApiException(String.format(
                            "Uploading a batch of items found invalid source ids and needs to be redone with unique ids. %s",
                            itemsToUpload
                    ));
                case 400:
                    // Malformed request.  Fail
                    throw new T3KApiException(String.format(
                            "The Upload request was malformed and cannot be repaired.  [POST]: %s B: %s",
                            Endpoint.UPLOAD.get(),
                            uploadBody.toString()
                    ));
                case 200:
                    // Success.  Map results to sources
                    Map<String, Object> resultsBody = (Map<String, Object>)uploadRequestResults.get("body");
                    resultsBody.forEach((resultKey, filePath ) -> {
                        long sourceId = itemsToUpload.entrySet().stream()
                                .filter(entry -> filePath.equals(entry.getValue()))
                                .map(Map.Entry::getKey).findFirst().get();
                        long resultId = Long.parseLong(resultKey);

                        sourceIdToResultIdMap.put(sourceId, resultId);
                    });
                    return true;
                default:
                    return handleServerErrors(uploadRequestResults);

            }
        });

        return sourceIdToResultIdMap;
    }

    public Map<String, Object> poll(long itemId) {
        LOG.trace("Polling for {}", itemId);

        // Using a lambda function for the retries.  The variables in the enclosing scope are final for the lambda
        // so using a map to have a modifiable container.  Results will be keyed to "value"
        Map<String, Map<String, Object>> pollResultsHolder = new HashMap<>();

        String pollEndpoint = Endpoint.POLL.get(String.valueOf(itemId));
        LOG.debug("Polling Endpoint: {}", pollEndpoint);

        doWithRetries(() -> {
            Map<String, Object> pollResponse = client.get(pollEndpoint);
            LOG.debug("Poll Response: {}", pollResponse);

            int pollResponseCode = (Integer)pollResponse.get("code");
            switch(pollResponseCode) {
                case 433:
                    // The file was broken.  Treat it as a successful poll but log an error.
                    LOG.warn("The upload with index #{} was broken.  Poll is complete.", itemId);
                    // Intentionally fall into the successes as this poll attempt completed
                case 210:
                    // The request is still processing.  We aren't waiting for completion here so treat as success
                    // Intentionally falling into the 200 result.
                case 200:
                    // Poll successful.  Work still may not be done, so client needs to check
                    pollResultsHolder.put("value", (Map<String, Object>)pollResponse.get("body"));
                    return true;
                case 400:
                    // The request was malformed.
                    throw new T3KApiException(String.format(
                       "The polling request was malformed.  The Endpoint called was %s.", pollEndpoint
                    ));
                case 404:
                    // The item being polled for doesn't exist on the server
                    throw new T3KApiException(String.format(
                        "The item id used for polling (%d) is not recognized", itemId
                    ));
                default:
                    return handleServerErrors(pollResponse);
            }
        });


        return pollResultsHolder.get("value");
    }

    private Boolean handleServerErrors(Map<String, Object> requestResponse) {
        int responseCode = (int) requestResponse.get("code");
        if (500 <= responseCode && 599 >= responseCode) {
            // Server error.  Try again.
            LOG.info("Server Error: [{}] {}: {}",
                    requestResponse.get("message"),
                    responseCode,
                    requestResponse.get("body")
            );

        } else {
            // Unexpected code.  Expect it to be bad and retry
            LOG.info(String.format(
                    "Unexpected return value.  Trying again.  [%s] %d: %s",
                    requestResponse.get("message"),
                    responseCode,
                    requestResponse.get("body")
            ));

        }
        return false;
    }

    static private class CycleQueueEntry {
        final long id;
        int cycle = 0;

        private final Object lock = new Object();

        CycleQueueEntry(long id) {
            this.id = id;
        }

        int incrementCycle() {
            synchronized (lock) {
                return ++cycle;
            }
        }

        int getCycle() {
            synchronized (lock) {
                return cycle;
            }
        }

        long getId() {
            return id;
        }
    }

    public void waitForBatch(Collection<Long> itemsToWaitFor, Queue<PollResults> completedItems, Consumer<Integer> callback) {
        LOG.trace("Waiting for these items: {}", itemsToWaitFor);

        int currentCycle = 0;
        int completedItemCount = 0;
        int numberOfItems = itemsToWaitFor.size();

        Queue<CycleQueueEntry> queuedItems = new ConcurrentLinkedQueue<>();
        itemsToWaitFor.forEach(itemId -> queuedItems.add(new CycleQueueEntry(itemId)));

        LOG.debug("Starting cycle 1");
        while (!queuedItems.isEmpty()) {
            CycleQueueEntry item = queuedItems.poll();

            // If this item is for the next cycle, wait a bit before checking and starting a new cycle
            if (item.getCycle() > currentCycle) {
                try {
                    Thread.sleep((long)(retryDelay * 1000));
                } catch (InterruptedException e) {
                    // Don't care...
                }

                currentCycle = item.getCycle();
                LOG.debug("Starting cycle {}", currentCycle + 1);
            }

            long pollForId = item.getId();
            LOG.debug("Polling for {}", pollForId);

            Map<String, Object> pollBody = poll(pollForId);
            LOG.debug("Poll Results: {}={}", pollForId, pollBody);

            PollResults results = PollResults.parseResults(pollBody);
            LOG.debug(results);

            if(results.isFinished()) {
                LOG.trace("Waiting on {} completed.", results.getId());

                completedItems.add(results);
                completedItemCount++;

                if(null != callback) {
                    LOG.trace("Updating callback with completed information.");

                    callback.accept(completedItemCount);
                }
            } else {
                LOG.trace("Item {} not yet complete, putting it back in queue", item.getId());

                item.incrementCycle();
                queuedItems.add(item);
            }
        }

        LOG.trace("Waiting on analysis completed.");
    }

    public PollResults waitForAnalysis(long id) {
        LOG.trace("Waiting for {}", id);

        // This method uses a lambda function to get results.  The container variables are final relative to the
        // lambda so use an array as a container.
        PollResults[] resultsHolder = { null };

        doWithRetries(() -> {
            Map<String, Object> pollBody = poll(id);

            PollResults results = PollResults.parseResults(pollBody);

            resultsHolder[0] = results;

            return results.isFinished();
        });

        return resultsHolder[0];
    }

    public AnalysisResult getResults(long id) {
        LOG.trace("Getting results for {}", id);


        // This method uses a lambda function to get results.  The container variables are final relative to the
        // lambda so use an array as a container.
        AnalysisResult[] analysisResults = { null };

        doWithRetries(() -> {
            String endpoint = Endpoint.RESULT.get(String.valueOf(id));
            Map<String, Object> resultResponse = client.get(Endpoint.RESULT.get(String.valueOf(id)));

            int responseCode = (int)resultResponse.get("code");
            String responseMessage = (String)resultResponse.get("message");
            Object responseBody = resultResponse.get("body");
            LOG.debug("Results Response: {}, {}", responseCode, responseBody);

            switch(responseCode) {
                case 200:
                    // Success
                    AnalysisResult result = AnalysisResult.parseResult((Map<String, Object>)responseBody);
                    analysisResults[0] = result;
                    return true;
                case 400:
                    // "Request was broken, not likely to fix it so error out
                    throw new T3KApiException(String.format(
                       "Malformed request to the server.  Endpoint: %s, ID: %d [%s]",
                            Endpoint.RESULT, id, endpoint
                    ));
                default:
                    return handleServerErrors(resultResponse);
            }
        });

        return analysisResults[0];
    }
}
