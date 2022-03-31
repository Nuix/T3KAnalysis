require 'logger'
require 'digest'
require 'java'
require 'json'
require 'thread'
require 'net/http'
require 'fileutils'
require 'date'

#=========================Modules Libs================================

#UI Classes
module Js
    include_package "javax.swing"
end
module Jl
    include_package "java.lang"
end
module Ja
    include_package "java.awt"
end
module Lo
    include_package "org.jdesktop.layout"
end
module Jmx
   include_package "java.lang.management"
end

def thread_safe(&block) 
	Js::SwingUtilities.invokeAndWait(block) 
end

def thread_invokeLater(&block)
	Js::SwingUtilities.invokeLater(block)
end

class Numeric
	def percent_of(n)
		self.to_f / n.to_f * 100
	end
end
#Primary class
class Primary < Js::JFrame

	def initialize( current_case, utilities, window, current_selected_items, ba )
		super "T3KAI Analyze Interface:"
		@current_case = current_case
		@utilities = utilities
		@window = window
		@current_selected_items = current_selected_items
		@ba = ba
		@nativePath = ""
		@eOpt = nil
		@abortSignal = false
		@schema = nil
		@schemaVersion = nil
		@items = nil
		t3kaijsonfile = File.read('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json')
		t3kaijsonstring = JSON.parse(t3kaijsonfile)
		loglocation = t3kaijsonstring['nuixLogLocation']
		# Creates directory as long as it doesn't already exist
		Dir.mkdir(loglocation) unless Dir.exist?(loglocation)
		currenttime = Time.new
		timestring = currenttime.day.to_s + '-' + currenttime.month.to_s + '-' + currenttime.year.to_s + '-' + currenttime.hour.to_s + '-' + currenttime.min.to_s + '-' + currenttime.sec.to_s
		@t3klog = Logger.new("#{loglocation}/t3kstatus-#{timestring}.log")
		
		initUI
	end
	
	attr_accessor :items
	attr_accessor :entities
	attr_accessor :abortSignal
	attr_accessor :nativePath
	attr_reader :window
	attr_reader :utilities
	attr_reader :current_selected_items
	attr_reader :current_case
	attr_reader :ba
	
	def initUI
		panelMain = Js::JPanel.new()
		statusBar = Js::JLabel.new()
		if @current_selected_items.nil?
			statusBar.setText("No Items Selected")

			@t3klog.info("Status: No Items Selected")
		else
			statusBar.setText("Selected Item Count: #{@current_selected_items.size}") if @current_selected_items != nil

			@t3klog.info("Status: Selected Item Count: #{@current_selected_items.size}")
		end
		panelMain.add statusBar
		@status_bar = statusBar
		
		progressBar = Js::JProgressBar.new(0,100)
		progressBar.set_value(0)
		progressBar.set_string_painted(true)
		panelMain.add(progressBar)
		@progress_bar = progressBar
		
		# Add new Tabs
		tabbedPane = Js::JTabbedPane.new()
		importPanel = Js::JPanel.new()
		analyzePanel = Js::JPanel.new()
		settingsPanel = Js::JPanel.new()
		processingStatsPanel = Js::JPanel.new()
	
		#dropdown box to select template
		#Shared selection option for all goes in main panel
		typeSelectionLabel = Js::JLabel.new()
		typeSelectionLabel.setText("T3KAI Analyze Type:")

		@analyze_items = @current_selected_items
		
		selectionIndex = 0
		typeArray = ["selected", "kind:image", "kind:multimedia", "kind:document", "kind:( image OR multimedia OR document )", "kind:( image OR multimedia )", "kind:( image OR document )", "kind:( multimedia OR document )", "NQL"]
		
		typeSelectionBox=Js::JComboBox.new(typeArray.to_java)
		typeSelectionBox.setSelectedIndex(selectionIndex)
		typeSelectionBox.setToolTipText("Select the type of items to Analyze")
		
		#Only applies when someone clicks NQL in the item type drop down
		nuixQueryBoxLabel = Js::JLabel.new()
		nuixQueryBoxLabel.setText("Nuix Query for items:")
		
		nuixQueryBox=Js::JTextField.new()
		nuixQueryBox.addActionListener { |e|
			@analyze_items = @current_case.search("#{nuixQueryBox.getText()}")
			@status_bar.setText("Nuix Query Result Count: #{@analyze_items.size}: #{nuixQueryBox.getText()}")
			@t3klog.info("Nuix Query Result Count: #{@analyze_items.size}: #{nuixQueryBox.getText()}")
			self.revalidate
			self.repaint
		}
		typeSelectionBox.addActionListener { |e|
			nuixQueryBox.setText('')
			nuixQueryBox.setEnabled(false)
			typeSelectionText = typeSelectionBox.getItemAt(typeSelectionBox.getSelectedIndex())
			
			if typeSelectionText != "selected"
				if typeSelectionText == "NQL"
					nuixQueryBoxLabel.setEnabled(true)
					nuixQueryBox.setEnabled(true)
				else
					@analyze_items = @current_case.search("#{typeSelectionText}")
					@status_bar.setText("Analyze Item Count: #{@analyze_items.size}: #{typeSelectionText}")
					@t3klog.info("Analyze Item Count: #{@analyze_items.size}: #{typeSelectionText}")
					self.revalidate
					self.repaint
				end
			else
				@analyze_items = @current_selected_items
				@status_bar.setText("Selected Items Count: #{@current_selected_items.size}")
				@t3klog.info("Selected Items Count: #{@current_selected_items.size}")
				self.revalidate
				self.repaint
			end
		}
		analyzeCancelButton = Js::JButton.new()
		analyzeCancelButton.setText("Cancel")
		analyzeCancelButton.setToolTipText("Cancel analyze process gracefully")
		analyzeCancelButton.addActionListener { |e|
			Jl::System.gc()
			self.dispose()
		}


		#button to analyse selected items
		analyzeButton = Js::JButton.new()
		analyzeButton.setText("Analyze Items in T3KAI")
		analyzeButton.setToolTipText("Analyze selected items or items responsive to a query in T3KAI")

		analyzeButton.addActionListener { |e|
			@status_bar.setText("Calling t3KAIanalyze")
			@t3klog.info("Calling t3KAIanalyze")

			nuixQueryText = nuixQueryBox.getText().to_s
			typeSelectionText = typeSelectionBox.getItemAt(typeSelectionBox.getSelectedIndex())
			icount = 0
			if @analyze_items == nil
				if @current_selected_items != nil 
					@analyze_items == @current_selected_items
					@status_bar.setText("Total Selected Items Count: #{@current_selected_items.size}")
					@t3klog.info("Total Selected Items Count: #{@current_selected_items.size}")
					
				end
			end
			if @analyze_items != nil
				@status_bar.setText("Calling t3KAIanalyze #{@analyze_items.size}")
				@t3klog.info("Calling t3KAIanalyze #{@analyze_items.size}")
			end

			@abortSignal = false
			
			task = AnalyzeTask.new(self, analyzeButton, analyzeCancelButton, statusBar, progressBar, utilities, @analyze_items, processingStatsPanel, @processingStatsCompleteValue, @processingStatsErrorValue, @processingStatsDetectionsValue, @processingStatsNoDetectionsValue, @processingStatsTaggedItemsValue, @t3klog, @current_case)
#			task.add_property_change_listener { |e2|
#				if "progress" == e2.get_property_name
#					progressBar.set_value(e2.get_new_value.to_i)
#				end
#			}
			task.execute()
		}
		
		#Build Buttons
		if @current_case == nil
			analyzeButton.setEnabled false
		end
		
		analyzePanel.add typeSelectionLabel
		analyzePanel.add typeSelectionBox
		analyzePanel.add nuixQueryBoxLabel
		analyzePanel.add nuixQueryBox
		analyzePanel.add analyzeButton
		analyzePanel.add analyzeCancelButton
		
		#Analyze Panel layout
		fieldLayout = Js::GroupLayout.new(analyzePanel)
		fieldLayout.setVerticalGroup(
			fieldLayout.createSequentialGroup()
				.addComponent(typeSelectionLabel, 18, 18, 18)
				.addComponent(typeSelectionBox, 18, 18, 18)
				.addComponent(nuixQueryBoxLabel, 18, 18, 18)
				.addComponent(nuixQueryBox, 18, 18, 18)
				.addComponent(analyzeButton, 18, 18, 18)
				.addComponent(analyzeCancelButton, 18, 18, 18)
		)
		
		fieldLayout.setHorizontalGroup(
			fieldLayout.createParallelGroup(Js::GroupLayout::Alignment::LEADING)
				.addComponent(typeSelectionLabel, 150, 150, 150)
				.addComponent(typeSelectionBox, 380, 380, 380)
				.addComponent(nuixQueryBoxLabel, 380, 380, 380)
				.addComponent(nuixQueryBox, 380, 380, 380)
				.addComponent(analyzeButton, 260, 260, 260)
				.addComponent(analyzeCancelButton, 260, 260, 260)
		)
		analyzePanel.setLayout(fieldLayout)
		
		#Only applies to imports button to import from file
		fileSelectionLabel = Js::JLabel.new()
		fileSelectionLabel.setText("Source processing directory (file system path):")
		
		fileSelectionBox=Js::JTextField.new()
		
		fileSelectionButton=Js::JButton.new()
		fileSelectionButton.setText("Browse")
		fileSelectionButton.setToolTipText("Browse for folders to create nuix case from...")
		importPanel.add fileSelectionButton
		fileSelectionButton.addActionListener { |e|
			selectedFile = fileChooser("C:\/",0)
			fileSelectionBox.setText("#{selectedFile}")
			self.revalidate
			self.repaint
		}

		importCancelButton = Js::JButton.new()
		importCancelButton.setText("Cancel")
		importCancelButton.setToolTipText("Cancel process")
		importCancelButton.addActionListener { |e|
			Jl::System.gc()
			self.dispose()
		}
	
		importButton = Js::JButton.new()
		importButton.setText("Process to Nuix")
		importButton.setToolTipText("Process new items into Nuix and Analyze with T3KAI")
		importButton.addActionListener { |e|
			imagedirectory = fileSelectionBox.getText()
			imagedirectory = imagedirectory.gsub("\\","\\\\")
			
			processtask = ProcessAndAnalyzeTask.new(self, importButton, importCancelButton, fileSelectionButton, statusBar, progressBar, utilities, imagedirectory, processingStatsPanel, @processingStatsCompleteValue, @processingStatsErrorValue, @processingStatsDetectionsValue, @processingStatsNoDetectionsValue, @processingStatsTaggedItemsValue, @t3klog)
#			task.add_property_change_listener { |e2|
#				if "progress" == e2.get_property_name
#					progressBar.set_value(e2.get_new_value.to_i)
#				end
#			}
			processtask.execute()
			
		}
		
		importPanel.add fileSelectionLabel
		importPanel.add fileSelectionBox
		importPanel.add importButton
		importPanel.add importCancelButton

		#panelMain.add importButton
		fieldLayout = Js::GroupLayout.new(importPanel)
		fieldLayout.setVerticalGroup(
			fieldLayout.createSequentialGroup()
				.addComponent(fileSelectionLabel, 18, 18, 18)
				.addComponent(fileSelectionBox, 18, 18, 18)
				.addComponent(fileSelectionButton, 18, 18, 18)
				.addComponent(importButton, 18, 18, 18)
				.addComponent(importCancelButton, 18, 18, 18)
		)
	
		fieldLayout.setHorizontalGroup(
			fieldLayout.createParallelGroup(Js::GroupLayout::Alignment::LEADING)
				.addComponent(fileSelectionLabel, 380, 380, 380)
				.addComponent(fileSelectionBox, 380, 380, 380)
				.addComponent(fileSelectionButton, 260, 260, 260)
				.addComponent(importButton, 260, 260, 260)
				.addComponent(importCancelButton, 260, 260, 260)
		)
		importPanel.setLayout(fieldLayout)

		#Only applies to the settings panel
		t3kairestserverlabel = Js::JLabel.new()
		t3kairestserverlabel.setText("T3KAI REST server URL:")
		t3kairestserverBox=Js::JTextField.new()

		t3kairestportlabel = Js::JLabel.new()
		t3kairestportlabel.setText("T3KAI REST Port:")
		t3kairestportBox=Js::JTextField.new()

		t3kaiuploadlabel = Js::JLabel.new()
		t3kaiuploadlabel.setText("T3KAI REST upload endpoint:")
		t3kaiuploadBox=Js::JTextField.new()

		t3kaipolllabel = Js::JLabel.new()
		t3kaipolllabel.setText("T3KAI REST Poll endpoint:")
		t3kaipollBox=Js::JTextField.new()

		t3kairesultlabel = Js::JLabel.new()
		t3kairesultlabel.setText("T3KAI REST Result endpoint:")
		t3kairesultBox=Js::JTextField.new()

		t3kaiwindowsexportlabel = Js::JLabel.new()
		t3kaiwindowsexportlabel.setText("Windows export location:")
		t3kaiwindowsexportBox=Js::JTextField.new()

		t3kailinuxprocessingdirlabel = Js::JLabel.new()
		t3kailinuxprocessingdirlabel.setText("Linux Analyze Directory:")
		t3kailinuxprocessingdirBox=Js::JTextField.new()

		t3kaiworkercountlabel = Js::JLabel.new()
		t3kaiworkercountlabel.setText("Nuix Worker Count:")
		t3kaiworkercountBox=Js::JTextField.new()
		
		t3kaiworkermemorylabel = Js::JLabel.new()
		t3kaiworkermemorylabel.setText("Nuix Worker Memory:")
		t3kaiworkermemoryBox=Js::JTextField.new()
		
		t3kidlabel = Js::JLabel.new()
		t3kidlabel.setText("T3KAI Starting ID number:")
		t3kidBox=Js::JTextField.new()

		#button to analyse selected items
		testsettingsButton = Js::JButton.new()
		testsettingsButton.setText("Test connection to T3KAI")
		testsettingsButton.setToolTipText("Test the connection to the T3KAI server and endpoints")

		testsettingsButton.addActionListener { |e|
			@status_bar.setText("Testing settings to T3KAI")
			@t3klog.info("Testing settings to T3KAI")

			t3kaibody = ''
			t3kid = ''
			pathtoimage = ''
			restserver = t3kairestserverBox.getText()
			restport = t3kairestportBox.getText()
			upload = t3kaiuploadBox.getText()
			
			uploadendpoint = restserver + ":" + restport + "/" + upload
			@status_bar.setText("Upload Endpoint: #{uploadendpoint}")
			
			uri = URI.parse("#{uploadendpoint}")
			request = Net::HTTP::Post.new(uri)
			request.content_type = "text/plain"
			request["Accept"] = "application/json"
			request.body = JSON.dump({
#				"id" => idcount,
#				"file_path" => "#{pathtoimage}"
				t3kid => "#{pathtoimage}"
			})

			req_options = {
				use_ssl: uri.scheme == "https",
			}
			response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http|
					http.request(request)
			end

			@status_bar.setText("Successfully tested connection to #{restserver}")
			@t3klog.info("Successfully tested connection to #{restserver}")
		}

		#add field for number of category to provide the case
		#add field for getting tags in the case.
		settingsButton = Js::JButton.new()
		settingsButton.setText("Apply Settings:")
		settingsButton.setToolTipText("Apply settings to JSON file.")

		settingsCancelButton = Js::JButton.new()
		settingsCancelButton.setText("Cancel")
		settingsCancelButton.setToolTipText("Cancel and Close")

		settingsButton.addActionListener { |e|
			t3kaijsonfile = File.read('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json')
			t3kaijsonstring = JSON.parse(t3kaijsonfile)
			restserver = t3kairestserverBox.getText()
			restport = t3kairestportBox.getText()
			uploadendpoint = t3kaiuploadBox.getText()
			pollendpoint = t3kaipollBox.getText()
			resultendpoint = t3kairesultBox.getText()
			windowsexportlocation = t3kaiwindowsexportBox.getText()
			linuxprocessingdir = t3kailinuxprocessingdirBox.getText()
			nuixworkerCount = t3kaiworkercountBox.getText()
			nuixworkerMemory = t3kaiworkermemoryBox.getText()
			t3kid = t3kidBox.getText()
			t3kaijsonstring['t3kairestserver'] = restserver 
			t3kaijsonstring['t3kairestport'] = restport
			t3kaijsonstring['t3kaiuploadendpoint'] = uploadendpoint
			t3kaijsonstring['t3kaipollendpoint'] = pollendpoint
			t3kaijsonstring['t3kairesultendpoint'] = resultendpoint
			t3kaijsonstring['windowsexportlocation'] = windowsexportlocation
			t3kaijsonstring['linuxprocessingdir'] = linuxprocessingdir
			t3kaijsonstring['workerCount'] = nuixworkerCount.to_i
			t3kaijsonstring['workerMemory'] = nuixworkerMemory.to_i
			t3kaijsonstring['t3kid'] = t3kid.to_i
			
			File.write('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json', JSON.pretty_generate(t3kaijsonstring))
			@status_bar.setText("Settings Successfully updated")
			@t3klog.info("Settings Successfully updated")
			self.revalidate
			self.repaint
		}

		settingsCancelButton.addActionListener { |e|
			Jl::System.gc()
			self.dispose()
		}

		settingsPanel.add t3kairestserverlabel
		settingsPanel.add t3kairestserverBox
		settingsPanel.add t3kairestportlabel
		settingsPanel.add t3kairestportBox
		settingsPanel.add t3kaiuploadlabel
		settingsPanel.add t3kaiuploadBox
		settingsPanel.add t3kaipolllabel
		settingsPanel.add t3kaipollBox
		settingsPanel.add t3kairesultlabel
		settingsPanel.add t3kairesultBox
		settingsPanel.add t3kaiwindowsexportlabel
		settingsPanel.add t3kaiwindowsexportBox
		settingsPanel.add t3kailinuxprocessingdirlabel
		settingsPanel.add t3kailinuxprocessingdirBox
		settingsPanel.add t3kaiworkercountlabel
		settingsPanel.add t3kaiworkercountBox
		settingsPanel.add t3kaiworkermemorylabel
		settingsPanel.add t3kaiworkermemoryBox
		settingsPanel.add t3kidlabel
		settingsPanel.add t3kidBox
		settingsPanel.add testsettingsButton
		#Settings Pane layout
		settingsLayout = Js::GroupLayout.new(settingsPanel)
		settingsLayout.setVerticalGroup(
			settingsLayout.createSequentialGroup()
				.addComponent(t3kairestserverlabel, 18, 18, 18)
				.addComponent(t3kairestserverBox, 18, 18, 18)
				.addComponent(t3kairestportlabel, 18, 18, 18)
				.addComponent(t3kairestportBox, 18, 18, 18)
				.addComponent(t3kaiuploadlabel, 18, 18, 18)
				.addComponent(t3kaiuploadBox, 18, 18, 18)
				.addComponent(t3kaipolllabel, 18, 18, 18)
				.addComponent(t3kaipollBox, 18, 18, 18)
				.addComponent(t3kairesultlabel, 18, 18, 18)
				.addComponent(t3kairesultBox, 18, 18, 18)
				.addComponent(t3kaiwindowsexportlabel, 18, 18, 18)
				.addComponent(t3kaiwindowsexportBox, 18, 18, 18)
				.addComponent(t3kailinuxprocessingdirlabel, 18, 18, 18)
				.addComponent(t3kailinuxprocessingdirBox, 18, 18, 18)
				.addComponent(t3kaiworkercountlabel, 18, 18, 18)
				.addComponent(t3kaiworkercountBox, 18, 18, 18)
				.addComponent(t3kaiworkermemorylabel, 18, 18, 18)
				.addComponent(t3kaiworkermemoryBox, 18, 18, 18)
				.addComponent(t3kidlabel, 18, 18, 18)
				.addComponent(t3kidBox, 18, 18, 18)
				.addComponent(testsettingsButton, 18, 18, 18)
				.addComponent(settingsButton, 18, 18, 18)
				.addComponent(settingsCancelButton, 18, 18, 18)
		)
		
		settingsLayout.setHorizontalGroup(
			settingsLayout.createParallelGroup(Js::GroupLayout::Alignment::LEADING)
				.addComponent(t3kairestserverlabel, 380, 380, 380)
				.addComponent(t3kairestserverBox, 380, 380, 380)
				.addComponent(t3kairestportlabel, 380, 380, 380)
				.addComponent(t3kairestportBox, 380, 380, 380)
				.addComponent(t3kaiuploadlabel, 380, 380, 380)
				.addComponent(t3kaiuploadBox, 380, 380, 380)
				.addComponent(t3kaipolllabel, 380, 380, 380)
				.addComponent(t3kaipollBox, 380, 380, 380)
				.addComponent(t3kairesultlabel, 380, 380, 380)
				.addComponent(t3kairesultBox, 380, 380, 380)
				.addComponent(t3kaiwindowsexportlabel, 380, 380, 380)
				.addComponent(t3kaiwindowsexportBox, 380, 380, 380)
				.addComponent(t3kailinuxprocessingdirlabel, 380, 380, 380)
				.addComponent(t3kailinuxprocessingdirBox, 380, 380, 380)
				.addComponent(t3kaiworkercountlabel, 380, 380, 380)
				.addComponent(t3kaiworkercountBox, 380, 380, 380)
				.addComponent(t3kaiworkermemorylabel, 380, 380, 380)
				.addComponent(t3kaiworkermemoryBox, 380, 380, 380)
				.addComponent(t3kidlabel, 380, 380, 380)
				.addComponent(t3kidBox, 380, 380, 380)
				.addComponent(testsettingsButton, 380, 380, 380)
				.addComponent(settingsButton, 380, 380, 380)
				.addComponent(settingsCancelButton, 380, 380, 380)
		)
		
		settingsPanel.setLayout(settingsLayout)

		processingStatsCompleteLabel = Js::JLabel.new()
		processingStatsCompleteLabel.setText("T3KAI Items Analyzed:")
		@processingStatsCompleteValue=Js::JLabel.new()
		@processingStatsCompleteValue.setText("0")
		
		processingStatsErrorLabel = Js::JLabel.new()
		processingStatsErrorLabel.setText("T3KAI Items with Errors:")
		@processingStatsErrorValue=Js::JLabel.new()
		@processingStatsErrorValue.setText("0")
		
		processingStatsDetectionsLabel = Js::JLabel.new()
		processingStatsDetectionsLabel.setText("T3KAI Items Detected:")
		@processingStatsDetectionsValue=Js::JLabel.new()
		@processingStatsDetectionsValue.setText("0")

		processingStatsNoDetectionsLabel = Js::JLabel.new()
		processingStatsNoDetectionsLabel.setText("T3KAI Items Not Matched:")
		@processingStatsNoDetectionsValue = Js::JLabel.new()
		@processingStatsNoDetectionsValue.setText("0")

		processingStatsTaggedItemsLabel = Js::JLabel.new()
		processingStatsTaggedItemsLabel.setText("Tagged Items:")
		taggedItems = ["army_tank:3","gun:3","Nothing to Report:14"]
		taggedItemsstring = taggedItems.to_s
		taggedItemsstring = taggedItemsstring.gsub(",","\n")
		@processingStatsTaggedItemsValue = Js::JTextArea.new(30, 30)
		@processingStatsTaggedItemsValue.setText(taggedItemsstring)
		@processingStatsTaggedItemsValue.setEditable(false)
		
		processingStatsPanel.add processingStatsCompleteLabel
		processingStatsPanel.add @processingStatsCompleteValue
		processingStatsPanel.add processingStatsErrorLabel
		processingStatsPanel.add @processingStatsErrorValue
		processingStatsPanel.add processingStatsDetectionsLabel
		processingStatsPanel.add @processingStatsDetectionsValue
		processingStatsPanel.add processingStatsNoDetectionsLabel
		processingStatsPanel.add @processingStatsNoDetectionsValue
		processingStatsPanel.add processingStatsTaggedItemsLabel
		processingStatsPanel.add @processingStatsTaggedItemsValue

		processingStatsLayout = Js::GroupLayout.new(processingStatsPanel)
		processingStatsLayout.setVerticalGroup(
			processingStatsLayout.createSequentialGroup()
				.addComponent(processingStatsCompleteLabel, 18, 18, 18)
				.addComponent(@processingStatsCompleteValue, 18, 18, 18)
				.addComponent(processingStatsErrorLabel, 18, 18, 18)
				.addComponent(@processingStatsErrorValue, 18, 18, 18)
				.addComponent(processingStatsDetectionsLabel, 18, 18, 18)
				.addComponent(@processingStatsDetectionsValue, 18, 18, 18)
				.addComponent(processingStatsNoDetectionsLabel, 18, 18, 18)
				.addComponent(@processingStatsNoDetectionsValue, 18, 18, 18)
				.addComponent(processingStatsTaggedItemsLabel, 18, 18, 18)
				.addComponent(@processingStatsTaggedItemsValue, 300, 300, 300)
		)
		
		processingStatsLayout.setHorizontalGroup(
			processingStatsLayout.createParallelGroup(Js::GroupLayout::Alignment::LEADING)
				.addComponent(processingStatsCompleteLabel, 380, 380, 380)
				.addComponent(@processingStatsCompleteValue, 380, 380, 380)
				.addComponent(processingStatsErrorLabel, 380, 380, 380)
				.addComponent(@processingStatsErrorValue, 380, 380, 380)
				.addComponent(processingStatsDetectionsLabel, 380, 380, 380)
				.addComponent(@processingStatsDetectionsValue, 380, 380, 380)
				.addComponent(processingStatsNoDetectionsLabel, 380, 380, 380)
				.addComponent(@processingStatsNoDetectionsValue, 380, 380, 380)
				.addComponent(processingStatsTaggedItemsLabel, 380, 380, 380)
				.addComponent(@processingStatsTaggedItemsValue, 380, 380, 380)
		)
		
		processingStatsPanel.setLayout(processingStatsLayout)

		#Compose Tab Panes
		#Do not present export options if no current case open
		tabbedPane.addTab("Create and Analyze", importPanel) if @current_case == nil
		tabbedPane.addTab("Analyze existing", analyzePanel) if @current_case != nil
		tabbedPane.addTab("Processing Stats", processingStatsPanel)
		tabbedPane.addTab("Settings", settingsPanel)
		
		panelMain.add(tabbedPane)

		#Main Pane layout
		fieldLayout = Js::GroupLayout.new(panelMain)
		fieldLayout.setVerticalGroup(
			fieldLayout.createSequentialGroup()
				.addComponent(tabbedPane)
				.addComponent(statusBar,24,24,24)
				.addComponent(progressBar,24,24,24)
		)
		
		fieldLayout.setHorizontalGroup(
			fieldLayout.createParallelGroup(Js::GroupLayout::Alignment::LEADING)
				.addComponent(tabbedPane,400,400,400)
				.addComponent(statusBar,390,390,390)
				.addComponent(progressBar,390,390,390)
		)
		
		panelMain.setLayout(fieldLayout)

		t3kaijsonfile = File.read('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json')
		t3kaijsonstring = JSON.parse(t3kaijsonfile)

		restserverstring = t3kaijsonstring['t3kairestserver']
		restportstring = t3kaijsonstring['t3kairestport']
		uploadendpointstring = t3kaijsonstring['t3kaiuploadendpoint']
		pollendpointstring = t3kaijsonstring['t3kaipollendpoint']
		resultendpointstring = t3kaijsonstring['t3kairesultendpoint']
		windowsexportlocation = t3kaijsonstring['windowsexportlocation']
		linuxprocessingdir = t3kaijsonstring['linuxprocessingdir']
		workercount = t3kaijsonstring['workerCount']
		workermemory = t3kaijsonstring['workerMemory']
		
		t3kid = t3kaijsonstring['t3kid']
		
		t3kairestserverBox.setText(restserverstring)
		t3kairestportBox.setText(restportstring)
		t3kaiuploadBox.setText(uploadendpointstring)
		t3kaipollBox.setText(pollendpointstring)
		t3kairesultBox.setText(resultendpointstring)
		t3kaiwindowsexportBox.setText(windowsexportlocation)
		t3kailinuxprocessingdirBox.setText(linuxprocessingdir)
		t3kaiworkercountBox.setText(workercount.to_s)
		t3kaiworkermemoryBox.setText(workermemory.to_s)

		t3kidBox.setText(t3kid.to_s)
		
		nuixQueryBoxLabel.setEnabled(false)
		nuixQueryBox.setEnabled(false)
		self.setAlwaysOnTop(false)
		self.setDefaultCloseOperation DISPOSE_ON_CLOSE
		self.setLocationRelativeTo nil
		self.add panelMain
		self.setResizable(false)
		self.pack
		Jl::System.gc()
	end
	
	def fileChooser(loadFileDir,mode)
		chooser = javax.swing.JFileChooser.new
		if mode==0
			chooser.dialog_title = 'CHOOSE A SOURCE FILE DIR'
			chooser.file_selection_mode = javax.swing.JFileChooser::DIRECTORIES_ONLY
		else
			chooser.dialog_title = 'CHOOSE AN IMPORT FILE'
			chooser.file_selection_mode = javax.swing.JFileChooser::FILES_ONLY
		end
		
		chooser.setCurrentDirectory(java.io.File.new("#{loadFileDir}"))
		
		if chooser.show_open_dialog(nil) == javax.swing.JFileChooser::APPROVE_OPTION
			return chooser.selected_file.path
		else
			return nil
		end
	end
	
