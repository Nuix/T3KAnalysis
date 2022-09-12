require_relative 'utilities/multi_logger'

LOGGER = MultiLogger.instance
LOGGER.add_output STDOUT
LOGGER.progname "Nuix T3K Connector"
LOGGER.log_level :info

require 'json'
require_relative 't3k_api'

CONFIG_FILE = "T3KAnalysis.json"
CONFIG_SECTION = "t3k_analysis"
CONFIG_SERVER_PATH = "server_file_path"
CONFIG_LOCAL_PATH = "local_file_path"
CONFIG_LAST_ID = "last_data_id"

def analyze_batch(api, list_of_paths)
  last_id = config[CONFIG_SECTION][CONFIG_LAST_ID]
  server_path = config[CONFIG_SECTION][CONFIG_SERVER_PATH]

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
    config[CONFIG_SECTION][CONFIG_LAST_ID] = next_id
    save_config config
  end
end
def analyze_single(api, path)
  last_id = config[CONFIG_SECTION][CONFIG_LAST_ID]
  server_path = config[CONFIG_SECTION][CONFIG_SERVER_PATH]

  begin
    if File.exist? path
      next_id = last_id + 1

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
    end
  ensure
    config[CONFIG_SECTION][CONFIG_LAST_ID] = next_id
    save_config config
  end

end
def run_analysis(config)
  api = T3kApi.new config

  local_path = config[CONFIG_SECTION][CONFIG_LOCAL_PATH]

  files_to_process = Dir["#{local_path}/*"]

  if files_to_process.size > 1
    analyze_batch api, files_to_process
  elsif files_to_process.size == 1
    analyze_single api, files_to_process[0]
  end
end

def read_config
  script_path = File.dirname(__FILE__)
  config = JSON.load_file File.join(script_path, CONFIG_FILE)
end

def save_config(config)
  script_path = File.dirname(__FILE__)
  File.write File.join(script_path, CONFIG_FILE), JSON.generate(config)
end

if __FILE__ == $0
  # Script run from command line
  config = read_config

  run_analysis config
end