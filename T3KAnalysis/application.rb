require 'json'
require_relative 't3k_api'

CONFIG_FILE = "t3k_settings.json"
CONFIG_SERVER_PATH = "t3k_server_path"
CONFIG_LOCAL_PATH = "nuix_output_path"
CONFIG_BATCH_SIZE = "nuix_batch_size"
ID_STORAGE_PATH = "t3k_data_id.json"
CONFIG_NEXT_ID = "last_id"

class Application
  def initialize(config)
    super()

    @batch_analysis_listener = nil
    @item_analysis_listener = nil
    @config = config
    @api = T3kApi.new config

  end

  attr_accessor :batch_analysis_listener, :item_analysis_listener

  def run_analysis

    local_path = @config[CONFIG_LOCAL_PATH].gsub "\\", "/"

    files_to_process = Dir.glob("#{local_path}/*")

    if files_to_process.size > 1
      batches = build_batches files_to_process

      batch_count = batches.size
      current_batch = 0

      batches.each do | batch_of_files |
        current_batch += 1

        unless @batch_analysis_listener.nil?
          @batch_analysis_listener.batch_started current_batch, batch_count,
                                                 "Processing Batch #{current_batch}/#{batch_count}"
        end

        results_map = analyze_batch batch_of_files

        yield results_map

        unless @batch_analysis_listener.nil?
          @batch_analysis_listener.batch_completed current_batch, batch_count
        end
      end

      # analyze_batch config, api, files_to_process
    elsif files_to_process.size == 1
      results_map = analyze_single files_to_process[0]

      yield results_map
    end
  end

  def analyze_single(path)
    results_map = {}
    next_id = read_next_id
    server_path = config[CONFIG_SERVER_PATH]

    unless @item_analysis_listener.nil?
      @item_analysis_listener.analysis_started "Uploading to T3K"
      @item_analysis_listener.analysis_updated 0, 3
    end

    begin
      if File.exist? path
        server_file = "#{server_path}/#{File.basename(path)}"
        LOGGER.info "Processing #{next_id}: #{server_file}"

        result_id = api.upload next_id, server_file
        LOGGER.info "Result ID: #{result_id}"

        unless @item_analysis_listener.nil?
          @item_analysis_listener.analysis_updated 1, 3, "Waiting for Analysis to Complete"
        end

        unless result_id.nil?
          wait_results = @api.wait_for_analysis result_id
          LOGGER.info "Results of waiting: #{wait_results}"

          if "DONE" == wait_results

            unless @item_analysis_listener.nil?
              @item_analysis_listener.analysis_updated 2, 3, "Getting Results"
            end

            analysis_results = api.get_results result_id

            results_map[next_id] = analysis_results

            unless @item_analysis_listener.nil?
              @item_analysis_listener.analysis_updated 3, 3
              @item_analysis_listener.analysis_completed
            end

          end
        end

        next_id += 1
      end

      return results_map
    ensure
      save_next_id next_id
    end

  end

  def analyze_batch(list_of_paths)
    next_id = read_next_id
    server_path = @config[CONFIG_SERVER_PATH]

    item_count = list_of_paths.size
    current_item_index = 0

    unless @batch_analysis_listener.nil?
      @batch_analysis_listener.batch_updated 0, item_count, "Uploading Items in Batch"
    end

    begin
      batch = {}
      list_of_paths.each do | path |
        current_item_index += 1

        LOGGER.debug "Preparing #{path}"
        if File.exist? path
          server_file = "#{server_path}/#{File.basename(path)}"

          batch[next_id] = server_file

          unless @batch_analysis_listener.nil?
            @batch_analysis_listener.batch_updated  current_item_index, item_count
          end

          LOGGER.debug "[#{next_id}] #{path} added to request at #{server_file}"
          next_id += 1
        else
          LOGGER.info "#{path} does not exist"
        end
      end
      LOGGER.debug "Batch Processing #{batch}"

      result_id_map = @api.batch_upload batch
      LOGGER.debug result_id_map

      unless @batch_analysis_listener.nil?
        @batch_analysis_listener.batch_updated  0, item_count, "Waiting for Items to Complete"
      end

      # Wait for the batch to complete
      polling_queue = result_id_map.values.collect{|id| {:id => id, :cycle => 0}}
      completed_queue = []
      @api.wait_for_batch(polling_queue, completed_queue) do | completed_count |
        unless @batch_analysis_listener.nil?
          @batch_analysis_listener.batch_updated  completed_count, item_count
        end
      end

      LOGGER.debug completed_queue

      #Get the results of each item in the batch
      results_map = {}

      unless @batch_analysis_listener.nil?
        @batch_analysis_listener.batch_updated  0, item_count, "Getting Results"
      end

      current_item_index = 0
      until completed_queue.empty?
        item = completed_queue.shift

        current_item_index += 1

        item_id = item[:id]
        poll_result = item[:result]
        source_id = result_id_map.key(item_id)
        source_item = batch[source_id]

        if "DONE" == poll_result

          analysis_results = @api.get_results item_id
          results_map[source_id] = analysis_results

          unless @batch_analysis_listener.nil?
            @batch_analysis_listener.batch_updated  current_item_index, item_count
          end

        else
          LOGGER.error "#{source_item} produced an error during processing: #{poll_result}"
        end
      end

      return results_map
    ensure
      save_next_id next_id
    end
  end

  def build_batches(files_to_process)
    batch_size = @config[CONFIG_BATCH_SIZE].to_i

    result_batches = []
    files_to_process.each_slice(batch_size) do | batch |
      result_batches.append batch
    end

    result_batches
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
  # Let the script run from command line assuming a config file and files are present
  config = read_config

  app = Application.new config

  app.batch_analysis_listener = Class.new do
    def batch_started(index, number_of_batches, message=nil)
      puts "Batch [#{index}/#{number_of_batches}] #{message}"
    end

    def batch_updated(index, batch_size, message=nil)
      puts "Item [#{index}/#{batch_size}] #{message}"
    end

    def batch_completed(index, number_of_batches, message=nil)
      puts "Batch Complete [#{index}/#{number_of_batches}] #{message}"
    end
  end.new

  app.item_analysis_listener = Class.new do
    def analysis_started(message)
      puts "Item Started: #{message}"
    end

    def analysis_updated(step, total_steps, message)
      puts "Item Step [#{step}/#{total_steps}]: #{message}"
    end

    def analysis_completed(message)
      puts "Item Completed: #{message}"
    end

    def analysis_error(message)
      puts "ERROR: #{message}"
    end
  end

  app.run_analysis do | results |
    puts results
  end
end