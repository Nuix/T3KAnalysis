java_import "com.nuix.proserv.t3k.ws.ScriptingBase"

case_folder = current_case.location.absolute_path

log_file = File.join case_folder, 't3k.log'
LOG = ScriptingBase.init_logging(log_file, "DEBUG")


java_import "com.nuix.proserv.t3k.conn.AnalysisListener"
java_import "com.nuix.proserv.t3k.conn.BatchListener"
java_import "com.nuix.proserv.t3k.conn.ResultsListener"
java_import "java.util.concurrent.LinkedBlockingQueue"

data_folder = File.join ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis'
settings_file = File.join(data_folder, 't3k_settings.json')

# The kinds of files to send to T3K for analysis
processable_kinds = %w[document image multimedia]

if File.exist? settings_file
  scripting_base = ScriptingBase.new utilities, current_case, settings_file
  result = LinkedBlockingQueue.new

  current_guid = current_item.item_guid
  source_item = current_item.source_item
  extension = source_item.corrected_extension
  item_kind = source_item.get_kind.downcase

  if processable_kinds.include? item_kind
    LOG.debug "Processing #{source_item.name}, with GUID=#{current_guid}.  Kind=#{item_kind}"


  end
else
  LOG.warn "No T3K processing done, as the configuration is not found at #{settings_file}"
end