end

class ProcessAndAnalyzeTask < Js::SwingWorker
	def initialize(program, process_button, process_cancel_button, process_browse_button, status_bar, progress_bar, utilities, original_items, processingStatusPanel, processingStatsCompleteValue, processingStatusErrorValue, processingtatusDetectionsValue, processingStatsNoDetectionsValue, processingStatsTaggedItemsValue, t3klog)
		super()
		@program = program
		@process_button = process_button
		@process_cancel_button = process_cancel_button
		@process_browse_button = process_browse_button
		@status_bar = status_bar
		@progress_bar = progress_bar
		@utilities = utilities
		@original_items = original_items
		@processingStatusPanel = processingStatusPanel
		@processingStatsCompleteValue = processingStatsCompleteValue
		@processingStatusErrorValue = processingStatusErrorValue
		@processingtatusDetectionsValue = processingtatusDetectionsValue
		@processingStatsNoDetectionsValue = processingStatsNoDetectionsValue
		@processingStatsTaggedItemsValue = processingStatsTaggedItemsValue
		@t3klog = t3klog
	end
	
	def doInBackground
		begin
			@program.set_cursor(Ja::Cursor.getPredefinedCursor(Ja::Cursor::WAIT_CURSOR))			
			set_progress(1)
			@status_bar.setText("Start processing : #{@original_items}")
			@t3klog.info("Start processing : #{@original_items}")
			t3kProcessAndAnalyze(@original_items)
			set_progress(100)
			@program.set_cursor(Ja::Cursor.getPredefinedCursor(Ja::Cursor::DEFAULT_CURSOR))
		rescue => ecp
			Js::JOptionPane.showMessageDialog(@program, "Exception - doInBackground: #{ecp.backtrace}");
			@t3klog.info("Excpetion in doInBackground: #{ecp.class.name}")
			@t3klog.info("Excpetion in doInBackground: #{ecp.backtrace}")
			@t3klog.info("Excpetion in doInBackground: #{ecp.message}")
			@doneState = "error"
			@program.set_cursor(Ja::Cursor.getPredefinedCursor(Ja::Cursor::DEFAULT_CURSOR))
		end
	end
	
	def t3kProcessAndAnalyze(original_items)
		begin
			@process_button.setEnabled(false)
			@process_cancel_button.setEnabled(false)
			@process_browse_button.setEnabled(false)
			@status_bar.setText("Processing started on #{original_items} items...")
			@t3klog.info("Processing started on #{original_items} items...")
			@progress_bar.setValue(1)
			@progress_bar.setString("Processing...")
			#Forced to redeclare!?
			@abortSignal = false
			begin
				t3kaijsonfile = File.read('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json')
				t3kaijsonstring = JSON.parse(t3kaijsonfile)
				analyzekinds = t3kaijsonstring['analyzekinds']
				loglocation = t3kaijsonstring['nuixLogLocation']
				@t3klog.info("Log Location: #{loglocation}")
				caselocation = t3kaijsonstring['caseBaseDirectory']
				@t3klog.info("Case Location: #{caselocation}")
				casebasename = t3kaijsonstring['caseBaseName']
				@t3klog.info("Case Base Name: #{casebasename}")
				workercount = t3kaijsonstring['workerCount']
				@t3klog.info("Worker Count: #{workercount}")
				workermemory = t3kaijsonstring['workerMemory']
				@t3klog.info("Worker Memory: #{workermemory}")
				windowsexportlocation = t3kaijsonstring['windowsexportlocation']
				@t3klog.info("Windows Export Location: #{windowsexportlocation}")
				linuxprocessingdir = t3kaijsonstring['linuxprocessingdir']
				@t3klog.info("Linux Processing Directory: #{linuxprocessingdir}")

				callback_frequency = 5
				callback_count = 0
				worker_side_script = "C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAIWss.rb"
				@t3klog.info("Worker Side Script: #{worker_side_script}")
				#######################################

				case_name = 'T3KAI - ' + Time.now.strftime('%Y%d%m %H%M%S')
				@t3klog.info("Case Name: #{case_name}")

				caseFactory = @utilities.getCaseFactory()
				case_settings = {
					:compound => false,
					:name => "#{case_name}",
					:description => "Process in Nuix and Analyze T3KAI ",
					:investigator => "T3K Analyze"
				}
				$current_case = caseFactory.create(caselocation + '/' + case_name, case_settings)
				case_guid = $current_case.getGuid
				processor = $current_case.createProcessor
				processing_settings = {
					:traversalScope => "full_traversal",
					:processLooseFileContents => true,
					:processForensicImages => true,
					:stopWords => "none",
					:stemming => "none",
					:enableExactQueries => true,
					:extractNamedEntities => true,
					:extractNamedEntitiesFromTextStripped => true,
					:extractShingles => true,
					:processTextSummaries => true,
					:extractFromSlackSpace => false,
					:carveFileSystemUnallocatedSpace => false,
					:calculateAuditedSize => false,
					:storeBinary=> true,
					:maxStoredBinary => 256000000,
					:maxDigestSize => 256000000,
					:addBccToEmailDigests => false,
					:addCommunicationDateToEmailDigests => false,
					:processFamilyFields => true,
					:hideEmbeddedImmaterialData => false,
					:analysisLanguage => "en",
					:identifyPhysicalFiles => true,
					:reuseEvidenceStores => true,
					:skinToneAnalysis => true,
					:detectFaces => true,
					:exactqueries => true,
					:processtext => true,
					:createThumbnails => true,
					:calculateSSDeepFuzzyHash => true,
					:extractNamedEntitiesFromText => true,
					:extractNamedEntitiesFromProperties => true,
					:reportProcessingStatus => "physical_files",
					:digests => ["MD5","SHA-1","SHA-256"],
					:calculatePhotoDNARobustHash => true,
					:workerItemCallback => "ruby:#{IO.read(worker_side_script)}"
					}
					processor.setProcessingSettings(processing_settings)
					parallel_processing_settings = {
						:workerCount => workercount,
						:workerMemory => workermemory,
						:embedBroker => true,
						:brokerMemory => 1168,
						:workerTemp => "C:\\Temp\\WorkerTemp"
					}
					processor.setParallelProcessingSettings(parallel_processing_settings)
					evidence_name = Time.now.strftime('%Y-%d-%m %H:%M:%S %Z')
					evidence_container = processor.newEvidenceContainer(evidence_name)
					evidence_container.addFile(original_items)
					evidence_container.setEncoding("utf-8")
					evidence_container.save

					start_time = Time.now
					last_progress = Time.now
					semaphore = Mutex.new

					@t3klog.info("T3KAI Analysis processing started at #{start_time}...")
					processor.when_progress_updated do |progress|
					semaphore.synchronize {
					#		 Progress message every 15 seconds
						current_size = progress.get_current_size
						total_size = progress.get_total_size
						percent_completed = current_size.percent_of(total_size).round(1)
						@status_bar.setText("Percent Completed #{percent_completed}...")
						@t3klog.info("Percent Completed: #{percent_completed} : Current Size: #{current_size} : Total Size: #{total_size} ...")
						@progress_bar.set_value(percent_completed)
#						@progress_bar.setString("#{total_size}")
						if callback_count % callback_frequency == 0
							last_progress = Time.now
							@status_bar.setText("Processing Status #{last_progress} - items processed #{callback_count}...")
							@t3klog.info("Processing Status #{last_progress} - items processed #{total_size}...")
							@progress_bar.set_value(percent_completed)
#							@progress_bar.setString("#{percent_completed}")
						end
					}
					callback_count += 1
				end
				@t3klog.info("Starting Async Processing on #{@original_items}...")
				@status_bar.setText("Starting Async Processing on #{@original_items}...")
				processor.process
