require_relative 'utilities/multi_logger'
require_relative 'meta_data'

LOGGER = MultiLogger.instance
LOGGER.add_output STDOUT
LOGGER.progname "Nuix T3K Connector"
LOGGER.log_level :debug
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

    unless 0 === analysis_results.detection_count

      if 1 === analysis_results.detection_count and analysis_results.detection(0).nil?
        # There is only one item, and it is nil, means no detections
        metadata.put_text MetaData.t3k, "No Matches Detected"
      else
        # otherwise, detections - watch for mid-stream nils...
        metadata.put_text MetaData.t3k, "Match Detected"
        metadata.put_integer MetaData.detection_count, analysis_results.detection_count

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
    run_analysis read_config, pd do | batch_results |

      batch_results_size = batch_results.size
      current_item = 0
      sub_progress_was_visible = false # todo add this method: pd.get_sub_progress_visible
      pd.sub_progress_visible = true
      pd.set_sub_progress(0, batch_results_size)

      batch_results.each do | id, result |
        current_item += 1

        pd.sub_status_and_log_it = "Assigning results for #{result.path}"
        assign_custom_metadata source_items_map, result

        pd.set_sub_progress(current_item, batch_results_size)
      end

      pd.sub_progress_visible = sub_progress_was_visible
    end

    pd.main_status = nil
    pd.main_progress_visible = false

  end
ensure
  LOGGER.close
end
