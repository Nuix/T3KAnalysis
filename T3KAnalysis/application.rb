require_relative 'utilities/multi_logger'

LOGGER = MultiLogger.instance
LOGGER.add_output STDOUT
LOGGER.progname "Nuix T3K Connector"
LOGGER.log_level :debug

require 'json'
require_relative 't3k_api'

CONFIG_FILE = "t3k_settings.json"
CONFIG_SERVER_PATH = "t3k_server_path"
CONFIG_LOCAL_PATH = "nuix_output_path"
ID_STORAGE_PATH = "t3k_data_id.json"
CONFIG_NEXT_ID = "last_id"

def analyze_batch(config, api, list_of_paths)
  last_id = config[CONFIG_NEXT_ID]
  server_path = config[CONFIG_SERVER_PATH]

  begin
    next_id = last_id
    batch = {}
    list_of_paths.each do | path |
      if File.exist? path
        next_id += 1
        server_file = "#{server_path}/#{File.basename(path)}"

        batch[next_id] = server_file
        Logger.info "Batch Processing #{batch}"

        result_id_map = api.batch_upload batch

        # rest of batch processing
      end
    end
  ensure
    config[CONFIG_NEXT_ID] = next_id
    save_config config
  end
end
def analyze_single(config, api, path)
  next_id = read_next_id
  server_path = config[CONFIG_SERVER_PATH]

  begin
    if File.exist? path
      server_file = "#{server_path}/#{File.basename(path)}"
      LOGGER.info "Processing #{next_id}: #{server_file}"

      result_id = api.upload next_id, server_file
      LOGGER.info "Result ID: #{result_id}"

      unless result_id.nil?
        wait_results = api.wait_for_analysis result_id
        LOGGER.info "Results of waiting: #{wait_results}"

        if "DONE" == wait_results
          analysis_results = api.get_results result_id
          LOGGER.info analysis_results
        end
      end

      next_id += 1
    end
  ensure
    save_next_id next_id
  end

end
def run_analysis(config)
  api = T3kApi.new config

  local_path = config[CONFIG_LOCAL_PATH].gsub "\\", "/"

  files_to_process = Dir.glob("#{local_path}/*")

  if files_to_process.size > 1
    analyze_batch config, api, files_to_process
  elsif files_to_process.size == 1
    analyze_single config, api, files_to_process[0]
  end
end

def read_config
  script_path = File.dirname $0
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