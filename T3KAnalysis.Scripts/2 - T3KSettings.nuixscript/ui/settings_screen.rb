script_directory = File.dirname $0
require_relative File.join script_directory, '..', 'libs.nuixscript', 'NxBootstrap'

def build_settings_dialog()
  dialog = TabbedCustomDialog.new

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

  _validate_settings(dialog)

  dialog
end

def display_and_get_settings(dialog)
  dialog.display

  oked = dialog.get_dialog_result

  if oked
    dialog.to_map
  else
    false
  end
end

def _validate_settings(dialog)
  dialog.validate_before_closing do | values |
    next true
  end
end

