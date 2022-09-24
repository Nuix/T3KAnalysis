require_relative 'utilities/multi_logger'

LOGGER = MultiLogger.instance
LOGGER.add_output STDOUT
LOGGER.progname "Nuix T3K Connector"
LOGGER.log_level :info
LOGGER.add_output File.new File.join(current_case.get_location.get_absolute_path, Time.now.strftime("T3K_Analysis_%Y%m%d_%H.%M.%S.log")), "w"

begin
  require_relative 'ui/processing_screen'
  require_relative 'processing/application'


  def export_selected_items(items, progress)
    config = read_config
    export_path = config["nuix_output_path"]
    item_count = items.size
    index = 0
    items.each do |item|
      index += 1
      progress.set_main_progress index, item_count
      LOGGER.info "Exporting #{item.name}: #{item.guid}"

      export_file = File.join export_path, "#{item.guid}.#{item.original_extension}"
      exporter = $utilities.binary_exporter
      exporter.export_item item, export_file, {}
    end
  end

  def assign_custom_metadata(source_items, analysis_results)
    analyzed_path = analysis_results.path

    guid = File.basename analyzed_path, ".*"

    unless source_items.include? guid
      LOGGER.error "An item was returned with a name that does not match a GUID of a processed" +
                     "item.  The results can not be added.  #{analyzed_path}"
      return
    end

    source_item = source_items[guid]
    metadata = source_item.custom_metadata

  end

  def assign_person_metadata result_object, metadata_map

  end

  source_items_map = {}
  current_selected_items.each do | item |
    source_items_map[item.guid] = item
  end

  show_processing_dialog do | pd, report_data |
    LOGGER.add_output pd
    LOGGER.debug $currentCase.get_location

    pd.main_status_and_log_it = "Exporting"
    export_selected_items source_items_map.values, pd

    pd.main_status_and_log_it = "Processing with T3K"
    run_analysis read_config, pd do | batch_results |

      batch_results_size = batch_results.size
      current_item = 0
      sub_progress_was_visible = pd.sub_progress_visible
      pd.sub_progress_visible = true
      pd.set_sub_progress = 0, batch_results_size

      batch_results.each do | id, result |
        current_item += 1

        pd.sub_status_and_log_it = "Assigning results for #{result.path}"
        assign_custom_metadata source_items_map, result

        pd.set_sub_progress current_item, batch_results_size
      end

      pd.sub_progress_visible = sub_progress_was_visible
    end

    pd.main_status = nil
    pd.main_progress_visible = false

  end
ensure
  LOGGER.close
end
