require 'uri'
require 'json'
require 'net/http'

class RestClient
  def initialize(host, port)
    super()

    @host = host
    @port = port

    @authentication = :no_authentication
                    # or :token_authentication
                    # or :basic_authentication
  end

  def authentication=( method,
                       user = nil, password = nil,
                       token_field = nil, token = nil)
    case method
    when :basic_authentication
      if user.nil? or password.nil?
        raise ArgumentError, "Attempting to use basic authorization but user and password values are not set."
      end

      @authentication = :basic_authentication
      @user = user
      @password = password
    when :token_authentication
      if token_field.nil? or token.nil?
        raise ArgumentError, "Attempting to use token authentication but the token field name or value is not set."
      end

      @authentication = :token_authentication
      @token_field = token_field
      @token = token
    else
      @authentication = :no_authentication
    end
  end

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
    puts "Body: #{body_str}"

    request.body = body_str
  end


  def do_request(endpoint, query: nil, headers: nil, body: nil, authenticate: false)
    uri = build_address endpoint

    puts "URI: #{uri}"

    unless query.nil?
      add_query uri, query
    end

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

      puts "Making Request: #{request.method} #{request.uri} Body: #{request.body}"

      http.request request

    end

    {
      :code => response.code.to_i,
      :message => response.message,
      :body => JSON.parse(response.body)
    }
  end

  def get(endpoint, query: nil, headers: nil, authenticate: false)
    do_request(endpoint, query: query, headers: headers, body: nil, authenticate: authenticate) do | uri |
      Net::HTTP::Get.new uri
    end
  end

  def post(endpoint, query: nil, headers: nil, body: nil, authenticate: false)
    puts "Q: #{query} H: #{headers} B: #{body}"
    do_request(endpoint, query: query, headers: headers, body: body, authenticate: authenticate) do | uri |
      Net::HTTP::Post.new uri
    end
  end
end
