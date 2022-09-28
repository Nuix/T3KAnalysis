require 'singleton'
require 'logger'

class MultiLogger
  include Singleton

  def initialize
    @outputs = []
    @logger = Logger.new(File.open(File::NULL, 'w'))
    @logger.formatter = proc do | severity, datetime, progname, msg |
      message = "#{datetime}: [#{progname}] #{severity.upcase} :: #{msg}"
      @outputs.each do | output |
        if output.class.method_defined? severity
          output.send severity, msg
        elsif output.class.method_defined? :puts
          output.puts message
        elsif output.class.method_defined? :write
          output.write message
        elsif output.class.method_defined? :log
          output.log message
        elsif output.class.method_defined? :log_message
          output.log_message message
        end
      end
    end
  end

  def add_output(logger)
    if logger.is_a? String
      # Assume it is a file path and open a logger to it
      @outputs.append Logger.new File.open(logger, 'w')
    else
      # Assume it has some means of being logged to...
      @outputs.append logger
    end
  end

  def close
    @outputs.each do | logger |
      if logger.class.method_defined? :close
        logger.close
      end
    end

    @logger.close
  end

  def log_level(severity)
    @logger.level = severity
  end

  def progname(program_name)
    @logger.progname = program_name
  end

  def debug(message)
    @logger.debug message
  end

  def info(message)
    @logger.info message
  end

  def warn(message)
    @logger.warn message
  end

  def error(message)
    @logger.error message
  end

  def fatal(message)
    @logger.fatal message
  end

  def unknown(message)
    @logger.unknown message
  end
end
