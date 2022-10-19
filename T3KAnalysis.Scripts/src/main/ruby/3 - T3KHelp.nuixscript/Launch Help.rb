script_directory = File.dirname(__FILE__)
help = "nuix-mip-decryption-help.html"
help_file = File.join script_directory, help
IO.popen("start \"\" \"#{help_file}\"")