#				processor.processAsync
				@t3klog.info("Processing completed at  #{Time.now}...")
				FileUtils.rm_f Dir.glob("#{windowsexportlocation}/*")
				analyzedstring = ''
				detections = 0
				@t3klog.info("Analyze Kinds  #{analyzekinds}...")
				analyzekindsarray = analyzekinds.split(",")
				analyzekindsarray.each do |analyzekind|
					analyzedkindcount = $current_case.count("kind:#{analyzekind}")
					if analyzedstring == ''
						analyzedstring = "#{analyzekind}:#{analyzedkindcount}"
					else
						analyzedstring = analyzedstring + "," + "#{analyzekind}:#{analyzedkindcount}"
					end
				end
				@processingStatsCompleteValue.setText(analyzedstring)
				nomatchdetection = $current_case.count("tag:" + "\"T3KAI Detection|Nothing to Report\"")
				@processingStatsNoDetectionsValue.setText("#{nomatchdetection}")
				tags = $current_case.getAllTags
				alltagsarray = []
				tags.each do |tag|
					@status_bar.setText("Processing Tag: #{tag}")
					searchstring = "tag:" + "\"#{tag}\""
					#puts "Searchstring : #{searchstring}"
					tagcount =  $current_case.count("#{searchstring}")
					detections = detections + tagcount
					alltagsarray << "#{tag}:#{tagcount}"
				end
				alltagsstring = ''
				icount = 0
				alltagsarray.each do |tagname|
					if icount == 2
						alltagsstring = alltagsstring + "," + tagname + "\n"
						icount = 0
					else
						icount += 1
						if alltagsstring == ''
							alltagsstring = tagname
						else
							alltagsstring = alltagsstring + "," + tagname
						end
					end
				end
