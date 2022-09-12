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
        end
      end
    end
  end

  def add_output(logger)
    @outputs.append logger
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
