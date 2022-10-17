package com.nuix.proserv.t3k.ws;

import com.nuix.proserv.t3k.conn.AnalysisListener;
import com.nuix.proserv.t3k.conn.Application;
import com.nuix.proserv.t3k.conn.BatchListener;
import com.nuix.proserv.t3k.conn.ResultsListener;
import com.nuix.proserv.t3k.detections.CCRDetection;
import com.nuix.proserv.t3k.detections.ObjectDetection;
import com.nuix.proserv.t3k.detections.PersonDetection;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata;
import com.nuix.proserv.t3k.ws.metadataprofile.*;
import com.nuix.proserv.t3k.ws.metadataprofile.MetadataProfile;
import lombok.Getter;
import nuix.*;


import com.nuix.proserv.t3k.conn.config.Configuration;
import org.apache.commons.lang3.text.WordUtils;
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
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Provides an entrypoint for Post Processing Scripts and Worker Side Scripts to interact with the T3K Connector
 * workflows.
 * <p>
 *     The purpose of this class is to share as much process and code between the two scripting sides as possible, and
 *     to minimize the size of those scripts.   To achieve this it handles exporting, tracking the analysis, and writing
 *     custom metadata, leaving the scripts to handle UI and sequencing.
 * </p>
 * <p>
 *     This class will use the {@link ProgressListener} to communicate simple incremental progress to the caller, so
 *     the caller can handle UI changes as needed, and this code can remain UI agnostic.  It will also take
 *     {@link com.nuix.proserv.t3k.conn.BatchListener}, {@link com.nuix.proserv.t3k.conn.ResultsListener} and
 *     {@link com.nuix.proserv.t3k.conn.AnalysisListener} to handle more detailed processing results.
 * </p>
 * <p>
 *     An example Ruby post-processing script that would use this class might be:
 * </p>
 * <pre>
 * # Start logging to the case directory as soon as possible
 * java_import "com.nuix.proserv.t3k.ws.ScriptingBase"
 * case_folder = current_case.location.absolute_path
 * log_file = File.join case_folder, 't3k.log'
 * LOG = ScriptingBase.init_logging(log_file, "DEBUG")
 *
 * # Read the config and initialize the ScriptingBase instance
 * data_folder = File.join ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis'
 * settings_file = File.join(data_folder, 't3k_settings.json')
 * scripting_base = ScriptingBase.new settings_file
 *
 * # Show the UI and do the work
 * show_processing_dialog do | pd, report_data |
 *     pd.main_status_and_log_it = "Exporting items to #{scripting_base.config.nuix_output_path}"
 *     pd.set_main_progress 0, 4
 *     pd.sub_progress_visible = true
 *
 *     source_guids = current_selected_items.map { |item| item.guid }.compact
 *     exported_files = scripting_base.export_items(current_selected_items, utilities) do | index, count, message |
 *         # This is the ProgressListener callback
 *         pd.set_sub_progress index, count
 *         unless message.nil?
 *             pd.log_message message
 *         end
 *     end
 *
 *     # The ProgressResport is a class that implements the BatchListener, AnalysisListener, and ResultsListener
 *     report = ProgressReport.new pd, report_data
 *
 *     result = LinkedBlockingQueue.new
 *     scripting_base.analyze exported_files, result, report, report, report
 *
 *     scripting_base.process_results result, current_case
 * end
 * </pre>
 */
public class ScriptingBase {
    /**
     * The LOGGING_NAME will be used to initialize logging with.   Use it from other classes when getting a logger
     * to log to the same shared Logger.
     */
    public static final String LOGGING_NAME = "com.nuix.proserv.t3k";
    private static Logger LOG;

    /**
     * Initialize the logger if it hasn't already been started, or provide the existing logger.  If a logger has
     * already been initialized with this ClassLoader's ScriptingBase.class then it will be re-used, in which
     * case the file and log level may not be accurate.
     *
     * @param logFile The full and absolute path to the file to log to.
     * @param logLevel Optional The log level to record.  Should be one of 'OFF' 'FATAL' 'ERROR' 'WARN' 'INFO' 'DEBUG'
     *                 'TRACE' 'ALL' and defaults to 'ERROR' if not supplied or one of those options is not provided.
     * @return Either an existing, already initialized Logger or a new one.
     */
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

