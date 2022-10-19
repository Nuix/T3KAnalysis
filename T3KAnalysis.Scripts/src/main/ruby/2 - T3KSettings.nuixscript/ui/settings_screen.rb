script_directory = File.dirname $0
require_relative File.join script_directory, '..', 'libs.nuixscript', 'NxBootstrap'

def build_settings_dialog(settings_file)
  dialog = TabbedCustomDialog.new

  dialog.enable_sticky_settings settings_file

  # dialog.set_help_file help_file  todo Make a help file
  dialog.hide_file_menu
  dialog.set_title "Nuix T3K Analysis"
  dialog.set_size 450, 310

  main_tab = dialog.add_tab "main_tab", "Main"
  main_tab.append_directory_chooser "nuix_output_path", "File Export Directory"
  main_tab.append_local_worker_settings "Nuix Worker Settings"
  #  main_tab.append_spinner "next_id", "Next Request ID", 0

  server_tab = dialog.add_tab "t3k_server", "Server Settings"
  server_tab.append_text_field "t3k_server_url", "T3K Server URL", "http://127.0.0.1"
  server_tab.append_spinner "t3k_server_port", "T3K Server Port", 5000
  server_tab.append_text_field "t3k_server_path", "Server-side Path to Images", "/CORE/resources/data"
  server_tab.append_spinner "nuix_retry_count", "Request Retry Count", 10, 0, 50
  server_tab.append_spinner "nuix_retry_delay_seconds", "Request Retry Delay (seconds)", 1
  server_tab.append_spinner "nuix_batch_size", "Processing Batch Size", 100, 1, 1000, 10

  validate_settings(dialog)

  dialog
end

def display_and_save_settings(dialog)

  dialog.display
  dialog.get_control("nuix_retry_delay_seconds").set_value 0.5

  oked = dialog.get_dialog_result

  if oked
    dialog.to_map
  else
    false
  end
end

def validate_settings(dialog)
  dialog.validate_before_closing do | values |

    # server must be set
    if values["t3k_server_url"].nil? or values["t3k_server_url"].empty?
      CommonDialogs.show_warning "T3K Server URL must not be empty [#{values["t3k_server_url"]}]"
      next false
    end

    # port must be set
    if values["t3k_server_port"].nil?
      CommonDialogs.show_warning "T3K Server Port not be empty. [#{values["t3k_server_port"]}]"
      next false
    end

    # server directory path must be set
    if values["t3k_server_path"].nil? or values["t3k_server_path"].empty?
      CommonDialogs.show_warning "The Server-side path to Images must not be empty. [#{values["t3k_server_path"]}]"
      next false
    end

    # export directory path must be set
    if values["nuix_output_path"].nil? or values["nuix_output_path"].empty?
      CommonDialogs.show_warning "The File Export Directory must not be empty. [#{values["nuix_output_path"]}]"
      next false
    end

    # and must exist
    unless File.exist? values["nuix_output_path"]
      CommonDialogs.show_warning "The File Export Directory must be a Directory, but is not. [#{values["nuix_output_path"]}]"
      next false
    end

    # and must be a directory
    unless File.directory? values["nuix_output_path"]
      CommonDialogs.show_warning "The File Export Directory must be a Directory, but is not. [#{values["nuix_output_path"]}]"
      next false
    end

    if values["nuix_retry_count"].nil?
      CommonDialogs.show_warning "The Request Retry Count must not be empty. [#{values["nuix_retry_count"]}]"
      next false
    end

    if values["nuix_retry_delay_seconds"].nil?
      CommonDialogs.show_warning "The Request Retry Delay must not be empty. [#{values["nuix_retry_delay_seconds"]}]"
      next false
    end

    if values["nuix_batch_size"].nil?
      CommonDialogs.show_warning "The Process Batch Size must not be empty. [#{values["nuix_batch_size"]}]"
      next false
    end

    next true
  end
end

