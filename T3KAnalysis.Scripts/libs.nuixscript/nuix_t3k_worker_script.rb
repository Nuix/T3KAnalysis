require 'fileutils'

# The kinds of files to send to T3K for analysis
$processable_kinds = %w[document image multimedia]

def init(worker_item)
  java_import "com.nuix.proserv.t3k.ws.ScriptingBase"

  # case_folder = current_case.location.absolute_path
  case_folder = worker_item.worker_store_dir.parent.parent.to_string

  log_file = File.join case_folder, 't3k.log'
  $LOG = ScriptingBase.init_logging(log_file, "TRACE")

  data_folder = File.join ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis'
  $LOG.info data_folder
  $settings_file = File.join(data_folder, 't3k_settings.json')
  $LOG.info $settings_file

  java_import "com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata"
  java_import "com.nuix.proserv.t3k.ws.WorkerItemMetadataMapWrapper"
  java_import "com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata"
  java_import "com.nuix.proserv.t3k.conn.Application"
  java_import "com.nuix.proserv.t3k.conn.AnalysisListener"
  java_import "com.nuix.proserv.t3k.conn.BatchListener"
  java_import "com.nuix.proserv.t3k.conn.ResultsListener"
  java_import "java.util.concurrent.LinkedBlockingQueue"
  java_import "java.util.LinkedList"
  $LOG.trace "Done with init"
end

def export_item_to_disk(worker_item, app_config)
  $LOG.trace "Exporting #{worker_item.item_guid}"

  source = LinkedList.new

  current_guid = worker_item.item_guid
  source_item = worker_item.source_item
  item_name = source_item.name
  item_kind = source_item.kind.name.downcase
  extension = source_item.corrected_extension

  if $processable_kinds.include? item_kind
    export_path = app_config.nuix_output_path
    export_file = File.join export_path, current_guid + "." + extension
    $LOG.debug "Exporting #{item_name} to #{export_file}"

    item_contents = source_item.binary
    if item_contents.nil?
      $LOG.info "[#{current_guid}]: This item has no binary contents available, it will not have any T3K results added."
    else
      unless File.exist? export_path
        $LOG.info "Creating export path: #{export_path}"
        FileUtils.mkdir_p export_path
      end

      file_contents = item_contents.binary_data

      $LOG.debug "Writing #{item_name} to #{export_file}"
      output_file = file_contents.copy_to export_file
      $LOG.debug "Done writing #{item_name} to #{export_file}"

      source.add export_file
    end
  else
    $LOG.trace "[#{current_guid}]: This is not a type that will be processed by T3K.  Type: #{item_kind}"
  end

  $LOG.trace "Done exporting"
  source
end

def run_t3k_analysis(items_to_process)
  result = LinkedBlockingQueue.new

  $LOG.debug "[#{items_to_process.peek}]: Sending item to T3K analysis"
  $scripting_base.analyze items_to_process, result, nil, nil, nil

  result
end

def apply_metadata_to_source(worker_item, result_queue)
  metadata_map = WorkerItemMetadataMapWrapper.new worker_item

  $LOG.debug "[#{worker_item.item_guid}]: Applying metadata to item."

  if result_queue.size < 2 or (result_queue.peek) == Application::END_OF_ANALYSIS
    $LOG.debug "[#{worker_item.item_guid}]: Analysis found nothing or had an error."
  else
    analysis_result = result_queue.poll # Assume just one result because we are doing a single item in the WSS
    unless analysis_result.nil?
      analysis_metadata = AnalysisMetadata::get_instance analysis_result, metadata_map
      analysis_metadata.apply_results
    end
  end
end

def cleanup(exported_files)
  exported_files.each do |file|
    $LOG.trace "Deleting #{file}"
    if File.exist? file
      File.delete file
    end
  end
end

def nuix_worker_item_callback(worker_item)
  init worker_item

  begin
    if File.exist? $settings_file
      $LOG.trace "Configuration present."
      $scripting_base = ScriptingBase.new $settings_file
      config = $scripting_base.config
      $LOG.info "Configuration: #{config.to_s}"

      current_guid = worker_item.item_guid
      source_item = worker_item.source_item
      item_name = source_item.name
      item_kind = source_item.kind.name.downcase
      $LOG.debug "Processing #{item_name}, with GUID=#{current_guid}.  Kind=#{item_kind}"

      exported_items = []
      begin
        exported_items = export_item_to_disk worker_item, config
        $LOG.debug "Exported #{exported_items.to_s}"

        unless exported_items.size < 1
          $LOG.info "Ready to anlyze exported items."
          analysis_result = run_t3k_analysis exported_items

          $LOG.info "Ready to apply results to the source item metadata."
          apply_metadata_to_source worker_item, analysis_result
        end
      ensure
        cleanup exported_items
      end
    else
      $LOG.warn "No T3K processing done, as the configuration is not found at #{settings_file}"
    end
  rescue => e
    $LOG.error e
  end

  $LOG.trace "Done with WSS on #{worker_item.item_guid}"
end
