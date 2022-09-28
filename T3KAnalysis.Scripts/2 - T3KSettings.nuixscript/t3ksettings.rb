script_directory = File.dirname $0
require_relative 'ui/settings_screen'

dialog = build_settings_dialog
settings_file = File.join script_directory, "..", "t3k_settings.json"
dialog.enable_sticky_settings settings_file
display_and_save_settings dialog
0