require "csv"
require "net/http"
require "json"
require 'logger'
require 'digest'
require 'java'
require 'json'
require 'thread'
require 'net/http'
require 'fileutils'
require 'date'
	
def nuixWorkerItemCallback(worker_item)
	exportpath = "C:/T3KAI/images"
	analyzekindsarray = []
	t3kaijsonfile = File.read('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json')
	t3kaijsonstring = JSON.parse(t3kaijsonfile)
	analyzekinds = t3kaijsonstring['analyzekinds']
	analyzekindsarray = analyzekinds.split(",")
	puts "Analyze kinds array Count #{analyzekindsarray.size}"
	exportdirectory = t3kaijsonstring['windowsexportlocation']
	puts "Windows Export Directory: '#{exportdirectory}'"
	linuxprocessingdir = t3kaijsonstring["linuxprocessingdir"]

	restserver = t3kaijsonstring["t3kairestserver"]
	restport = t3kaijsonstring["t3kairestport"]
	upload = t3kaijsonstring["t3kaiuploadendpoint"]
	poll = t3kaijsonstring["t3kaipollendpoint"]
	result = t3kaijsonstring["t3kairesultendpoint"]
	workercount = t3kaijsonstring["workerCount"]
	workermemory = t3kaijsonstring["workerMemory"]
	rightnow = DateTime.now.to_s
	rightnow = rightnow.delete(':')
	
	worker_item.setProcessItem(true)
	source_item = worker_item.getSourceItem
	item_type = source_item.type.name
	item_guid = worker_item.getItemGuid
	item_extension = source_item.getCorrectedExtension
	puts "Item Type: #{item_type}"
	item_kind = source_item.getKind
	puts "Item Type: '#{item_type}' : Item Kind: '#{item_kind}'"
	filename = source_item.getLocalisedName
	pathname = source_item.getLocalisedPathNames
	filepathname = source_item.getPathNames
	puts "File Path Name: #{filepathname } : Path name: #{pathname}  : File name: #{filename}"
	t3kid = t3kaijsonstring["t3kid"]
	t3kaibody = ''
	pathtoimage = ''

	if analyzekindsarray.include?(item_kind.to_s)
		binary = source_item.getBinary
		if binary.nil?
			puts "Binary has no data"
		else
			binarydata = binary.getBinaryData
			file = binarydata.copyTo(File.join(exportdirectory,item_guid + '.' + item_extension))
			pathtoimage = exportdirectory + '/' + item_guid + '.' + item_extension
			pathtoimage = pathtoimage.gsub(exportdirectory, linuxprocessingdir)
			pathtoimage = pathtoimage.gsub("\\","/")

			puts "Path to Image: #{pathtoimage}"
			uploadendpoint = restserver + ":" + restport + "/" + upload
			pollendpoint = restserver + ":" + restport + "/" + poll
			resultendpoint = restserver + ":" + restport + "/" + result
			t3kid +=1
			puts "t3Kid: #{t3kid}"