    /**
     * The application configuration.
     *
     * See: {@link com.nuix.proserv.t3k.conn.config.Configuration}
     */
    @Getter private final Configuration config;

    /**
     * A reference to the T3K Connection application that handles the workflow for communicating with T3K CORE.
     *
     * See: {@link com.nuix.proserv.t3k.conn.Application}
     */
    @Getter private final Application app;

    private final String pathToConfig;

    /**
     * Creates a new instance of the ScriptingBase class with the path to the application configuration file.
     * @param pathToConfig The full path the JSON file with the application's configuration
     * @throws IllegalArgumentException if pathToConfig is null
     */
    public ScriptingBase(String pathToConfig) {
        if (null == pathToConfig) throw new IllegalArgumentException("The path to the configuration file must not be null.");

        this.pathToConfig = pathToConfig;

        this.app = new Application(pathToConfig);
        this.config = app.getConfig();

    }

    /**
     * Export items to the location defined in the application configuration.
     * <p>
     *     The items will have their binaries exported exactly to the path defined, no sub folder will be created.
     *     If the binaries don't exist, no error will occur but the items won't be exported.  Items are exported with
     *     their GUID for a name, and their corrected extension.
     * </p>
     * <p>
     *     Progress can be tracked with the provided ProgressListener which will be updated on each file exported
     *     with the name of exported item and the path it was exported to.
     * </p>
     * <p>
     *     This method uses nuix.Utilities and a nuix.SingleItemExporter to do the export, and so is not suitable
     *     for use in Worker Side Scripts.
     * </p>
     * <p>
     *     An example of a Ruby script using this method:
     * </p>
     * <pre>
     * exported_files = scripting_base.export_items(current_selected_items, utilities) do | index, count, message |
     *     # This is the ProgressListener callback
     *     pd.set_sub_progress index, count
     *     unless message.nil?
     *         pd.log_message message
     *     end
     * end
     * </pre>
     * @param itemsToExport java.util.List of nuix.Items to export.  Must not be null, an empty list will result in a no-op.
     * @param utilities The nuix.Utilities instance to get a BinaryExporter from.
     * @param listener The ProgressListener that will be informed of each file being exported.
     * @return A java.util.List containing the full paths of the files that were exported to.
     * @throws IllegalStateException if the utilities object is null.
     */
    public List<String> exportItems(List<Item> itemsToExport, Utilities utilities, ProgressListener listener) {
        LOG.trace("Exporting items: itemsToExport");
        if(null == utilities) {
            throw new IllegalStateException("The utilities object must not be null when exporting items.");
        }

        SingleItemExporter exporter = utilities.getBinaryExporter();

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

    /**
     * Get a {@link Metadata} instance with the correct script calls for a scripted metadata entry into a metadata
     * profile.
     * @param detection A string with the type of detection to make a script for.  If it is "person" the script will
     *                  target display of a person's gender and age.  Otherwise, it will use a generic display for
     *                  matches to the detection.
     * @return An instance of Metadata with everything needed to display it as a column in a profile.
     */
    private Metadata getScriptedMetadata(String detection) {
        String script = null;

        String name = WordUtils.capitalize(StringUtils.replaceChars(detection, '_', ' '));

        if ("person".equalsIgnoreCase(detection)) {
            script = ScriptedExpression.PERSON_SCRIPT_TEMPLATE;
        } else {
            script = String.format(ScriptedExpression.TYPE_SCRIPT_TEMPLATE, detection);
        }

        ScriptedExpression expression = new ScriptedExpression(ScriptType.ruby, script);
        return new Metadata(MetadataType.SPECIAL, name, expression);
    }

    /**
     * Read the metadata profile if it exists, make it if it does not, and add the three always-present columns to it.
     * @param profileSource The MetadataProfileReaderWriter to parse the metadata profile from disk
     * @param currentCase The case where the profile will be / is stored.
     * @return A MetadataProfile object with at least the three standard columns, and any additional columns already present in the case.
     */
    private MetadataProfile initializeMetadataProfile(MetadataProfileReaderWriter profileSource, Case currentCase) {
        MetadataProfile metadataProfile = profileSource.readProfile(currentCase, "T3K Analysis");

        Set<Metadata> columns = metadataProfile.getColumns();
        Metadata itemName = new Metadata(MetadataType.SPECIAL, "Name", null);
        Metadata detectionsFound = new Metadata(MetadataType.CUSTOM, "T3K Detections", null);
        Metadata detectionsCount = new Metadata(MetadataType.CUSTOM, "T3K Detections|Count", null);
        columns.add(itemName);
        columns.add(detectionsFound);
        columns.add(detectionsCount);


        return metadataProfile;
    }

    /**
     * Read the contents of the results queue and convert the raw results into Custom Metadata to apply to items in
     * the case.
     * <p>
     *     The Each {@link com.nuix.proserv.t3k.results.AnalysisResult} in the queue will contain the path to the file
     *     which it applies to - the name of the file is the GUID for the item to apply the custom metadata to.  This
     *     method will search for the item, translate each {@link com.nuix.proserv.t3k.detections.Detection} found in
     *     the result into an implementation of {@link com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata} and apply it
     *     to an item based on its GUID.  It will also log the type of the detection to be applied to the metadata
     *     profile used for display afterwards.
     * </p>
     * <p>
     *     This method will continue to run until it reaches the
     *     {@link com.nuix.proserv.t3k.conn.Application#END_OF_ANALYSIS} object.  It is possible to run this method in
     *     separate thread while analysis continues.
     * </p>
     * @param results The queue that holds, or will hold the analysis results to apply as custom metadata.  The queue
     *                must be terminated with {@link com.nuix.proserv.t3k.conn.Application#END_OF_ANALYSIS}.
     * @param currentCase The case to use for finding the items to annotate with custom metadata.
     */
    public void processResults(BlockingQueue<AnalysisResult> results, Case currentCase) {
        MetadataProfileReaderWriter profileSource = new MetadataProfileReaderWriter();
        MetadataProfile metadataProfile = initializeMetadataProfile(profileSource, currentCase);

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
                List<Item> foundItems = currentCase.search(guidSearch);
                // Item list should be a singleton, but iterate anyway just to be safe...
                for(Item foundItem : foundItems) {
                    CustomMetadataMap metadataMap = foundItem.getCustomMetadata();
                    AnalysisMetadata metadataApplier = AnalysisMetadata.getInstance(currentResult, metadataMap);
                    metadataApplier.applyResults();

                    currentResult.forEachDetection(detection -> {
                        if(PersonDetection.TYPE.equals(detection.getType())) {
                            metadataProfile.getColumns().add(getScriptedMetadata("person"));
                        } else if (ObjectDetection.TYPE.equals(detection.getType())) {
                            metadataProfile.getColumns().add(getScriptedMetadata(((ObjectDetection)detection).getClass_name()));
                        } else if (CCRDetection.TYPE.equals(detection.getType())) {
                            metadataProfile.getColumns().add(getScriptedMetadata(((CCRDetection)detection).getInfo()));
                        }
                    });
                }
            } catch (IOException e) {
                LOG.error("{} while searching the case for item with guid {}.  Skipping results for this item",
                        e.getMessage(), guid);
            }
        }

