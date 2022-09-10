require 'json'
require_relative 'rest_client/rest_client'

class T3kApi
  ENDPOINT_UPLOAD = "/upload"
  ENDPOINT_POLL = "/poll/%{id}"
  ENDPOINT_RESULTS = "/result/%{id}"
  ENDPOINT_SESSION = "/session/%{uid}"
  ENDPOINT_KEEP_ALIVE = "/keep-alive"
  ENDPOINT_SEARCH = "/search/%{uid}"

  POLL_FIELD_ID = "id"
  POLL_FIELD_FILEPATH = "filepath"
  POLL_FIELD_FINISHED = "finished"
  POLL_FIELD_PENDING = "pending"
  POLL_FIELD_ERROR = "error"
  POLL_FIELD_RESULT_TYPE = "result_type"
  POLL_FIELD_BROKEN_MEDIA = "BROKEN_MEDIA"
  POLL_FIELD_ID_NOT_FOUND = "ID_NOT_FOUND"
  POLL_FIELD_FILE_NOT_FOUND = "FILE_NOT_FOUND"
  POLL_FIELD_VALID_MEDIA = "VALID_MEDIA_OBJECT"

  RESULTS_DETECTION = "detections"
  RESULTS_METADATA = "metadata"
  METADATA_MD5 = "md5"

  DETECTION_FIELD_TYPE = "type"
  DETECTION_FIELD_SCORE = "score"
  DETECTION_FIELD_INFO = "info"
  DETECTION_FIELD_BOX = "box"

  DETECTION_TYPE_AGE_GENDER = "age/gender"
  DETECTION_TYPE_OBJECT = "object"
  DETECTION_TYPE_MD5 = "md5"
  DETECTION_TYPE_TEXT = "text"

  AGE_GENDER_VALUE_GENDER = "gender"
  AGE_GENDER_VALUE_AGE = "age"

  OBJECT_VALUE_CLASSIFICATION = "class_name"
  OBJECT_VALUE_DATA = "data"
  OBJECT_DATA_PAGE = "document_page_number"
  OBJECT_DATA_IMAGE = "document_image_number"
  OBJECT_DATA_FRAME = "frame"

  CONFIG_SECTION = "t3k_server"
  CONFIG_HOST = "host"
  CONFIG_PORT = "port"
  CONFIG_BATCH_SIZE = "batch_size"
  CONFIG_RETRY_COUNT = "retry_count"
  CONFIG_RETRY_DELAY = "retry_delay_seconds"

  def initialize(config)
    @config = config

    @host = config[CONFIG_SECTION][CONFIG_HOST]
    @port = config[CONFIG_SECTION][CONFIG_PORT]
    @rest_client = RestClient.new @host, @port

    @batch_size = config[CONFIG_SECTION][CONFIG_BATCH_SIZE]
    @retry_count = config[CONFIG_SECTION][CONFIG_RETRY_COUNT]
    @retry_delay = config[CONFIG_SECTION][CONFIG_RETRY_DELAY]
  end

  # Performs a task, retrying it if it fails.  The task is passed in as a block.  The block
  # should return true if it completed successfully and false if it failed.  After the
  # defined number of retries, the task will be aborted uncompleted.
  def do_with_retries(&block)
    attempts = 0

    do_try = true
    while do_try and (@retry_count >= 0 ? attempts <= @retry_count : true)
      do_try = !block.call

      if do_try
        sleep @retry_delay
        attempts += 1
        puts "Retrying task (#{attempts} / #{@retry_count}) ..."
      end
    end

    if do_try
      puts "Exceeded #{@retry_count} retry attempts.  Task not completed."
    end
  end

  def upload(id, path)
    result_id = nil

    do_with_retries do
      upload_body = { id => path }
      upload_request_results = @rest_client.post ENDPOINT_UPLOAD, body: upload_body

      success = false

      if upload_request_results[:code] == 434
        # invalid id, increment and try again
        id += 1
        success = false
      end

      if upload_request_results[:code] == 400
        # malformed request, fail.
        raise "The request was malformed.  Request: POST #{ENDPOINT_UPLOAD}, body=#{upload_body}"
      end

      puts upload_request_results

      if upload_request_results[:code] >= 500
        # server error, try again
        success = false
      end

      if upload_request_results[:code] == 200
        # success, finish
        result_id = upload_request_results[:body].keys[0]
        success = true
      end

      success
    end

    result_id
  end

  def poll(id)
    poll_results = nil

    success = false

    do_with_retries do

      poll_response = @rest_client.get(ENDPOINT_POLL % { id: id})

      puts poll_response

      if poll_response[:code] >= 500
        # Server error, try again
        success = false
      end

      if 400 == poll_response[:code]
        raise "The poll request was malformed: #{poll_response[:body].values[0]}"
      end

      if 404 == poll_response[:code]
        raise "The item being polled for was not found."
      end

      if 433 == poll_response[:code]
        # file was broken, stop polling and return the error
        poll_results = poll_response[:body]
        success = true
      end

      if poll_response[:code].between? 200, 299
        # polling was successful (work may not be done)
        poll_results = poll_response[:body]
        success = true
      end

      # Should not get here, assume bad, try again
      success
    end

    poll_results
  end

  def wait_for_analysis(id)
    done = false
    results = nil

    do_with_retries do
      poll_results = poll id

      puts poll_results

      if poll_results[T3kApi::POLL_FIELD_FINISHED]
        done = true

        if poll_results[POLL_FIELD_ERROR]
          if poll_results.key? POLL_FIELD_VALID_MEDIA
            results = poll_results[POLL_FIELD_VALID_MEDIA]
          elsif poll_results.key? POLL_FIELD_FILE_NOT_FOUND
            results = poll_results[POLL_FIELD_FILE_NOT_FOUND]
          elsif poll_results.key? POLL_FIELD_ID_NOT_FOUND
            results = poll_results[POLL_FIELD_ID_NOT_FOUND]
          else
            results = "Unknown Error: #{poll_results}"
          end
        else
          results = "DONE"
        end
      end

      done
    end

    results
  end

  def get_results(id)
    analysis_results = nil

    do_with_retries do
      results = false

      analysis_response = @rest_client.get(ENDPOINT_RESULTS % { id: id })
      puts analysis_response

      if analysis_response[:code] >= 500
        # Server error.  Try again
        results = false
      end

      if 400 == analysis_response[:code]
        raise "Malformed request for getting the results.  [#{analysis_response[:body]}]"
      end

      if 200 == analysis_response[:code]
        analysis_results = analysis_response[:body]
        results = true
      end

      results
    end

    analysis_results
  end
end