#			t3kaibody = '{"id":"' + "#{t3kid}" + '"file_path":' + "#{pathtoimage}" + '"}'
			t3kaibody = t3kaibody +  "\"#{t3kid}\"" + ":" + "\"#{pathtoimage}\""
			t3kaifullbody = '{' + t3kaibody + '}'
			puts "Upload Endpoint #{uploadendpoint}"
			
			uri = URI.parse("#{uploadendpoint}")
			request = Net::HTTP::Post.new(uri)
			request.content_type = "text/plain"
			request["Accept"] = "application/json"
			request.body = t3kaifullbody
			t3kaifullbodyjson = JSON.parse(t3kaifullbody)
			puts "Json Body: #{t3kaifullbody}"
			req_options = {
				use_ssl: uri.scheme == "https",
			}
			response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http|
				http.request(request)
			end
			begin
				t3kaijsonstring['t3kid'] = t3kid.to_i
				File.write('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json', JSON.pretty_generate(t3kaijsonstring))
			rescue => ecp
				puts "Exception - t3kAIanalyze: #{ecp.message}"
				puts "Exception - t3kAIanalyze: #{ecp.backtrace}"
				puts "Exception - t3kAIanalyze: #{ecp.class.name}"
			end
			responsecode = response.code
			responsebody = response.body
			puts "Response Body: #{responsebody}"
			begin
				if responsecode == '200'
					pollingitem = worker_item
					responsebodyvalues = JSON.parse(responsebody)
					responsebodyvalues.each do |responseid, responsepath|
						@responseid = responseid
						@responsepath = responsepath
					end
					polluri = URI.parse("#{pollendpoint}/#{@responseid}")

					resulttype = ''
					doneprocessing = false
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
							doneprocessing = true
						end
						if finished == true
							doneprocessing = true
						end
					end
					resulturi = URI.parse("#{resultendpoint}/#{@responseid}")
					resultresponse = Net::HTTP.get_response(resulturi)
					resultsjson = JSON.parse(resultresponse.body)
					detectionvalues = ''
					detections = ''
					detections = resultsjson["detections"]["0"]
					puts "Detections : #detection"
					detectionscount = detections.count
					puts "Detections Count: #detectionscount"
					if detectionscount == 0
						nomatch_count += 1
						pollingitem.addTag("T3KAI Detection|Nothing to Report")
						puts "T3KAi Result: Nothing to Report"
					else
						resultsjson["detections"]["0"].each do |detection|
							match_count += 1
							if detectionvalues == ''
								detectionvalues = "#{detection[0]} - #{detection[1]}"
							else
								detectionvalues = detectionvalues + "," + "#{detection[0]} - #{detection[1]}"
							end
							puts "Detection : #{detection}"
							puts "Detection Type : #{detection[0]}"
							puts "Detection Percent : #{detection[1]}"
							pollingitem .addTag("T3KAI Detection|#{detection[0]}")
							puts "T3KAI Detection Type: #{detection[0]} : perecent: #{detection[1]}"
						end
						pollingitem .getCustomMetadata["t3kairesult"] = "Match Detected"
						pollingitem .getCustomMetadata["t3kaidetection"] = "#{detectionvalues}"
					end
				elsif responsecode == '400'
					puts "Response code #{responsecode}"
#					@status_bar.setText("Response code #{responsecode}")
#					@t3klog.info("T3KAI Repsonse code #{responsecode}")
				elsif responsecode == '433'
					puts "Response code #{responsecode}"
#					@status_bar.setText("Response code #{responsecode}")
#					@t3klog.info("T3KAI Repsonse code #{responsecode}")
				else
					puts "Response code #{responsecode}"
#					@status_bar.setText("Response code #{responsecode}")
#					@t3klog.info("T3KAI Repsonse code #{responsecode}")
				end
			rescue => ecp
				puts "Exception - t3kAIanalyze: #{ecp.message}"
				puts "Exception - t3kAIanalyze: #{ecp.backtrace}"
				puts "Exception - t3kAIanalyze: #{ecp.class.name}"
			end
		end
	end
#	worker_guid = worker_item.getWorkerGuid()
#	CSV.open("C:\\Users\\CCarlson01\\Documents\\Nuix\\CrimesAgainstChildren\\Processing Files\\#{worker_guid}.csv", 'a+t')  do |csv|
#		worker_item.setProcessItem(true)
#		source_item = worker_item.getSourceItem
#		
#		item_type = source_item.type.name
#		if item_type == "application/xml"
#			source_item_text = ''
#			source_item_get_text = source_item.getText.to_s
#			source_item.getBinary.withBinaryData do |binary_data|
#				input_stream = binary_data.getInputStream
#				source_item_text = org.apache.commons.io.IOUtils.toString(input_stream,"UTF8")
#			end
#			doc = REXML::Document.new(source_item_text)
#			doc.root.each_recursive do |elem|
#				parent_node = elem.parent
#				path = get_node_path(parent_node)
#				elementvalue = elem.get_text
#				elementvalue = elementvalue.to_s.strip
#				if "#{elementvalue}" != nil && "#{elementvalue}".length < 256
#					basepath = path.map{|n|n.name}.join("/")
#					full_path = basepath + "/#{elem.name}"
#					full_path = full_path[1..-1]
#					full_path = full_path.gsub '/', '|'
#					full_path = full_path.gsub '| ', '|'
#					full_path = full_path.gsub ' | ', '|'
#					basepath = basepath.gsub '/', '|'
#					basepath = basepath.gsub '| ', '|'
#					basepath = basepath.gsub ' |', '|'
#					searchadditionaldatasql = "select ElementName from CACValues where ElementName = '|#{full_path}|' and SearchAdditionalData = -1"
#					searchadditionaldata = db.query(searchadditionaldatasql)
#					searchadditionaldata.each do |additionaldata|
#						cobwebsEmailEndpoint = cobwebsURL + '&Email=' + "#{elem.name}"
#						uri = URI(cobwebsEmailEndpoint)
#						response = Net::HTTP.get(uri)
#						cobwebsemailjson = JSON.parse(response)
#
#						cobwebsemailjson["persons"].each do |person|
#						msp = person["matchScorePercent"]
#						if msp == 1.0 
#							puts "value matched"
#						elsif msp == 0.0
#							puts "value not matched"
#						end
#					end


