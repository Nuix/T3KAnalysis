script_directory = File.dirname $0
require_relative 'ui/settings_screen'

data_folder = File.join ENV['ProgramData'], 'Nuix', 'Nuix T3K Analysis'
settings_file = File.join(data_folder, 't3k_settings.json')

puts settings_file

dialog = build_settings_dialog settings_file
display_and_save_settings dialog
0