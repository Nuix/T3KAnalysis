script_directory = File.dirname $0
require_relative 'ui/settings_screen'

dialog = build_settings_dialog
settings_file = File.join script_directory, "..", "t3k_settings.json"
puts settings_file
puts "Exists: #{File.exist? settings_file}"
dialog.enable_sticky_settings settings_file
results = display_and_get_settings dialog

puts results