#					createtagsql = "select ElementName from CACValues where ElementName = '|#{full_path}|' and CreateTag = -1"
#					tagrecords = db.query(createtagsql)
#					tagrecords.each do |tagrecord|
#						if elementvalue.to_s.strip != '' && elementvalue.to_s.strip != nil && elementvalue.to_s.strip.length < 256
#							tag_path = full_path + '|' + elementvalue.to_s
#							tag_path = tag_path.delete("\n")
#							begin
#								puts "Adding Tag 1 #{tag_path} to #{worker_item.getSourceItem.getName}"
#								worker_item.addTag(tag_path)
#								csv << ["tag","#{tag_path}",Time.now.strftime("%d/%m/%Y")]
#							rescue
#								puts "ERROR 3 - Adding Tag #{tag_path} to #{worker_item.getSourceItem.getName}"
#							end
#						end
#						elem.attributes.each_attribute do |attrs| 
#							if attrs.name.to_s.strip != '' && attrs.name.to_s.strip != nil && attrs.name.to_s.strip.length < 256
#								if attrs.value.to_s.strip != '' && attrs.value.to_s.strip != nil && attrs.value.to_s.strip.length < 256
#									full_path = path.map{|n|n.name}.join("/") + "/#{elem.name}"
#									full_path = full_path[1..-1]
#									full_path = full_path.gsub '/', '|'
#									full_path = full_path.gsub '/', '|'
#									full_path = full_path.gsub '| ', '|'
#									full_path = full_path.gsub ' |', '|'
#									full_path = full_path + "|" + "#{attrs.name}"
#									tag_path = full_path + '|' + elementvalue.to_s + '|' + attrs.name + '|' + attrs.value.to_s.strip
#									tag_path = tag_path.delete("\n")
#									case_path = full_path.strip
#									itemtag = "#{case_path}|#{attrs.value.to_s.strip}"
#									full_tag = full_path + "|" + "#{attrs.value.to_s.strip}"
#									begin
#										puts "Adding Tag 2 #{tag_path} to #{worker_item.getSourceItem.getName}"
#										worker_item.addTag(tag_path)
#										csv << ["tag","#{tag_path}",Time.now.strftime("%d/%m/%Y")]
#									rescue
#										puts "ERROR 4 - Adding Tag #{tag_path} to #{worker_item.getSourceItem.getName}"
#									end
#								end
#							end
#						end
#					end
#					createcustommetadatasql = "select ElementName from CACValues where ElementName = '|#{full_path}|' and CreateCustomMetadata = -1"
#					metadatarecords = db.query(createcustommetadatasql)
#					metadatarecords.each do |metadatarecord|
#						if elementvalue.strip != '' && elementvalue.strip != nil && elementvalue.to_s.length < 256
#							case_path = full_path.strip
#							begin
#								worker_item.addCustomMetadata(full_path,elementvalue.to_s.strip,'text','user')
#								csv << ["custom_metadata","#{full_path}",elementvalue.to_s.strip,Time.now.strftime("%d/%m/%Y")]
#							rescue
#								puts "ERROR 1 - Adding Custom Metadata #{full_path} - #{elementvalue} to #{worker_item.getSourceItem.getName}"
#							end
#						end
#						elem.attributes.each_attribute do |attrs| 
#                          if attrs.name != '' && attrs.name != nil && attrs.name.to_s.length < 256
#								if attrs.value.to_s != '' && attrs.value.to_s != nil && attrs.value.to_s.length < 256
#									basepath = path.map{|n|n.name}.join("/")
#									begin
#										worker_item.addCustomMetadata(full_path,attrs.value.to_s.strip,'text','user')
#										csv << ["custom_metadata","#{full_path}",attrs.value.to_s.strip,Time.now.strftime("%d/%m/%Y")]
#									rescue
#										puts "ERROR 2 - Adding Custom Metadata #{full_path} - #{attrs.value.strip} to #{worker_item.getSourceItem.getName}"
#									end
#								end
#							end
#						end
#					end
#				end
#			end
#		end
#	end
end
