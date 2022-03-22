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
	match_count = 0
	nomatch_count = 0

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
					puts "Results JSON: #{resultsjson}"
					nuixdetectionvalues = ''
					detections = ''
					detections = resultsjson["detections"]["0"]
					puts "Detections : {#detection}"
					detectionscount = detections.count
					puts "Detections Count: {#detectionscount}"
					if detectionscount == 0
						nomatch_count +=1
						pollingitem.addTag("T3KAI Detection|Nothing to Report")
					else
						resultsjson["detections"]["0"].each do |detection|
							match_count += 1
							puts "Detection : #{detection}"
							if detection[0] == "None"
								detectionvalues = detection[2]
								detectionvalues.each do |detectionvalue|
									detectiontype = detectionvalue[0]
									detectionpercent = detectionvalue[1]
									puts "Detection Type : #{detectiontype}"
									puts "Detetion Percent : #{detectionpercent}"
									if nuixdetectionvalues == ''
										nuixdetectionvalues = "#{detectiontype} - #{detectionpercent}"
									else
										nuixdetectionvalues = detectionvalues + "," + "#{detectiontype} - #{detectionpercent}"
									end
								end
								pollingitem.addTag("T3KAI Detection|#{detectiontype}")
								pollingitem.addCustomMetadata("t3kairesult", "Match Detected", "text", "api")
								pollingitem.addCustomMetadata("t3kaidetection", "#{nuixdetectionvalues}", "text", "api")
							else
								detectiontype = detection[0]
								detectionpercent = detection[1]
								puts "Detection Value : #{detection[0]}"
								puts "Detection Percent : #{detection[1]}"

								pollingitem.addTag("T3KAI Detection|#{detectiontype}")
								pollingitem.addCustomMetadata("t3kairesult", "Match Detected", "text", "api")
								pollingitem.addCustomMetadata("t3kaidetection", "#{detectiontype} - #{detectionpercent}", "text", "api")
							end
						end
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
end
