package com.nuix.proserv.ws;

import com.nuix.proserv.t3k.conn.AnalysisListener;
import com.nuix.proserv.t3k.conn.Application;
import com.nuix.proserv.t3k.conn.BatchListener;
import com.nuix.proserv.t3k.results.AnalysisResult;
import lombok.Getter;
import nuix.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;

import com.nuix.proserv.t3k.conn.config.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ScriptingBase {
    private static final Logger LOG = LogManager.getLogger(ScriptingBase.class.getCanonicalName());

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

            String filePath = currentResult.getFile();
            String fileName = Path.of(filePath).getFileName().toString();
            String guid = fileName.split("\\.")[0];

            String guidSearch = String.format("guid:%s", guid);
            try {
                List<Item> foundItems = current_case.search(guidSearch);
                // Item list should be a singleton, but iterate anyway just to be safe...
                for(Item foundItem : foundItems) {
                    CustomMetadataMap metadataMap = foundItem.getCustomMetadata();

                }
            } catch (IOException e) {
                LOG.error("{} while searching the case for item with guid {}.  Skipping results for this item",
                        e.getMessage(), guid);
            }
        }
    }

    public void analyze(List<String> filesToAnalyze, StatusListener statusListener, ProgressListener progressListener) {
        BlockingQueue<AnalysisResult> completedResults = new LinkedBlockingQueue<>();

        AnalysisListener analysisListener = new AnalysisListener() {
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
        };

        app.setAnalysisListener(analysisListener);
        app.setBatchListener(batchListener);

        app.analyze(filesToAnalyze, completedResults);

    }
}
