java_import "com.nuix.proserv.t3k.ws.ScriptingBase"

case_folder = current_case.location.absolute_path

log_file = File.join case_folder, 't3k.log'
LOG = ScriptingBase.init_logging(log_file, "DEBUG")


java_import "com.nuix.proserv.t3k.conn.AnalysisListener"
java_import "com.nuix.proserv.t3k.conn.BatchListener"
java_import "com.nuix.proserv.t3k.conn.ResultsListener"
java_import "java.util.concurrent.LinkedBlockingQueue"

require_relative 'ui/processing_screen'

data_folder = File.join ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis'
settings_file = File.join(data_folder, 't3k_settings.json')

class ProgressReport
  include AnalysisListener # For single item processing
  include BatchListener    # For batch processing
  include ResultsListener  # For reporting result counts

  def initialize(pd, report)
    super()

    @pd = pd
    @report = report
  end

  def analysisStarted(message)
    @pd.sub_progress_visible = false
    @pd.sub_status = nil
    @pd.set_main_progress 0, 1
    unless message.nil?
      @pd.log_message message
    end
  end

  def analysisUpdated(step, count, message)
    @pd.set_main_progress step, count
    unless message.nil?
      @pd.log_message message
    end
  end

  def analysisCompleted(message)
    @pd.set_main_progress 1, 1
    @pd.log_message message
  end

  def analysisError(message)
    @pd.log_message "ERROR: " + message
  end

  def batchStarted(index, count, message)
    @pd.set_main_progress index-1, count
    @pd.main_status_and_log_it = message
    @pd.set_sub_progress 0, 1
    @pd.sub_progress_visible = true
    @pd.sub_status = ""
  end

  def batchUpdated(index, count, message)
    @pd.set_sub_progress index, count

    if (0 == index and not message.nil?)
      unless message.nil?
        @pd.sub_status = message
      end
    else
      unless message.nil?
        @pd.log_message message
      end
    end
  end

  def batchCompleted(index, count, message)
    @pd.set_sub_progress 1, 1
    @pd.set_main_progress index, count

    unless message.nil?
      @pd.main_status = message
    end
  end

  def incrementAnalyzed()
    current_analyzed = @report.get_data_field_value("Item Counts", "Analyzed").to_i
    current_analyzed += 1
    @report.update_data "Item Counts", "Analyzed", current_analyzed
  end

  def incrementErrors()
    current_errors = @report.get_data_field_value("Item Counts", "Errors").to_i
    current_errors += 1
    @report.update_data "Item Counts", "Errors", current_errors
  end

  def incrementNotMatched()
    current_nomatch = @report.get_data_field_value("Item Counts", "Not Matched").to_i
    current_nomatch += 1
    @report.update_data "Item Counts", "Not Matched", current_nomatch
  end

  def addDetections(addedCount)
    current_detections = @report.get_data_field_value("Item Counts", "Detected").to_i
    current_detections += addedCount
    @report.update_data "Item Counts", "Detected", current_detections
  end
end


def cleanup_ui(pd)
  pd.set_main_progress 1, 1
  pd.set_sub_progress 1, 1
  pd.sub_progress_visible = false
  pd.sub_status = nil
  pd.main_progress_visible = false
  pd.main_status = nil
end

if File.exist? settings_file
  scripting_base = ScriptingBase.new utilities, current_case, settings_file
  result = LinkedBlockingQueue.new

  source_guids = current_selected_items.map { |item| item.guid }.compact

  show_processing_dialog do | pd, report_data |

    pd.main_status_and_log_it = "Exporting items to #{scripting_base.config.nuix_output_path}"
    pd.set_main_progress 0, 4
    pd.sub_progress_visible = true

    exported_files = scripting_base.export_items(current_selected_items) do | index, count, message |
      pd.set_sub_progress index, count
      unless message.nil?
        pd.log_message message
      end
    end

    pd.main_status_and_log_it = "Processing with T3K"
    report = ProgressReport.new pd, report_data
    scripting_base.analyze exported_files, result, report, report, report
    scripting_base.process_results result

    cleanup_ui pd

    pd.log_message ""
    pd.log_message "Logs for this process can be found at: #{log_file}."
    pd.log_message ""
    pd.log_message "The analysis is complete, you may close this dialog."

  end

  guid_search = "guid:(#{source_guids.join " OR "})"
  window.close_all_tabs
  window.open_tab "workbench", {"search" => guid_search, "metadataProfile" => "T3K"}

end
