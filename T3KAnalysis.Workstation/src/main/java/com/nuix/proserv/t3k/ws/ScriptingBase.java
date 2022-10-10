package com.nuix.proserv.t3k.ws;

import com.nuix.proserv.t3k.conn.AnalysisListener;
import com.nuix.proserv.t3k.conn.Application;
import com.nuix.proserv.t3k.conn.BatchListener;
import com.nuix.proserv.t3k.conn.ResultsListener;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata;
import lombok.Getter;
import nuix.*;


import com.nuix.proserv.t3k.conn.config.Configuration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;


public class ScriptingBase {
    public static final String LOGGING_NAME = "com.nuix.proserv.t3k";
    private static Logger LOG;

    public static synchronized Logger initLogging(String logFile, String logLevel) {
        if (null != LOG) {
            return LOG;
        }


        Level level = Level.toLevel(logLevel, Level.ERROR);

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Layout<String> layout = PatternLayout.newBuilder().withPattern(
                "%d{yyyy-MM-dd HH:mm:ss.SSS Z}: %c{1.} [%-5p] %m%ex{full}%n").build();
        org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();

        String fileAppenderName = "T3K_File";
        Appender appender = FileAppender.newBuilder().withFileName(logFile).withAppend(true).withCreateOnDemand(true)
                        .withLocking(false).withImmediateFlush(true).withBufferedIo(false).withBufferSize(1024)
                        .setName(fileAppenderName).setConfiguration(config).withAdvertise(false).withAdvertiseUri(null)
                        .setLayout(layout).build();
        appender.start();
        config.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef(fileAppenderName, level, null);
        AppenderRef[] refs = new AppenderRef[] { ref };

        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, level, LOGGING_NAME,
                "true", refs, null, config, null);
        loggerConfig.addAppender(appender, level, null);

        config.addLogger(LOGGING_NAME, loggerConfig);
        context.updateLoggers();

        LOG = LogManager.getLogger(LOGGING_NAME);
        return LOG;
    }

    @Getter
    private final Utilities utilities;

    @Getter
    private final Configuration config;

    @Getter
    private final Application app;

    private final String pathToConfig;

    private final Case current_case;


    public ScriptingBase(Utilities utilities, Case current_case, String pathToConfig) {
        this.utilities = utilities;
        this.current_case = current_case;
        this.pathToConfig = pathToConfig;

        this.app = new Application(pathToConfig);
        this.config = app.getConfig();

    }

    public List<String> exportItems(List<Item> itemsToExport, ProgressListener listener) {
        LOG.trace("Exporting items: itemsToExport");
        SingleItemExporter exporter = getUtilities().getBinaryExporter();

        List<Item> exportList = List.copyOf(itemsToExport);

        List<String> exportedItems = new ArrayList<>();
        final int itemCount = itemsToExport.size();
        int currentIndex = 0;
        listener.updateProgress(0, itemCount, "Beginning Export Process");

        for(Item item : exportList) {
            LOG.trace("Exporting {}", item.getName());
            currentIndex++;
            String name = item.getName();
            String guid = item.getGuid();
            String ext = item.getCorrectedExtension();

            String fileName = guid + "." + ext;
            String outputPath = Path.of(getConfig().getNuix_output_path(), fileName).toString();
            try {
                exporter.exportItem(item, outputPath);
                exportedItems.add(outputPath);
            } catch (IOException e) {
                LOG.error("Exception exporting {}: {}", name, e.getMessage());
                LOG.error(e);
            }

            listener.updateProgress(currentIndex, itemCount, String.format(
                    "Exported %s as %s", name, fileName
            ));
        }

        return exportedItems;
    }

    public void processResults(BlockingQueue<AnalysisResult> results) {

        while(true) {
            AnalysisResult currentResult = null;

            try {
                currentResult = results.take();
            } catch (InterruptedException e) {
                LOG.info("Getting the next item from the queue was interrupted.  Trying again.");
                continue;
            }

            if (Application.END_OF_ANALYSIS.equals(currentResult)) {
                LOG.trace("Came to the end of the queue, exiting work.");
                break;
            }

            String filePath = currentResult.getMetadata().getFile_path();
            String fileName = Path.of(filePath).getFileName().toString();
            String guid = fileName.split("\\.")[0];

            String guidSearch = String.format("guid:%s", guid);
            try {
                List<Item> foundItems = current_case.search(guidSearch);
                // Item list should be a singleton, but iterate anyway just to be safe...
                for(Item foundItem : foundItems) {
                    CustomMetadataMap metadataMap = foundItem.getCustomMetadata();
                    AnalysisMetadata metadataApplier = AnalysisMetadata.getInstance(currentResult, metadataMap);
                    metadataApplier.applyResults();
                }
            } catch (IOException e) {
                LOG.error("{} while searching the case for item with guid {}.  Skipping results for this item",
                        e.getMessage(), guid);
            }
        }
    }

    public void analyze(List<String> filesToAnalyze, BlockingQueue<AnalysisResult> completedResults,
                        AnalysisListener analysisListener,
                        BatchListener batchListener,
                        ResultsListener resultsListener) {

        /*AnalysisListener analysisListener = new AnalysisListener() {
            @Override
            public void analysisStarted(String message) {
                statusListener.updateStatus(message);
            }

            @Override
            public void analysisUpdated(int index, int count, String message) {
                progressListener.updateProgress(index, count, message);
            }

            @Override
            public void analysisCompleted(String message) {
                statusListener.updateStatus(message);
            }

            @Override
            public void analysisError(String message) {
                LOG.error(message);
            }
        };

        BatchListener batchListener = new BatchListener() {
            @Override
            public void batchStarted(int batch, int batchCount, String message) {
                progressListener.updateProgress(batch, batchCount, "START: " + message);
            }

            @Override
            public void batchUpdated(int index, int count, String message) {
                progressListener.updateProgress(index, count, "UPDATE: " + message);
            }

            @Override
            public void batchCompleted(int batch, int batchCount, String message) {
                progressListener.updateProgress(batch, batchCount, "DONE: " + message);
            }
        };*/

        app.setAnalysisListener(analysisListener);
        app.setBatchListener(batchListener);
        app.setResultsListener(resultsListener);

        app.analyze(filesToAnalyze, completedResults);

    }
}
