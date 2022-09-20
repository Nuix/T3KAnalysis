require 'uri'
require 'json'
require 'net/http'
require_relative 'multi_logger'

class RestClient
  def initialize(host, port)
    super()

    @host = host
    @port = port

    @authentication = :no_authentication

    @user = nil
    @password = nil
    @token_field = nil
    @token = nil
  end

  # @param [Label] method The method of authentication to use for this REST server.  One of:
  #                       :no_authentication Do not use authentication, and remove any existing credentials
  #                       :token_authentication Add a token to the Headers.  Requires providing the token_field:
  #                                             and token: parameters
  #                       :basic_authentication Use basic username and password authentication.  Requires providing
  #                                             user: and password: parameters
  # @param [String|nil] user (optional) If using :basic_authentication, this is the user name to login as.  nil otherwise.
  # @param [String|nil] password (optional) If using :basic_authentication, this is the password to login with.  nil otherwise.
  # @param [String|nil] token_field (optional) If using :token_authentication, this is the header key to set the token to.
  # @param [String|nil] token (optional) If using :token_authentication, this is the token to use for authentication.
  def authentication=( method,
                       user: nil, password: nil,
                       token_field: nil, token: nil)
    case method
    when :basic_authentication
      if user.nil? or password.nil?
        raise ArgumentError, "Attempting to use basic authorization but user and password values are not set."
      end

      @authentication = :basic_authentication
      @user = user
      @password = password
      @token_field = nil
      @token = nil
    when :token_authentication
      if token_field.nil? or token.nil?
        raise ArgumentError, "Attempting to use token authentication but the token field name or value is not set."
      end

      @authentication = :token_authentication
      @token_field = token_field
      @token = token
      @user = nil
      @password = nil
    else
      @authentication = :no_authentication
      @user = nil
      @password = nil
      @token_field = nil
      @token = nil
    end
  end

  # @param [String] endpoint Path added to the Host to make a request to
  # @param [Hash|nil] query (optional) Key-Value pairs of query parameters to add to the URL
  # @param [Hash|nil] headers (optional) Key-Value pairs of HTTP headers to send with the request.  The Accepts and
  #                           Content-Type headers will automatically be set to "application/json" but can be overriden
  #                           with this map.  Additionally, if the authentication method is set to :token_authentication
  #                           then the specified token field will be set in the headers and does not need to be part of
  #                           this map (if the authenticate parameter is true)
  # @param [Boolean] authenticate True to use authentication with this request.  To do so, the authentication property
  #                               must have been set before making this request or an error will occur.  When set to
  #                               true and using :basic_authentication the user and password fields will be added to
  #                               the request.  If using :token_authentication the token_field will be added as a header
  #                               with the provided token as the value.
  # @return A Hash with the following components:
  #         :code => The HTTP Response Code (such as 200, or 404)
  #         :message => The message attached to the HTTP Response (such as "OK")
  #         :body => The body of the HTTP Response parsed into a Hash, or nil if there was no body to the response
  def get(endpoint, query: nil, headers: nil, authenticate: false)
    LOGGER.debug "GET #{endpoint} Q: #{query} H: #{headers} A: #{authenticate}"
    do_request(endpoint, query: query, headers: headers, body: nil, authenticate: authenticate) do | uri |
      Net::HTTP::Get.new uri
    end
  end

  # @param [String] endpoint Path added to the Host to make a request to
  # @param [Hash|nil] query (optional) Key-Value pairs of query parameters to add to the URL
  # @param [Hash|nil] headers (optional) Key-Value pairs of HTTP headers to send with the request.  The Accepts and
  #                           Content-Type headers will automatically be set to "application/json" but can be overriden
  #                           with this map.  Additionally, if the authentication method is set to :token_authentication
  #                           then the specified token field will be set in the headers and does not need to be part of
  #                           this map (if the authenticate parameter is true)
  # @param [Hash|nil] body Hash containing the body, to be transformed into JSON before being sent
  # @param [Boolean] authenticate True to use authentication with this request.  To do so, the authentication property
  #                               must have been set before making this request or an error will occur.  When set to
  #                               true and using :basic_authentication the user and password fields will be added to
  #                               the request.  If using :token_authentication the token_field will be added as a header
  #                               with the provided token as the value.
  # @return A Hash with the following components:
  #         :code => The HTTP Response Code (such as 200, or 404)
  #         :message => The message attached to the HTTP Response (such as "OK")
  #         :body => The body of the HTTP Response parsed into a Hash, or nil if there was no body to the response
  def post(endpoint, query: nil, headers: nil, body: nil, authenticate: false)
    LOGGER.debug "POST #{endpoint} Q: #{query} H: #{headers} A: #{authenticate} B: #{body}"
    do_request(endpoint, query: query, headers: headers, body: body, authenticate: authenticate) do | uri |
      Net::HTTP::Post.new uri
    end
  end

  protected

  def add_query(uri, query_map)
    uri.query = URI.encode_www_form query_map
  end

  def build_address(endpoint)
    uri = URI.parse @host
    uri.port = @port
    uri.path = endpoint

    uri
  end

  def add_authentication(request)
    case @authentication
    when :basic_authentication
      request.basic_auth @user, @password
    when :token_authentication
      request[@token_field] = @token
    else
      # do nothing, no authentication
    end
  end

  def add_standard_headers(request)
    request['Content-Type'] = "application/json"
    request['Accept'] = "application/json"
  end

  def add_headers(request, header_map)
    header_map.each do | header, value |
      request[header] = value
    end
  end

  def add_body(request, body_map)
    body_str = JSON.generate body_map

    LOGGER.debug "Body: #{body_str}"

    request.body = body_str
  end

  # @param [String] endpoint Path added to the Host to make a request to
  # @param [Hash|nil] query (optional) Key-Value pairs of query parameters to add to the URL
  # @param [Hash|nil] headers (optional) Key-Value pairs of HTTP headers to send with the request.  The Accepts and
  #                           Content-Type headers will automatically be set to "application/json" but can be overriden
  #                           with this map.  Additionally, if the authentication method is set to :token_authentication
  #                           then the specified token field will be set in the headers and does not need to be part of
  #                           this map (if the authenticate parameter is true)
  # @param [Hash|nil] body Hash containing the body, to be transformed into JSON before being sent
  # @param [Boolean] authenticate True to use authentication with this request.  To do so, the authentication property
  #                               must have been set before making this request or an error will occur.  When set to
  #                               true and using :basic_authentication the user and password fields will be added to
  #                               the request.  If using :token_authentication the token_field will be added as a header
  #                               with the provided token as the value.
  # @param [Proc] &block After the URI is built (including the query string) the block will be called to generate the
  #                      HTTPRequest object.  The block must take one parameter - the URI to make a request against. The
  #                      block must return an HTTPRequest instance.
  # @return A Hash with the following components:
  #         :code => The HTTP Response Code (such as 200, or 404)
  #         :message => The message attached to the HTTP Response (such as "OK")
  #         :body => The body of the HTTP Response parsed into a Hash, or nil if there was no body to the response
  def do_request(endpoint, query: nil, headers: nil, body: nil, authenticate: false)
    LOGGER.debug "Request #{endpoint} Q: #{query} H: #{headers} A: #{authenticate} B: #{body}"

    uri = build_address endpoint
    unless query.nil?
      add_query uri, query
    end

    LOGGER.debug "URI: #{uri}"

    response = Net::HTTP.start(uri.host, uri.port) do | http |
      request = yield uri

      if authenticate
        add_authentication request
      end

      add_standard_headers request

      unless headers.nil?
        add_headers request, headers
      end

      if request.request_body_permitted?
        unless body.nil?
          add_body request, body
        end
      end

      LOGGER.debug "Making Request: #{request.method} #{request.uri}"

      LOGGER.debug "Headers:"
      request.each_header do | header, header_value |
        LOGGER.debug "    #{header}=#{header_value}"
      end

      unless request.body.nil?
        LOGGER.debug "Body: #{request.body}"
      end

      http.request request

    end

    LOGGER.debug "Response: #{response}"

    {
      :code => response.code.to_i,
      :message => response.message,
      :body => JSON.parse(response.body)
    }
  end
end