#				alltagsstring = alltagsstring.gsub(",", "\n")
				@t3klog.info("All tag array : #{alltagsarray.to_s}")
				@t3klog.info("All tag string : #{alltagsstring}")
				@processingStatsTaggedItemsValue.setText(alltagsstring)
				@processingtatusDetectionsValue.setText(detections.to_s)
				@progress_bar.set_value(100)
				@status_bar.setText("Processing completed - #{Time.now}...")
				@progress_bar.setString("Processing completed at #{Time.now}...")
				@process_cancel_button.setText("Close")
				@process_cancel_button.setEnabled(true)
				$current_case.close
			ensure
				Jl::System.gc()
				#self.dispose()
			end
		end
	end
end

class AnalyzeTask < Js::SwingWorker
	def initialize(program, analyze_button, analyze_cancel_button, status_bar, progress_bar, utilities, analyze_items, processingStatsPanel, processingStatsCompleteValue, processingStatsErrorValue, processingStatsDetectionsValue, processingStatsNoDetectionsValue, processingStatsTaggedItemsValue, t3klog, current_case)
		super()
		@program = program
		@analyze_button = analyze_button
		@analyze_cancel_button = analyze_cancel_button
		@status_bar = status_bar
		@progress_bar = progress_bar
		@utilities = utilities
		@doneState = "Normal"
		@processingStatsPanel = processingStatsPanel
		@processingStatsCompleteValue = processingStatsCompleteValue 
		@processingStatsErrorValue = processingStatsErrorValue
		@processingStatsDetectionsValue = processingStatsDetectionsValue 
		@processingStatsNoDetectionsValue = processingStatsNoDetectionsValue
		@processingStatsTaggedItemsValue = processingStatsTaggedItemsValue
		@analyze_items = analyze_items
		@t3klog = t3klog
		@current_case = current_case
	end
	
	attr_accessor :doneState
	
	def doInBackground
		begin
			@program.set_cursor(Ja::Cursor.getPredefinedCursor(Ja::Cursor::WAIT_CURSOR))			
			set_progress(1)
			@status_bar.setText("Item count in Analyze Task #{@analyze_items.size}")
