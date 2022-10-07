java_import "com.nuix.proserv.t3k.ws.ScriptingBase"
java_import "com.nuix.proserv.t3k.ws.ProgressListener"
java_import "com.nuix.proserv.t3k.ws.StatusListener"

java_import "java.util.concurrent.LinkedBlockingQueue"

case_folder = current_case.location.absolute_path
data_folder = File.join ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis'

log_file = File.join case_folder, 't3k.log'
LOG = ScriptingBase.init_logging(log_file, "DEBUG")
settings_file = File.join(data_folder, 't3k_settings.json')

class ProgressReport
  include ProgressListener
  include StatusListener

  def updateProgress(index, count, message)
    output = "Progress: #{index}/#{count}: #{message}"
    LOG.info output
    puts output
  end

  def updateStatus(message)
    output = "Status: #{message}"
    LOG.info output
    puts output
  end
end


if File.exist? settings_file
  scripting_base = ScriptingBase.new utilities, current_case, settings_file
  result = LinkedBlockingQueue.new
  report = ProgressReport.new

  exported_files = scripting_base.export_items current_selected_items, report
  scripting_base.analyze exported_files, result, report, report
  scripting_base.process_results result
end
