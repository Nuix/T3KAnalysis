require 'json'
require_relative 'utilities/rest_client'
require_relative 'utilities/multi_logger'

# LOGGER = MultiLogger.instance

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

  # Performs a task, retrying it if it fails.
  #
  # The task is passed in as a block.  The block takes no parameters and should return true if it completed successfully
  # and false if it failed.  If the block returns false the block will be re-called after a delay.  After the defined
  # number of retries, the task will be aborted uncompleted.
  def do_with_retries(&block)
    attempts = 0

    do_try = true
    while do_try and (@retry_count >= 0 ? attempts <= @retry_count : true)
      do_try = !block.call

      if do_try
        sleep @retry_delay
        attempts += 1
        LOGGER.warn "Retrying task (#{attempts} / #{@retry_count}) ..."
      end
    end

    if do_try
      raise "Exceeded #{@retry_count} retry attempts.  Task not completed."
    end
  end

  # Upload a single file to the server and begin analyzing it.
  #
  # This endpoint is poorly named - the file must already exist on the server and this endpoint will simply tell the
  # server to begin working on it.  It would be better named "start_analysis" but I am keeping the method name here
  # consistent with the endpoint name (even though there is no 'upload' happening).
  #
  # The provided id must be a unique identifier for a running instance of the server.  It can be an integer or a String
  # that contains an integer (such as an_integer.to_s, or a string that could successfully be turned into an integer
  # with a_string.to_i).
  #
  # The provided path needs to be an absolute path on the server.  The host operating system is mounted on the server
  # under /host/host_mnt/ but the contents of that mount seem to have limited visibility - a file will pass the
  # first "exists" check made with this upload command, but will (could?) fail a later exists check when doing the
  # analysis (in my tests it took close to 10s to fail that check).  It was safer to put the files under a dedicated
  # mounting point.  I used something under /CORE/resources since that space was already used and visible by T3K.
  #
  # The upload request will be repeated if it fails with a 5xx error.  If it fails with a bad source id, the id will
  # be incremented and tried again.
  #
  # @raise Exceptions if there was a malformed request, or it took too many retries to complete.
  # @param [Int | String] id Unique identifier for the item to upload.  This is the request id, and will be different
  #                           than the results id.
  # @param [String] path Absolute path to the file as it exists on the server.  This path must be visible to the server
  #                      and seems to be best located under the /CORE mount point (at least under the /host seemed to
  #                      be only partially accessible to the server).
  # @return The results id - the unique identifier for polling status and getting results for this item.
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

  # Upload a series of items to analyze in a single batch.
  #
  # See the #upload(id, path) method for details.  The key in the Hash parameter for this method is the unique source id
  # for the item, and the value in the Hash is its path.
  #
  # @param [Hash<Int|String, String>] hash_of_items A Hash of items to upload, with source ids as keys for paths.  See
  #                                                 the documents for #upload(id, path) for details on the id and path
  #                                                 values.
  # @return A Hash with the results ids for each item as values, keyed to the source id that was sent to this method.
  # @see #upload(id, path)
  def batch_upload(hash_of_items)

  end


  # Perform a single poll attempt for the status of an item being processed.
  #
  # This poll attempt will be retried if there are server errors, will throw exceptions if request was malformed or
  # the file being processed was not found, and will return an error if the original image was found but broken.
  #
  # @todo Double check the errors here - perhaps be more permissive esp. of missing files
  # @param [Int | String] id The results id to check status for
  # @return The body of the poll request as a Hash
  def poll(id)
    poll_results = nil

    success = false

    do_with_retries do

      poll_response = @rest_client.get(ENDPOINT_POLL % { id: id})

      LOGGER.debug poll_response

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

  # Repeatedly poll an item until it completes.
  #
  # This method uses #poll(id) to get the status and waits for it to reach the finished status.
  #
  # @param [Int | String] id The results id for the item to wait for
  # @return A String with the results: "DONE" if finished without error, or an appropriate error message.
  # @see #poll(id)
  def wait_for_analysis(id)
    done = false
    results = nil

    do_with_retries do
      poll_results = poll id

      LOGGER.debug poll_results

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

  # Retrieve the results of analysis for an item.
  #
  # This function should be called only after the polling has returned in a finished state and without errors (if
  # using the #wait_for_analysis(id) method, call this after it returns "DONE").
  #
  # Getting the results will be retried if there is a server error, and an exception will be thrown if there is a
  # malformed request.
  #
  # @param [Int | String] id The results id for the items whose results will be retrieved
  # @return A Hash with the results of the analysis
  def get_results(id)
    analysis_results = nil

    do_with_retries do
      results = false

      analysis_response = @rest_client.get(ENDPOINT_RESULTS % { id: id })
      LOGGER.debug analysis_response

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