#			@t3klog.info("Item count in Analyze Task #{@analyze_items.size}")
			t3KAIanalyze(@analyze_items)
			set_progress(100)
			@program.set_cursor(Ja::Cursor.getPredefinedCursor(Ja::Cursor::DEFAULT_CURSOR))
		rescue => ecp
			Js::JOptionPane.showMessageDialog(@program, "Exception - doInBackground: #{ecp.backtrace}");
			@t3klog.info("Excpetion in doInBackground: #{ecp.class.name}")
			@t3klog.info("Excpetion in doInBackground: #{ecp.backtrace}")
			@t3klog.info("Excpetion in doInBackground: #{ecp.message}")
			@doneState = "error"
			@program.set_cursor(Ja::Cursor.getPredefinedCursor(Ja::Cursor::DEFAULT_CURSOR))
		end
	end
	
	def t3KAIanalyze(items)
		begin
			error_count = 0
			finished_count = 0
			match_count = 0
			nomatch_count = 0
				
			@analyze_button.setEnabled(false)
			@analyze_cancel_button.setEnabled(false)
			@status_bar.setText("In t3KAIanalyze")
			@t3klog.info("In t3kaibody")
			jsonfile = "C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json"
			jsoncontents = File.read(jsonfile)
			t3kaijson = JSON.parse(jsoncontents)
			restserver = t3kaijson["t3kairestserver"]
			restport = t3kaijson["t3kairestport"]
			upload = t3kaijson["t3kaiuploadendpoint"]
			poll = t3kaijson["t3kaipollendpoint"]
			result = t3kaijson["t3kairesultendpoint"]
			workercount = t3kaijson["workerCount"]
			workermemory = t3kaijson["workerMemory"]
			@analyzekinds = t3kaijson["analyzekinds"]

			uploadendpoint = restserver + ":" + restport + "/" + upload
			pollendpoint = restserver + ":" + restport + "/" + poll
			resultendpoint = restserver + ":" + restport + "/" + result
			@t3klog.info("T3KAI restserver: #{restserver}")
			@t3klog.info("T3KAI rest port: #{restport}")
			@t3klog.info("T3KAI upload: #{upload}")
			@t3klog.info("T3KAI poll: #{poll}")
			@t3klog.info("T3KAI result: #{result}")
			@t3klog.info("Nuix Export worker count: #{workercount}")
			@t3klog.info("Nuix Export worker memory: #{workermemory}")
			

			begin
				exporttime = DateTime.now.strftime "%d%m%Y%H%M"
				exportfolder = t3kaijson["windowsexportlocation"] + "\\" + exporttime
				@t3klog.info("Exportfolder: #{exportfolder}")
				linuxprocessingdir = t3kaijson["linuxprocessingdir"] + "/" + exporttime
				@t3klog.info("Linuxprocessingdir: #{linuxprocessingdir}")
				t3kid = t3kaijson["t3kid"]
				@t3klog.info("t3kid: #{t3kid}")
