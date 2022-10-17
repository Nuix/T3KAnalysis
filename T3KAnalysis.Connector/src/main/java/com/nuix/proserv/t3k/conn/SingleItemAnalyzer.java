package com.nuix.proserv.t3k.conn;

import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.PollResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public class SingleItemAnalyzer extends Analyzer<String> {
    public SingleItemAnalyzer(T3KApi api, String configPath, Configuration configuration, AnalysisListener analysisListener, ResultsListener resultsListener) {
        super(api, configPath, configuration, analysisListener, null, resultsListener);
    }

    private long uploadFile(long sourceId, String fileName) {
        updateAnalysisUpdated(0, 3, String.format(
                "[%d] %s uploading to T3K",
                sourceId, fileName
        ));

        String pathOnServer = String.format("%s/%s", getServerSidePath(), fileName);
        LOG.info("[{}] {} uploading as {}", sourceId, fileName, pathOnServer);

        long resultId = getApi().upload(sourceId, pathOnServer);

        if (-1L != resultId) {
            // item was uploaded successfully
            LOG.debug("[{}] {} uploaded correctly with id {}", sourceId, fileName, resultId);
        } else {
            LOG.error("[{}] {} uploading failed.", sourceId, fileName);
            updateAnalysisError(String.format(
                    "[%d] %s uploading failed.",
                    sourceId, fileName
            ));
        }

        return resultId;
    }

    private PollResults waitForAnalysis(long sourceId, long resultId, String fileName) {
        updateAnalysisUpdated(1, 3, String.format(
                "[%d/%d] %s Waiting for analysis to complete.",
                sourceId, resultId, fileName
        ));

        PollResults pollResults = getApi().waitForAnalysis(resultId);
        LOG.debug("[{}/{}] {} results of waiting: {}", sourceId, resultId, fileName, pollResults.toString());

        if (!pollResults.isFinished() || pollResults.isError()) {
            LOG.error("[{}/{}] {} error when processing the file: {}",
                    sourceId, resultId, fileName, pollResults);
            updateAnalysisError(String.format(
                    "[%d/%d] %s Error when processing file: %s.",
                    sourceId, resultId, fileName, pollResults.toString()
            ));
        }

        return pollResults;
    }

    private AnalysisResult getAnalysisResults(long sourceId, long resultId, String fileName) {
        updateAnalysisUpdated(2, 3, String.format(
                "[%d/%d] %s Retrieving results.",
                sourceId, resultId, fileName
        ));

        AnalysisResult analysisResult = getApi().getResults(resultId);
        LOG.debug("[{}/{}] {} analysis results: {}",
                sourceId, resultId, fileName, analysisResult);
        updateResultAnalyzed();

        if (null == analysisResult) {
            LOG.error("[{}/{}] {} Analysis failed for the file.", sourceId, resultId, fileName);
            updateAnalysisError(String.format(
                    "[%d/%d] %S.  There was an error processing the file.",
                    sourceId, resultId, fileName
            ));
            updateResultError();
        } else {
            updateAnalysisUpdated(3, 3, String.format(
                    "[%d/%d] %s analysis completed.",
                    sourceId, resultId, fileName
            ));
            if (0 == analysisResult.getDetectionCount()) {
                updateResultNoMatch();
            } else {
                updateResultDetections(analysisResult.getDetectionCount());
            }
        }

        return analysisResult;
    }

    @Override
    public void analyze(String localFilePath, BlockingQueue<AnalysisResult> completedResults) {
        Path localPath = Path.of(localFilePath);
        if (!Files.exists(localPath)) {
            LOG.warn("The file to process ({}) does not exist, doing nothing.", localFilePath);
            return;
        }

        if (Files.isDirectory(localPath)) {
            LOG.warn("The passed in path is a directory ({}).  To do batch processing use a list of files rather than a directory of files.", localFilePath);
            return;
        }

        String fileName = localPath.getFileName().toString();
        SourceId sourceId = getSourceId();
        long nextId = sourceId.getNextId();

        LOG.info("Analyzing [{}] {}", nextId, fileName);
        updateAnalysisStarted(String.format(
                "[%d] %s Beginning analysis",
                nextId, fileName
        ));

        long resultId = uploadFile(nextId, fileName);

        if (-1L == resultId) {
            return;
        }

        PollResults pollResults = waitForAnalysis(nextId, resultId, fileName);

        if (!pollResults.isFinished() || pollResults.isError()) {
            return;
        }

        AnalysisResult analysisResult = getAnalysisResults(nextId, resultId, fileName);

        updateAnalysisCompleted("Analysis completed.");

        try {
            completedResults.put(analysisResult);
        } catch (InterruptedException e) {
            LOG.error("Sending the analysis results to the completed queue was interrupted.");
        }
    }

}
