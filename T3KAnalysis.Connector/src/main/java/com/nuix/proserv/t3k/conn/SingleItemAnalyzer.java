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

public class SingleItemAnalyzer extends Analyzer {
    private static final Logger LOG = LogManager.getLogger(SingleItemAnalyzer.class.getCanonicalName());


    public SingleItemAnalyzer(T3KApi api, String configPath, Configuration configuration, AnalysisListener listenerh) {
        super(api, configPath, configuration, listenerh);
    }

    private long uploadFile(long sourceId, String fileName) {
        updateAnalysisUpdated(0, 3, String.format(
                    "[%d] %s uploading to T3K",
                    sourceId, fileName
        ));

        String pathOnServer = String.format("%s/%s", getServerSidePath(), fileName);
        LOG.info("[{}] {} uploading as {}", sourceId, fileName, pathOnServer);

        long resultId = getApi().upload(sourceId, pathOnServer);

        if(-1L != resultId) {
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

        if(!pollResults.isFinished() || pollResults.isError()) {
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

        if (null == analysisResult) {
            LOG.error("[{}/{}] {} Anaysis failed for the file.", sourceId, resultId, fileName);
            updateAnalysisError(String.format(
                        "[%d/%d] %S.  There was an error processing the file.",
                        sourceId, resultId, fileName
            ));
        } else {
            updateAnalysisUpdated(3, 3, String.format(
                        "[%d/%d] %s analysis completed.",
                        sourceId, resultId, fileName
            ));
        }

        return analysisResult;
    }

    public AnalysisResult analyze(String localFilePath) throws FileNotFoundException {
        Path localPath = Path.of(localFilePath);
        if (!Files.exists(localPath)) {
            throw new FileNotFoundException("The provided file can not be found. " + localFilePath);
        }

        if (Files.isDirectory(localPath)) {
            throw new IllegalArgumentException("The provided file to analyze is a directory, only a single file can be" +
                    " processed using this method.  To analyze multiple files use the \"batchAnalyze\" method.");
        }

        String fileName = localPath.getFileName().toString();
        SourceId sourceId = getSourceId();
        long nextId = sourceId.getNextId();

        try {
            LOG.info("Analyzing [{}] {}", nextId, fileName);
            updateAnalysisStarted(String.format(
                        "[%d] %s Beginning analysis",
                        nextId, fileName
            ));

            long resultId = uploadFile(nextId, fileName);

            if(-1L == resultId) {
                return null;
            }

            PollResults pollResults = waitForAnalysis(nextId, resultId, fileName);

            if(!pollResults.isFinished() || pollResults.isError()) {
                return null;
            }

            AnalysisResult analysisResult = getAnalysisResults(nextId, resultId, fileName);

            updateAnalysisCompleted("Analysis completed.");

            return analysisResult;

        } finally {
            try {
                cacheSourceId(sourceId);
            } catch (IOException e) {
                LOG.error("Unable to store source id.  Next time the application runs the ids may be out of sync.");
                LOG.error(e);
            }
        }
    }

}