#				case_name = @current_case.getName().strip
				rightnow = DateTime.now.to_s
				rightnow = rightnow.delete(':')
				batchexportname = "'#{exportfolder}'"
				@status_bar.setText("Exporting #{items.size} items to : #{exportfolder}")
				@t3klog.info("Exporting #{items.size} items to : #{exportfolder}")
				set_progress(25)
				exporter = @utilities.createBatchExporter(exportfolder)	
				exporter.setNumberingOptions({"createProductionSet" => false})
				exporter.setParallelProcessingSettings({
					:workerCount => workercount,
					:workerMemory => workermemory,
					:workerTemp => "C:\\Temp",
					:embedBroker => true,
					:brokerMemory => 768
				})

				natives_settings = {
					:naming => "guid",
					:path => "",
					:mailFormat => "pst",
					:includeAttachments => true,
				}
				exporter.addProduct("native", natives_settings)
				exporter.exportItems(items)
			rescue Java::JavaLang::IllegalStateException => e
				@status_bar.setText("Nuix erroneous IllegalStateException ignored")
				@t3klog.info("Nuix erroneous IllegalStateException ignored")
			end
			
			@status_bar.setText("items exported")
			@t3klog.info("items exported")
			
			#nativesdir = exportfolder + "\\NATIVE"
			nativesdir = exportfolder

			icount = 0
			idmap = {}
			t3kidmap = {}
			t3kaibody = ""
			items.each do |processitem|
				icount +=1
				percentcomplete = icount.fdiv(items.size)  * 100
				percentcomplete = percentcomplete.round()
