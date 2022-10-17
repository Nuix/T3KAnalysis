package com.nuix.proserv.t3k.conn;

import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.PollResults;
import org.apache.commons.collections4.ListUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BatchAnalyzer extends Analyzer<List<String>> {

    protected BatchAnalyzer(T3KApi api, String configPath, Configuration configuration, BatchListener batchListener, ResultsListener resultsListener) {
        super(api, configPath, configuration, null, batchListener, resultsListener);
    }

    public List<List<String>> buildBatches(List<String> items) {
        return ListUtils.partition(items, getBatchSize());
    }

    private Map<Long, String> buildUploadBody(List<String> batchToAnalyze) {
        SourceId sourceId = getSourceId();

        final int itemCount = batchToAnalyze.size();
        int currentItem = 0;
        Map<Long, String> batchContents = new HashMap<>(itemCount);

        for (String itemToAnalyze : batchToAnalyze) {
            currentItem++;

            Path localPath = Path.of(itemToAnalyze);
            if (!Files.exists(localPath)) {
                LOG.warn("{} does not exist, skipping file.", itemToAnalyze);
                continue;
            }

            long nextId = sourceId.getNextId();
            String fileName = localPath.getFileName().toString();
            String serverPath = String.format("%s/%s", getServerSidePath(), fileName);

            batchContents.put(nextId, serverPath);
            updateBatchUpdated(currentItem, itemCount, String.format("[%d] %s added to batch", nextId, fileName));
            LOG.debug("[{}] {} added to batch at {}", nextId, fileName, serverPath);
        }

        return batchContents;
    }

    private BlockingQueue<Long> uploadBatch(Map<Long, String> uploadContents) {
        LOG.trace("Uploading to batch: {}", uploadContents);

        Map<Long, Long> sourceToResultIdMap = getApi().batchUpload(uploadContents);
        LOG.debug("Upload complete: {}", sourceToResultIdMap);

        return new LinkedBlockingQueue<>(sourceToResultIdMap.values());
    }

    private void waitForBatch(Queue<Long> itemsToWaitOn, Queue<PollResults> completedItems) {
        LOG.trace("Waiting on {}", itemsToWaitOn);

        final int itemCount = itemsToWaitOn.size();

        updateBatchUpdated(0, itemCount, "Waiting for items to complete.");

        getApi().waitForBatch(itemsToWaitOn, completedItems, (completedIndex) -> {
            updateBatchUpdated(completedIndex, itemCount, null);
        });
    }

    private void collectResults(final int itemCount, BlockingQueue<PollResults> itemsToCollect, BlockingQueue<AnalysisResult> collectedResults) {
        updateBatchUpdated(0, itemCount, "Collecting results");

        int currentItemIndex = 0;

        // Will break out when we hit the end of the queue
        while (true) {
            try {
                PollResults pollResults = itemsToCollect.take();

                // Exit the loop when we reach the end of the queue.
                if(END_OF_QUEUE == pollResults) {
                    LOG.trace("End of the queue for collecting items");
                    break;
                }

                LOG.debug("Collecting Results for {}", pollResults);
                currentItemIndex++;

                Long itemId = pollResults.getId();
                String itemPath = pollResults.getFilepath();
                String fileName = Path.of(itemPath).getFileName().toString();

                if(pollResults.isError()) {
                    LOG.error("[{}] {} produced an error during processing: {}",
                            itemId, fileName, pollResults);
                    continue;
                }

                AnalysisResult analysisResult = getApi().getResults(itemId);
                LOG.debug("[{}] {} Produced: {}", itemId, fileName, analysisResult);
                updateResultAnalyzed();

                if(null == analysisResult) {
                    LOG.error("[{}] {} Error getting results.", itemId, fileName);
                    updateResultError();
                    continue;
                }

                try {
                    collectedResults.put(analysisResult);
                } catch (InterruptedException e) {
                    LOG.error("[{}] {} Putting the results into the queue was interrupted.", itemId, fileName);
                }

                updateBatchUpdated(currentItemIndex, itemCount, String.format(
                        "[%d] %s Processed.",
                        itemId, fileName
                ));

                if (0 == analysisResult.getDetectionCount()) {
                    updateResultNoMatch();
                } else {
                    updateResultDetections(analysisResult.getDetectionCount());
                }

            } catch (InterruptedException e) {
                LOG.warn("Taking an item from the queue of items to collect was interrupted.  For now, just re-trying.");
            }
        }
    }

    @Override
    public void analyze(List<String> toAnalyze, BlockingQueue<AnalysisResult> completedResults) {
        List<String> batchToAnalyze = List.copyOf(toAnalyze);

        final int itemCount = batchToAnalyze.size();
        updateBatchUpdated(0, itemCount, "Uploading items in batch.");

        Map<Long, String> batchContents = buildUploadBody(batchToAnalyze);
        LOG.debug("Batch Processing: {}", batchContents);

        BlockingQueue<Long> resultIds = uploadBatch(batchContents);
        LOG.debug("Results List: {}", resultIds);

        BlockingQueue<PollResults> completedItems = new LinkedBlockingQueue<>();
        waitForBatch(resultIds, completedItems);
        LOG.debug("Finished waiting, ready to process: {}", completedItems);
        completedItems.add(END_OF_QUEUE);

        collectResults(itemCount, completedItems, completedResults);
    }

    private static final PollResults END_OF_QUEUE = new PollResults() {};
}