        profileSource.writeProfile(metadataProfile, currentCase, "T3K Analysis");
    }

    /**
     * Use the {@link com.nuix.proserv.t3k.conn.Application} to run the list of files through the analysis workflow.
     * @param filesToAnalyze The non-null list of files to analyze.  These files should be full paths to the files where
     *                       they were exported by Nuix.  The file paths will be converted to server-visible paths by
     *                       the Application.
     * @param completedResults The queue that will be used to fill with the results of analysis.  This queue must not
     *                         be null, and may or may not be empty.  The analysis routine will add additional results
     *                         into this queue as they become available, and will add the
     *                         {@link com.nuix.proserv.t3k.conn.Application#END_OF_ANALYSIS} when analysis completes.
     * @param analysisListener Callback for the progress of a single item through the analysis routine.
     * @param batchListener Callbacks for the progress of one or more batches of items through the analysis routine.
     * @param resultsListener Callback to inform each type of result of analysis as they complete.
     */
    public void analyze(List<String> filesToAnalyze, BlockingQueue<AnalysisResult> completedResults,
                        AnalysisListener analysisListener,
                        BatchListener batchListener,
                        ResultsListener resultsListener) {

        app.setAnalysisListener(analysisListener);
        app.setBatchListener(batchListener);
        app.setResultsListener(resultsListener);

        app.analyze(filesToAnalyze, completedResults);

    }
}