#				Js::JOptionPane.showMessageDialog(@program, "Percent Complete: #{percentcomplete}: icount: #{icount} : item size: #{items.size}");

				set_progress(percentcomplete)

				@status_bar.setText("Percent Complete: #{percentcomplete} : Analyzing #{processitem.getName}")
				@t3klog.info("Percent complete: #{percentcomplete} : Analyizing #{processitem.getName}")
				@progress_bar.set_value(percentcomplete)

				itemguid = processitem.getGuid()
				itemname = processitem.getName()
				itemextension = processitem.getCorrectedExtension
				@status_bar.setText("Name: #{itemname} : Guid: #{itemguid}")
				@t3klog.info("Name: #{itemname} : Guid: #{itemguid}")

				nativesdir = nativesdir.gsub("\\","/")
				@t3klog.info("Nativesdir: #{nativesdir}")

				searchfiles = "#{nativesdir}/**/#{itemguid}.#{itemextension}"
				searchfiles = searchfiles.gsub("\\","/")
				exportfolder = exportfolder.gsub("\\","/")
				@t3klog.info("Search Files: #{searchfiles}")

				Dir.glob(searchfiles).each do |search_file|
					extension = File.extname(search_file)
					@t3klog.info("Search File: #{search_file}")
					pathtoimage = search_file.gsub(exportfolder, linuxprocessingdir)
					pathtoimage = pathtoimage.gsub("\\","/")
					@t3klog.info("Path To Image: #{pathtoimage}")

					t3kid +=1
#					t3kaibody = '{"id":"' + "#{t3kid}" + '"file_path":' + "#{pathtoimage}" + '"}'
					if t3kaibody == ""
						t3kaibody = t3kaibody +  "\"#{t3kid}\"" + ":" + "\"#{pathtoimage}\""
					else
						t3kaibody = t3kaibody + "," + "\"#{t3kid}\"" + ":" + "\"#{pathtoimage}\""
					end
					idmap.store("#{t3kid}", "#{itemguid}")
				end
			end
			t3kaifullbody = '{' + t3kaibody + '}'
			@t3klog.info("T3KAI Body: #{t3kaifullbody}")
			uri = URI.parse("#{uploadendpoint}")
			request = Net::HTTP::Post.new(uri)
			request.content_type = "text/plain"
			request["Accept"] = "application/json"
			request.body = t3kaifullbody
			t3kaifullbodyjson = JSON.parse(t3kaifullbody)
