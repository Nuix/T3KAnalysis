java_import "com.nuix.proserv.t3k.ws.ScriptingBase"
java_import "com.nuix.proserv.t3k.ws.ProgressListener"
java_import "com.nuix.proserv.t3k.ws.StatusListener"

java_import "java.util.concurrent.LinkedBlockingQueue"

settings_file = File.join(ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis', 't3k_settings.json')

class ProgressReport
  include ProgressListener
  include StatusListener

  def updateProgress(index, count, message)
    puts "Progress: #{index}/#{count}: #{message}"
  end

  def updateStatus(message)
    puts "Status: #{message}"
  end
end


if File.exist? settings_file
  scripting_base = ScriptingBase.new utilities, current_case, settings_file
  result = LinkedBlockingQueue.new
  report = ProgressReport.new
  report2 = ProgressReport.new

  exported_files = scripting_base.export_items current_selected_items, report
  scripting_base.analyze exported_files, result, report, report2
end
