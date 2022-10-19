package com.nuix.proserv.t3k;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuix.proserv.restclient.RestClient;
import com.nuix.proserv.restclient.SimpleResponse;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.PollResults;
import com.nuix.proserv.t3k.results.UploadResult;
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
    public static final String LOGGER_NAME = "com.nuix.proserv.t3k";
    private static final Logger LOG = LogManager.getLogger(LOGGER_NAME);
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
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(UploadResult.class, new UploadResult.Deserializer());
        Gson gson = builder.create();

        // Using arrays to hold the mutable values as the outer scope to lambdas is considered final.
        long[] sourceIdHolder = { sourceId };
        long[] resultIdHolder = { -1L };

        doWithRetries(() -> {

            Map<String, Object> uploadBody = Map.of(String.valueOf(sourceIdHolder[0]), serverPath);
            LOG.debug("Upload Body: {}", uploadBody);

            SimpleResponse uploadRequestResults = this.client.post(Endpoint.UPLOAD.get(), uploadBody);
            LOG.debug("Upload Results: {}", uploadRequestResults);

            int resultCode = (Integer)uploadRequestResults.getCode();

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
                    String resultsBody = uploadRequestResults.getBodyAsString();
                    UploadResult result = gson.fromJson(resultsBody, UploadResult.class);
                    result.forEachId(id -> {
                        if (-1L == resultIdHolder[0]) resultIdHolder[0] = id;
                    });
                    return true;
                default:
                    if (resultCode >= 500 && resultCode <= 599) {
                        // Server error.  Try again.
                        LOG.info("Server Error: [{}] {}: {}",
                                uploadRequestResults.getMessage(),
                                resultCode,
                                uploadRequestResults.getBodyAsString()
                        );

                        return false;
                    } else {
                        // Unexpected value.  Assume it is bad and retry
                        LOG.info("Unexpected return value.  Trying again.  [{}] {}: {}",
                           uploadRequestResults.getMessage(),
                           resultCode,
                           uploadRequestResults.getBodyAsString()
                        );

                        return  false;
                    }
            }
        });

        return resultIdHolder[0];
    }

    public Map<Long, Long> batchUpload(Map<Long, String> itemsToUpload) {
        LOG.trace("Uploading Batch: {}", itemsToUpload);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(UploadResult.class, new UploadResult.Deserializer());
        Gson gson = builder.create();

        Map<Long, Long> sourceIdToResultIdMap = new HashMap<>();

        Map<String, Object> uploadBody = new HashMap<>();

        for(Long sourceId : itemsToUpload.keySet()) {
            String path = itemsToUpload.get(sourceId);
            uploadBody.put(String.valueOf(sourceId), path);
        }

        doWithRetries(() -> {
            SimpleResponse uploadRequestResults = this.client.post( Endpoint.UPLOAD.get(), uploadBody );
            LOG.debug("Batch Upload Response: {}", uploadRequestResults);

            int returnCode = (int)uploadRequestResults.getCode();
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
                    String resultsBody = uploadRequestResults.getBodyAsString();
                    UploadResult results = gson.fromJson(resultsBody, UploadResult.class);
                    results.forEachId(resultKey -> {
                        String resultPath = results.get(resultKey);
                        if(null != resultPath) {
                            long sourceId = itemsToUpload.entrySet().stream()
                                    .filter(entry -> resultPath.equals(entry.getValue()))
                                    .map(Map.Entry::getKey).findFirst().get();

                            sourceIdToResultIdMap.put(sourceId, resultKey);
                        }
                    });
                    return true;
                default:
                    return handleServerErrors(uploadRequestResults);

            }
        });

        return sourceIdToResultIdMap;
    }

    public PollResults poll(long itemId) {
        LOG.trace("Polling for {}", itemId);
        Gson gson = new Gson();

        // Using a lambda function for the retries.  The variables in the enclosing scope are final for the lambda
        // so using an array to hold the reference to the results
        PollResults[] pollResultsHolder = new PollResults[] { null };

        String pollEndpoint = Endpoint.POLL.get(String.valueOf(itemId));
        LOG.debug("Polling Endpoint: {}", pollEndpoint);

        doWithRetries(() -> {
            SimpleResponse pollResponse = client.get(pollEndpoint);
            LOG.debug("Poll Response: {}", pollResponse);

            int pollResponseCode = (Integer)pollResponse.getCode();
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
                    PollResults pollResults = gson.fromJson(pollResponse.getBodyAsString(), PollResults.class);
                    pollResultsHolder[0] = pollResults;
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


        return pollResultsHolder[0];
    }

    private Boolean handleServerErrors(SimpleResponse requestResponse) {
        int responseCode = (int) requestResponse.getCode();
        if (500 <= responseCode && 599 >= responseCode) {
            // Server error.  Try again.
            LOG.info("Server Error: [{}] {}: {}",
                    requestResponse.getMessage(),
                    responseCode,
                    requestResponse.getBodyAsString()
            );

        } else {
            // Unexpected code.  Expect it to be bad and retry
            LOG.info(String.format(
                    "Unexpected return value.  Trying again.  [%s] %d: %s",
                    requestResponse.getMessage(),
                    responseCode,
                    requestResponse.getBodyAsString()
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

            PollResults results = poll(pollForId);
            LOG.debug("Poll Results: {}={}", pollForId, results);

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
            PollResults results = poll(id);

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
            SimpleResponse resultResponse = client.get(Endpoint.RESULT.get(String.valueOf(id)));

            int responseCode = resultResponse.getCode();
            String responseMessage = resultResponse.getMessage();
            String responseBody = resultResponse.getBodyAsString();
            LOG.debug("Results Response: {}, {}", responseCode, responseBody);

            switch(responseCode) {
                case 200:
                    // Success
                    AnalysisResult result = AnalysisResult.parseResult(responseBody);
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