#			request.body = t3kaibody.to_json
			@t3klog.info("request body: #{request.body}")

			req_options = {
				use_ssl: uri.scheme == "https",
			}
			response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http|
				http.request(request)
			end

			responsecode = response.code
			responsebody = response.body
			@t3klog.info("Response Code: #{responsecode}")
			@t3klog.info("Response Body: #{responsebody}")

			@status_bar.setText("Response code: '#{responsecode}' : responsebody:'#{responsebody}'")
			@t3klog.info("Response code: '#{responsecode}' : responsebody:'#{responsebody}'")
			responsebodyjson = JSON.parse(responsebody)
			@t3klog.info("response body json #{responsebodyjson}")
			responsebodyjson.each do |returnkey, returnvalue|
				@t3klog.info("Response Body Key: #{returnkey} : Value: #{returnvalue}")
				t3kaifullbodyjson.each do |sentkey, sentvalue|
					@t3klog.info("Full Body JSON Key: #{sentkey} : Value: #{sentvalue}")
					if sentvalue == returnvalue
						@t3klog.info("Sent Value and Return Value equal: Sent Value #{sentvalue} : Return Key: #{returnvalue}")
						t3kidmap.store(sentkey, returnkey)
					end
				end
			end
			t3kidmap.each do |sendid, returnid|
				@t3klog.info("T3K Send ID : #{sendid} : TK3 Return ID: #{returnid}")
			end
			if responsecode == '200'
				@status_bar.setText("Successfully sent #{items.size} to T3KAI")
				@t3klog.info("Successfully sent #{items.size} to T3KAI")
				t3kidmap.each do |sendid, returnid|
					pollguid = idmap["#{sendid}"]
					@t3klog.info("Poll GUID: #{pollguid}")
					pollingitems = @current_case.search("guid:#{pollguid}")
					pollingitem = pollingitems[0]
					@t3klog.info("Poll ID: #{returnid} : Poll GUID: #{pollguid} : Poll Item: #{pollingitem.getName()}")
					polluri = URI.parse("#{pollendpoint}/#{returnid}")
					
					resulttype = ''
					doneprocessing = false
					@status_bar.setText("Polling T3KAI: #{responsebody}")
					@t3klog.info("Polling T3KAI: #{pollendpoint}/#{returnid}")
					until doneprocessing == true do
						pollresponse = Net::HTTP.get_response(polluri)
						polljson = JSON.parse(pollresponse.body)
						id = polljson["id"]
						filepath = polljson["filepath"]
						finished = polljson["finished"]
						pending = polljson["pending"]
						error = polljson["error"]
						broken_media = ["BROKEN_MEDIA"]
						resulttype = polljson["resulttype"]
						if error == true
							@status_bar.setText("Error processing #{filepath} : #{broken_media}")
							@t3klog.error("Error processing #{filepath} : #{broken_media}")
							error_count += 1
							doneprocessing = true
						end
						if finished == true
							@status_bar.setText("Completed is true")
							@t3klog.info("Complete")
							finished_count += 1
							doneprocessing = true
						end
					end
					@status_bar.setText("Processing done")
					@t3klog.info("Processing done")
					@t3klog.info("Getting Results from : #{resultendpoint}/#{returnid}")
					resulturi = URI.parse("#{resultendpoint}/#{returnid}")
					resultresponse = Net::HTTP.get_response(resulturi)
					resultsjson = JSON.parse(resultresponse.body)
					nuixdetectionvalues = ''
					detections = ''
					detections = resultsjson["detections"]["0"]
					detectionscount = detections.count
					if detectionscount == 0
						nomatch_count +=1
						pollingitem.addTag("T3KAI Detection|Nothing to Report")
					else
						resultsjson["detections"]["0"].each do |detection|
							match_count += 1
							if detection[0] == "None"
								detectiontype = ''
								detectionpercent = ''
								detectionvalues = detection[2]
								detectionvalues.each do |detectionvalue|
									detectiontype = detectionvalue[0]
									detectionpercent = detectionvalue[1]
									detectionpercent.delete! ' %'
									@t3klog.info("None Detection : #{detection}")
									@t3klog.info("None Detection Type : #{detectiontype}")
									@t3klog.info("None Detection Percent : #{detectionpercent}")
									if detectiontype != nil
										pollingitem.addTag("T3KAI Detection|#{detectiontype}")
										pollingitem.addTag("T3KAI Detection|#{detectionpercent}")
										pollingitem.getCustomMetadata["t3kaidetection"] = "Match Detected"
										custommetadata = pollingitem.getCustomMetadata
										metadatavalue = custommetadata["t3kai-#{detectiontype}"]
										if metadatavalue != nil
											if detectionpercent > metadatavalue
												pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
											end
										else
											pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
										end
									end
									if nuixdetectionvalues == ''
										nuixdetectionvalues = "#{detectiontype} - #{detectionpercent}"
									else
										if !nuixdetectionvalues.include? "#{detectiontype} - #{detectionpercent}"
											nuixdetectionvalues = nuixdetectionvalues + "\n" + "#{detectiontype} - #{detectionpercent}"
										end 
									end
								end
								pollingitem.getCustomMetadata["t3kresult"] = "#{nuixdetectionvalues}"
							elsif detection[0] == nil
								detectiontype = ''
								detectionpercent = ''
								detectionvalues = detection[2]
								detectionvalues.each do |detectionvalue|
									detectiontype = detectionvalue[0]
									detectionpercent = detectionvalue[1]
									detectionpercent.delete! ' %'
									@t3klog.info("nil Detection : #{detection}")
									@t3klog.info("nil Detection Type : #{detectiontype}")
									@t3klog.info("nil Detection Percent : #{detectionpercent}")
									if detectiontype != nil
										pollingitem.addTag("T3KAI Detection|#{detectiontype}")
										pollingitem.addTag("T3KAI Detection|#{detectionpercent}")
										pollingitem.getCustomMetadata["t3kaidetection"] = "Match Detected"
										custommetadata = pollingitem.getCustomMetadata
										metadatavalue = custommetadata["t3kai-#{detectiontype}"]
										if metadatavalue != nil
											if detectionpercent > metadatavalue
												pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
											end
										else
											pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
										end
									end
									if nuixdetectionvalues == ''
										nuixdetectionvalues = "#{detectiontype} - #{detectionpercent}"
									else
										if !nuixdetectionvalues.include? "#{detectiontype} - #{detectionpercent}"
											nuixdetectionvalues = nuixdetectionvalues + "\n" + "#{detectiontype} - #{detectionpercent}"
										end 
									end
								end
								pollingitem.getCustomMetadata["t3kresult"] = "#{nuixdetectionvalues}"
							elsif detection[0] == "nil"
								detectiontype = ''
								detectionpercent = ''
								detectionvalues = detection[2]
								detectionvalues.each do |detectionvalue|
									detectiontype = detectionvalue[0]
									detectionpercent = detectionvalue[1]
									detectionpercent.delete! ' %'
									@t3klog.info("nilstring Detection : #{detection}")
									@t3klog.info("nilstring Detection Type : #{detectiontype}")
									@t3klog.info("nilstring Detection Percent : #{detectionpercent}")
									if detectiontype != nil
										pollingitem.addTag("T3KAI Detection|#{detectiontype}")
										pollingitem.addTag("T3KAI Detection|#{detectionpercent}")
										pollingitem.getCustomMetadata["t3kaidetection"] = "Match Detected"
										custommetadata = pollingitem.getCustomMetadata
										metadatavalue = custommetadata["t3kai-#{detectiontype}"]
										if metadatavalue != nil
											if detectionpercent > metadatavalue
												pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
											end
										else
											pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
										end
									end
									if nuixdetectionvalues == ''
										nuixdetectionvalues = "#{detectiontype} - #{detectionpercent}"
									else
										if !nuixdetectionvalues.include? "#{detectiontype} - #{detectionpercent}"
											nuixdetectionvalues = nuixdetectionvalues + "\n" + "#{detectiontype} - #{detectionpercent}"
										end 
									end
								end
								pollingitem.getCustomMetadata["t3kresult"] = "#{nuixdetectionvalues}"
							else
								detectiontype = detection[0]
								detectionpercent = detection[1]
								detectionpercent.delete! ' %'
								@t3klog.info("notnullornil Detection Value : #{detectiontype}")
								@t3klog.info("notnullornil Detection Percent : #{detectionpercent}")
								if nuixdetectionvalues == ''
									nuixdetectionvalues = "#{detectiontype} - #{detectionpercent}"
								else
									if !nuixdetectionvalues.include? "#{detectiontype} - #{detectionpercent}"
										nuixdetectionvalues = nuixdetectionvalues + "\n" + "#{detectiontype} - #{detectionpercent}"
									end 
								end
								if detectiontype != nil
									pollingitem.addTag("T3KAI Detection|#{detectiontype}")
									pollingitem.addTag("T3KAI Detection|#{detectionpercent}")
									pollingitem.getCustomMetadata["t3kaidetection"] = "Match Detected"
									custommetadata = pollingitem.getCustomMetadata
									metadatavalue = custommetadata["t3kai-#{detectiontype}"]
									if metadatavalue != nil
										if detectionpercent > metadatavalue
											pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
										end
									else
										pollingitem.getCustomMetadata["t3kai-#{detectiontype}"] = "#{detectionpercent}"
									end
								end
								pollingitem.getCustomMetadata["t3kresult"] = "#{nuixdetectionvalues}"
							end
						end
					end
				end
			elsif responsecode == '400'
				@status_bar.setText("Response code #{responsecode}")
				@t3klog.info("T3KAI Repsonse code #{responsecode}")
			elsif responsecode == '433'
				@status_bar.setText("Response code #{responsecode}")
				@t3klog.info("T3KAI Repsonse code #{responsecode}")
			else
				@status_bar.setText("Response code #{responsecode}")
				@t3klog.info("T3KAI Repsonse code #{responsecode}")
			end
		end		
	rescue => ecp
		Js::JOptionPane.showMessageDialog(@program, "Exception - t3KAIanalyze: #{ecp.message}");
		@t3klog.info("Exception - t3kAIanalyze: #{ecp.backtrace}")
		@t3klog.info("Exception - t3kAIanalyze: #{ecp.class.name}")
		@doneState = "error"
	ensure
		nomatchdetection = @current_case.count("tag:" + "\"T3KAI Detection|Nothing to Report\"")
		@processingStatsNoDetectionsValue.setText("#{nomatchdetection}")
		analyzedstring = ''
		detections = 0
		@t3klog.info("Analyze Kinds: #{@analyzekinds}")
		analyzekindsarray = @analyzekinds.split(",")
		@t3klog.info("Analyze Kinds: #{analyzekindsarray}")
		analyzekindsarray.each do |analyzekind|
			analyzedkindcount = @current_case.count("kind:#{analyzekind}")
			if analyzedstring == ''
				analyzedstring = "#{analyzekind}:#{analyzedkindcount}"
			else
				analyzedstring = analyzedstring + "," + "#{analyzekind}:#{analyzedkindcount}"
			end
		end
		@processingStatsCompleteValue.setText(analyzedstring)

		tags = @current_case.getAllTags
		alltagsarray = []
		detections = 0
		tags.each do |tag|
			@status_bar.setText("Processing Tag: #{tag}")
			searchstring = "tag:" + "\"#{tag}\""
			#puts "Searchstring : #{searchstring}"
			tagcount =  @current_case.count("#{searchstring}")
			detections = detections + tagcount
			alltagsarray << "#{tag}:#{tagcount}"
		end
		@t3klog.info("All tags array : #{alltagsarray.to_s}")
		icount = 0
		alltagsstring = ''
		alltagsarray.each do |tagname|
			if icount == 2
				alltagsstring = alltagsstring + "," + tagname + "\n"
				icount = 0
			else
				icount += 1
				if alltagsstring == ''
					alltagsstring = tagname
				else
					alltagsstring = alltagsstring + "," + tagname
				end
			end
		end

		@processingStatsErrorValue.setText(error_count.to_s)
		@processingStatsDetectionsValue.setText(detections.to_s)
		@processingStatsNoDetectionsValue.setText(nomatchdetection.to_s)
		@processingStatsTaggedItemsValue.setText(alltagsstring)
		@status_bar.setText("Analyze Complete: #{@analyze_items.size} items")
		#@progress_bar.set_value(100)
		t3kaijsonfile = File.read('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json')
		t3kaijsonstring = JSON.parse(t3kaijsonfile)
		t3kaijsonstring['t3kid'] = t3kid.to_i
		File.write('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json', JSON.pretty_generate(t3kaijsonstring))
		@program.set_cursor(Ja::Cursor.getPredefinedCursor(Ja::Cursor::DEFAULT_CURSOR))
		FileUtils.rm_rf(exportfolder)
		@analyze_button.setEnabled(true)
		@analyze_cancel_button.setEnabled(true)
		@analyze_cancel_button.setText("Close")
	end
end

thread_invokeLater {
	ba = $utilities.getBulkAnnotater() if $current_case != nil
	mainframe = Primary.new($current_case, $utilities, $window, $current_selected_items,ba)
	mainframe.setVisible true
}
