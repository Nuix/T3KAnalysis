require 'json'
require_relative 't3k_api'

CONFIG_FILE = "t3k_settings.json"
CONFIG_SERVER_PATH = "t3k_server_path"
CONFIG_LOCAL_PATH = "nuix_output_path"
CONFIG_BATCH_SIZE = "nuix_batch_size"
ID_STORAGE_PATH = "t3k_data_id.json"
CONFIG_NEXT_ID = "last_id"

def analyze_batch(config, api, list_of_paths, progress)
  next_id = read_next_id
  server_path = config[CONFIG_SERVER_PATH]

  progress.sub_progress_visible = true
  progress.sub_status_and_log_it = "Uploading Items in Batch"
  item_count = list_of_paths.size
  current_item_index = 0
  progress.set_sub_progress 0, item_count
  begin
    batch = {}
    list_of_paths.each do | path |
      current_item_index += 1

      LOGGER.debug "Preparing #{path}"
      if File.exist? path
        server_file = "#{server_path}/#{File.basename(path)}"

        batch[next_id] = server_file

        progress.set_sub_progress current_item_index, item_count
        LOGGER.debug "[#{next_id}] #{path} added to request at #{server_file}"
        next_id += 1
      else
        LOGGER.info "#{path} does not exist"
      end
    end
    LOGGER.debug "Batch Processing #{batch}"

    result_id_map = api.batch_upload batch
    LOGGER.debug result_id_map

    progress.sub_status_and_log_it = "Waiting for Items to Complete"
    progress.set_sub_progress 0, item_count

    # Wait for the batch to complete
    polling_queue = result_id_map.values.collect{|id| {:id => id, :cycle => 0}}
    completed_queue = []
    api.wait_for_batch polling_queue, completed_queue, progress
    LOGGER.debug completed_queue

    #Get the results of each item in the batch
    results_map = {}

    progress.set_sub_progress 0, item_count
    progress.sub_status_and_log_it = "Getting Results"
    current_item_index = 0
    until completed_queue.empty?
      item = completed_queue.shift

      current_item_index += 1

      item_id = item[:id]
      poll_result = item[:result]
      source_id = result_id_map.key(item_id)
      source_item = batch[source_id]

      if "DONE" == poll_result

        analysis_results = api.get_results item_id
        results_map[source_id] = analysis_results

        progress.set_sub_progress current_item_index, item_count

      else
        LOGGER.error "#{source_item} produced an error during processing: #{poll_result}"
      end
    end

    progress.sub_status = nil
    progress.sub_progress_visible = false

    return results_map
  ensure
    save_next_id next_id
  end
end

def analyze_single(config, api, path, progress)
  results_map = {}
  next_id = read_next_id
  server_path = config[CONFIG_SERVER_PATH]

  progress.set_sub_progress_visible false
  progress.sub_status_and_log_it = "Uploading to T3K"
  progress.set_main_progress 0, 3
  begin
    if File.exist? path
      server_file = "#{server_path}/#{File.basename(path)}"
      LOGGER.info "Processing #{next_id}: #{server_file}"

      result_id = api.upload next_id, server_file
      LOGGER.info "Result ID: #{result_id}"

      progress.set_main_progress 1, 3
      progress.sub_status_and_log_it = "Waiting for Analysis to Complete"

      unless result_id.nil?
        wait_results = api.wait_for_analysis result_id
        LOGGER.info "Results of waiting: #{wait_results}"

        if "DONE" == wait_results

          progress.set_main_progress 2, 3
          progress.sub_status_and_log_it = "Getting Results"

          analysis_results = api.get_results result_id

          results_map[next_id] = analysis_results
          progress.set_main_progress 3,3
        end
      end

      next_id += 1
    end
    progress.sub_status = nil

    return results_map
  ensure
    save_next_id next_id
  end

end

def build_batches(config, files_to_process)
  batch_size = config[CONFIG_BATCH_SIZE].to_i

  result_batches = []
  files_to_process.each_slice(batch_size) do | batch |
    result_batches.append batch
  end

  result_batches

end

def run_analysis(config, progress)
  api = T3kApi.new config

  local_path = config[CONFIG_LOCAL_PATH].gsub "\\", "/"

  files_to_process = Dir.glob("#{local_path}/*")

  if files_to_process.size > 1
    batches = build_batches config, files_to_process
    batch_count = batches.size
    current_batch = 0
    progress.set_main_progress current_batch, batch_count

    batches.each do | batch_of_files |
      current_batch += 1

      progress.main_status_and_log_it = "Processing Batch #{current_batch}/#{batch_count}"

      results_map = analyze_batch config, api, batch_of_files, progress

      yield results_map

      progress.set_main_progress current_batch, batch_count
    end

    # analyze_batch config, api, files_to_process
  elsif files_to_process.size == 1
    results_map = analyze_single config, api, files_to_process[0], progress

    yield results_map
  end
end

def read_config
  script_path = File.dirname File.dirname $0
  JSON.load_file File.join(script_path, CONFIG_FILE)
end

def read_next_id
  script_path = File.dirname $0

  next_id_file = File.join script_path, ID_STORAGE_PATH
  if File.exist? next_id_file
    hash = JSON.load_file File.join(script_path, ID_STORAGE_PATH)
    hash[CONFIG_NEXT_ID].to_i
  else
    0
  end
end

def save_next_id(next_id)
  script_path = File.dirname $0
  next_id_file = File.join script_path, ID_STORAGE_PATH
  next_id_data = { CONFIG_NEXT_ID => next_id }
  File.write next_id_file, JSON.generate(next_id_data)
end

if __FILE__ == $0
  # Script run from command line
  config = read_config

  run_analysis config
end