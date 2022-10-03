require_relative 'utilities/multi_logger'
require_relative 'meta_data'

LOGGER = MultiLogger.instance
# LOGGER.add_output STDOUT
LOGGER.progname "Nuix T3K Connector"
LOGGER.log_level :debug
LOGGER.add_output File.new File.join(current_case.get_location.get_absolute_path, Time.now.strftime("T3K_Analysis_%Y%m%d_%H.%M.%S.log")), "w"

$utilities = utilities
$items_to_export = current_selectect_items

begin
  require_relative 'ui/processing_screen'

  java_import "com.nuix.proserv.t3k.conn.Application"
  java_import "com.google.gson.Gson"
  java_import "java.util.Map"

  settings = File.join(ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis', 't3k_settings.json')
  $app_config = Gson.new.fromJson File.read(settings), Map.java_class

  def export_selected_items(listener)

    exporter = $utilities.binary_exporter
    exported_paths = []

    item_count $items_to_export.size
    current_item_index = 0

    listener.update 0, item_count, "Starting export of selected items."
    $items_to_export.each do | item |
      current_item_index += 1

      output_name = "#{item.guid}.#{item.corrected_extension}"

      output_path = File.join $app_config["nuix_output_path"], output_name

      LOGGER.info "Exporting #{item.name}: #{item.guid}"
      exporter.export_item item, output_path
      listener.update current_item_index, item_count, "Exported #{item.name} to #{output_name}"

      exported_paths << output_path
    end

  end

  def assign_custom_metadata(source_items, analysis_results, report)
    analyzed_path = analysis_results.path

    guid = File.basename analyzed_path, ".*"

    unless source_items.include? guid
      LOGGER.error "An item was returned with a name that does not match a GUID of a processed" +
                     "item.  The results can not be added.  #{analyzed_path}"
      return
    end

    source_item = source_items[guid]
    metadata = source_item.custom_metadata

    if 0 < analysis_results.detection_count

      if 1 === analysis_results.detection_count and analysis_results.detection(0).nil?
        # There is only one item, and it is nil, means no detections
        not_detected_count = report.get_data_field_value("Item Counts", "Not Matched").to_i
        report.update_data("Item Counts", "Not Matched", not_detected_count + 1)

        metadata.put_text MetaData.t3k, "No Matches Detected"

      else
        # otherwise, detections - watch for mid-stream nils...
        metadata.put_text MetaData.t3k, "Match Detected"
        metadata.put_integer MetaData.detection_count, analysis_results.detection_count

        detected_count = report.get_data_field_value("Item Counts", "Detected").to_i
        report.update_data("Item Counts", "Detected", detected_count + analysis_results.detection_count)

        detection_id = 0
        analysis_results.each_detection do | detection |
          unless detection.nil?
            detection_id += 1

            case detection.type
            when "person"
              assign_person_metadata detection_id, detection, metadata
            when "md5"
              assign_md5_metadata detection_id, detection, metadata
            when "text"
              assign_text_metadata detection_id, detection, metadata
            else
              assign_object_metadata detection_id, detection, metadata
            end
          end
        end
      end

    else
      not_detected_count = report.get_data_field_value("Item Counts", "Not Matched").to_i
      report.update_data("Item Counts", "Not Matched", not_detected_count + 1)

      LOGGER.debug "Result MetaData: #{metadata}"
    end

  end

  def assign_nomatch_metadata(metadata_mao)
    metadata.put_text MetaData.t3k, "Match Detected"
  end

  def assign_person_metadata(index, detection, metadata_map)
    age_label, gender_label, score_label = MetaData.person index

    metadata_map.put_text gender_label, detection.gender
    metadata_map.put_integer age_label, detection.age
    metadata_map.put_float score_label, detection.score

    assign_container_data index, detection, metadata_map
  end

  def assign_object_metadata(index, detection, metadata_map)
    label = MetaData.object index, detection.classification

    metadata_map.put_float label, detection.score

    assign_container_data index, detection, metadata_map
  end

  def assign_md5_metadata(index, detection, metadata_map)
    type_label, hash_label, description_label, id_label = MetaData.md5 index

    metadata_map.put_text type_label, detection.hit_type
    metadata_map.put_text hash_label, detection.hit_hash
    metadata_map.put_text description_label, detection.description
    metadata_map.put_text id_label, detection.hit_id

    assign_container_data index, detection, metadata_map
  end

  def assign_text_metadata(index, detection, metadata_map)
    string_label, description_label, language_label, regex_label, fuzzy_label, mlr_label, matches_label = MetaData.text index

    metadata_map.put_text string_label, detection.string
    metadata_map.put_text language_label, detection.language
    metadata_map.put_text description_label, detection.description
    metadata_map.put_boolean regex_label, detection.regex
    metadata_map.put_boolean fuzzy_label, detection.fuzzy
    metadata_map.put_text mlr_label, detection.mlr
    metadata_map.put_text matches_label, detection.matches

    assign_container_data index, detection, metadata_map
  end

  def assign_container_data(index, detection, metadata_map)
    if detection.respond_to? :data
      LOGGER.debug "Data: #{detection.data} [#{detection}]"
      unless detection.data.nil?
        case detection.data.type
        when "video"
          assign_video_data index, detection, metadata_map
        when "document"
          assign_document_data index, detection, metadata_map
        else
          # do something
        end
      end
    end
  end

  def assign_video_data(index, detection, metadata_map)
    if "object" === detection.type
      label = MetaData.video_data index, detection.classification
    else
      label = MetaData.video_data index, detection.type
    end

    metadata_map.put_integer label, detection.data.frame
  end

  def assign_document_data(index, detection, metadata_map)
    if "object" === detection.type
      page_label, image_label = MetaData.document_data index, detection.classification
    else
      page_label, image_label = MetaData.document_data index, detection.type
    end

    metadata_map.put_integer page_label, detection.data.page_number
    metadata_map.put_integer image_label, detection.data.image_number
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

    config = read_config
    app = Application.new config

    sub_progress_was_visible = false
    app.batch_analysis_listener = Class.new do
      def initialize(pd)
        @pd = pd
      end

      def batch_started(index, number_of_batches, message=nil)
        @pd.main_status_and_log_it = message unless message.nil?
        @pd.set_main_progress index-1, number_of_batches

        @pd.sub_progress_visible = true
        sub_progress_was_visible = true
      end

      def batch_updated(index, batch_size, message=nil)
        @pd.sub_status_and_log_it = message unless message.nil?
        @pd.set_sub_progress index, batch_size
      end

      def batch_completed(index, number_of_batches, message=nil)
        @pd.main_status_and_log_it = message unless message.nil?
        @pd.set_main_progress index, number_of_batches
      end
    end.new pd

    app.item_analysis_listener = Class.new do
      def initialize(pd)
        @pd = pd
      end

      def analysis_started(message)
        @pd.main_status_and_log_it = message
      end

      def analysis_updated(step, total_steps, message=nil)
        @pd.main_status_and_log_it = message unless message.nil?
        @pd.set_main_progress step, total_steps
      end

      def analysis_completed(message=nil)
        @pd.main_status_and_log_it = message unless message.nil?
      end

      def analysis_error(message)
        LOGGER.error message
      end
    end

    app.run_analysis do | batch_results |

      batch_results_size = batch_results.size

      analyzed_count = report_data.get_data_field_value("Item Counts", "Analyzed").to_i
      report_data.update_data("Item Counts", "Analyzed", analyzed_count + batch_results.size)

      current_item = 0
      pd.sub_progress_visible = true
      pd.set_sub_progress(0, batch_results_size)

      batch_results.each do | id, result |
        current_item += 1

        pd.sub_status_and_log_it = "Assigning results for #{result.path}"
        assign_custom_metadata source_items_map, result, report_data

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
