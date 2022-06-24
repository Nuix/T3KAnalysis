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
	begin
		analyzekindsarray = []
		t3kaijsonfile = File.read('C:\\ProgramData\\Nuix\\ProcessingFiles\\T3KAI\\T3KAI.json')
		t3kaijsonstring = JSON.parse(t3kaijsonfile)
		analyzekinds = t3kaijsonstring['analyzekinds']
		analyzekindsarray = analyzekinds.split(",")
		puts "Analyze kinds array Count #{analyzekindsarray.size}"
		exporttime = DateTime.now.strftime "%d%m%Y%H%M"
		exportdirectory = t3kaijsonstring["windowsexportlocation"] + "\\" + exporttime
#		exportdirectory = t3kaijsonstring["windowsexportlocation"]
		puts "Windows Export Directory: '#{exportdirectory}'"
		linuxprocessingdir = t3kaijsonstring["linuxprocessingdir"] + "/" + exporttime
#		linuxprocessingdir = t3kaijsonstring["linuxprocessingdir"]
		puts "Linux Processing Directory: '#{linuxprocessingdir}'"

		restserver = t3kaijsonstring["t3kairestserver"]
		restport = t3kaijsonstring["t3kairestport"]
		upload = t3kaijsonstring["t3kaiuploadendpoint"]
		poll = t3kaijsonstring["t3kaipollendpoint"]
		result = t3kaijsonstring["t3kairesultendpoint"]
		workercount = t3kaijsonstring["workerCount"]
		workermemory = t3kaijsonstring["workerMemory"]
		retrycount = t3kaijsonstring["retrycount"]
		rightnow = DateTime.now.to_s
		rightnow = rightnow.delete(':')
		puts "Rest Server: #{restserver}"
		puts "Rest Port: #{restport}"
		puts "Upload Endpoint: #{upload}"
		puts "Poll Endpoint: #{poll}"
		puts "Result Endpoint: #{result}"
		puts "Worker Count: #{workercount}"
		puts "Worker Memory: #{workermemory}"
		puts "Retry Count: #{retrycount}"
	
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
				begin
					FileUtils.mkdir_p exportdirectory
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
	
#					t3kaibody = '{"id":"' + "#{t3kid}" + '"file_path":' + "#{pathtoimage}" + '"}'
					t3kaibody = t3kaibody +  "\"#{t3kid}\"" + ":" + "\"#{pathtoimage}\""
					t3kaifullbody = '{' + t3kaibody + '}'
					puts "Upload Endpoint #{uploadendpoint}"
	
					uri = URI.parse("#{uploadendpoint}")
					request = Net::HTTP::Post.new(uri)
					request.content_type = "text/plain"
					request["Accept"] = "application/json"
					request.body = t3kaifullbody
					t3kaifullbodyjson = JSON.parse(t3kaifullbody)
					puts "request body: #{request.body}"

					req_options = {
						use_ssl: uri.scheme == "https",
					}
					success=false
					failcount = 0
					while success == false && failcount < retrycount
						puts "In Batches while loop"
						begin
							response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http|
								http.request(request)
							end
							responsecode = response.code
							responsebody = response.body
							puts "Response Code: #{responsecode}"
							puts "Response Body: #{responsebody}"
							success = true
						rescue => ecp1
							failcount +=1
							success=false
							sleep 3
							puts "Failcount : #{failcount}"
							puts "Exception 1 - t3kAIanalyze: #{ecp1.backtrace}"
							puts "Exception 1 - t3kAIanalyze: #{ecp1.class.name}"
						end
					end

					if success == true
						puts "Response code: '#{responsecode}'"
						if responsecode == '200'
							success = false
							pollingitem = worker_item
							responsebodyjson = JSON.parse(responsebody)
							responsebodyvalues = JSON.parse(responsebody)
							responsebodyvalues.each do |responseid, responsepath|
								@responseid = responseid
								@responsepath = responsepath
							end
							polluri = URI.parse("#{pollendpoint}/#{@responseid}")

							resulttype = ''
							while success == false && failcount < retrycount
								begin
									doneprocessing = false
									puts "Polling T3KAI2: #{pollendpoint}/#{@responseid} : success: #{success} : failcount: #{failcount} : doneprocessing : #{doneprocessing}"
									until doneprocessing == true do
#											puts "In Polling Loop"
										pollresponse = Net::HTTP.get_response(polluri)
										polljson = JSON.parse(pollresponse.body)
										puts "Poll JSON : #{polljson}"
										id = polljson["id"]
										filepath = polljson["filepath"]
										finished = polljson["finished"]
										pending = polljson["pending"]
										error = polljson["error"]
										broken_media = ["BROKEN_MEDIA"]
										resulttype = polljson["resulttype"]
										filenotfound = polljson["FILE_NOT_FOUND"]
										puts "Polling response filepath : #{filepath} - finished : #{finished} - pending : #{pending} - error : #{error} - filenotfound : #{filenotfound}"
										if error == true
											puts "Error processing #{filepath} : #{broken_media}"
											doneprocessing = true
										end
										if finished == true
											puts "Finished analyzing in T3K"
											doneprocessing = true
										end
										success = true
										sleep 5
									end
								rescue => ecp2
									failcount +=1
									success=false
									sleep 3
									puts "Failcount : #{failcount}"
									puts "Exception 2 - t3kAIanalyze: #{ecp2.backtrace}"
									puts "Exception 2 - t3kAIanalyze: #{ecp2.class.name}"
								end
							end
							if !error == true
								success = false
								failcount = 0
								while success==false && failcount < retrycount
									begin
										success = true
										nuixdetectionvalues = ""
										detectioncount = 0
										puts "Getting Results from : #{resultendpoint}/#{@responseid}"
										resulturi = URI.parse("#{resultendpoint}/#{@responseid}")
										resultresponse = Net::HTTP.get_response(resulturi)
										resultsjson = JSON.parse(resultresponse.body)
										detections = resultsjson["detections"]
										puts "Results JSON : #{resultsjson}"
										begin
											resultsmd5 = resultsjson["metadata"]["md5"]
											puts "Results MD5 : #{resultsmd5}"
										rescue
											puts "No Results MD5"
										end
										if resultsmd5 != nil
											if pollingitem != nil
												detectioncount = 0
												itemdetections = {}
												puts "Detections Count #{detections.count}"
												if detections.count > 0
													detections.each do |k,v|
														t3kdetection = {}
														detectioncount +=1
														v.each do |key, value|
															t3kdetection.store(key,value)
														end
														itemdetections.store(detectioncount, t3kdetection)
													end 
												else
													pollingitem.addTag("T3KAI Detection|Nothing to Report")
													puts "No detections available"
												end
												agemap = {}
												gendermap = {}
												gendercount = 0
												agecount = 0
												documentpagearray = []
												framenumberarray = []
												framecount = 0
												pagecount = 0

												itemdetections.each do |key, value|
													if value.size > 0
														case value["type"]
															when "age/gender"
																gender = value["gender"]
																age = value["age"]
																score = value["score"]
																box = value["box"]
																info = value["info"]
																puts "Gender : #{gender} - Age : #{age} - Score : #{score} - info : #{info} - Box : #{box}"
																if !gendermap.key?(gender)
																	puts "Adding #{gender} to gendermap"
																	gendercount +=1
																	gendermap.store(gendercount, gender)														
																else
																	puts "#{gender} already exists in gender store"
																end
																puts "Detection Age : #{age}"
																if !agemap.key?(age)
																	puts "Adding #{age} to agemap"
																	agecount += 1
																	agemap.store(agecount, age)															
																else
																	puts "#{age} already exists in agemap"
																end
																detectiontype = "person"
																pollingitem.addTag("T3KAI Detection|#{detectiontype}")
																pollingitem.addTag("T3KAI Detection|person|#{gender}")
																pollingitem.addTag("T3KAI Detection|person|#{gender}|#{age}|#{score}")
																pollingitem.addCustomMetadata("t3kaidetection", "Match Detected", "text", "user")
																pollingitem.addCustomMetadata("t3kai-#{detectiontype}", "#{score}", "text", "user") 
																puts "Person identified"
															when "object"
																classname = value["class_name"]
																type = value["type"]
																score = value["score"]
																info = value["info"]
																box = value["box"]
																data = value["data"]
																puts "Class Name: #{classname} - Type : #{type} - Score : #{score} - info : #{info} - Box : #{box} - Data : #{data}"
																puts "Data: #{data}"
																if data != nil
																	data.to_s.delete!('[]')
																	fullarray = data.to_s.split(",")
																	arraycount = 0
																	fullarray.each do |arraystring|
																		if arraystring = '"document_page_number"'
																			documentpagenumber = fullarray[arraycount+1]
																			if documentpagenumber != nil
																				documentpagenumber.delete!("[")
																				documentpagenumber.delete!("]")
																				documentpagenumber.delete!('"')
																				documentpagenumber.delete!("\\")
																				if documentpagearray.include?(documentpagenumber)

																				else
																					if documentpagenumber != '" document_image_number"'
																						documentpagearray << documentpagenumber
																					end
																				end
																			end
																			puts "Document Page Number : #{documentpagenumber} : Arraycount : #{arraycount}"
																		elsif arraystring = '"frame"'
																			framenumber = fullarray[arraycount+1]
																			if framenumber != nil
																				framenumber.delete!("[")
																				framenumber.delete!("]")
																				framenumber.delete!('"')
																				framenumber.delete!("\\")
																				if framepagearray.include?(framenumber)
																			
																				else
																					framepagearray << framenumber
																				end
																			end
																				puts "Frame Number : #{framenumber}: Arraycount : #{arraycount}"
																		end
																		arraycount +=1
																	end
																end
																puts "Class Name: #{classname} - Type : #{type} - Score : #{score} - info : #{info} - Box : #{box}"
																pollingitem.addTag("T3KAI Detection|#{classname}")
																pollingitem.addTag("T3KAI Detection|#{classname}|#{score}")
																pollingitem.addCustomMetadata("t3kaidetection", "Match Detected", "text", "user")
																pollingitem.addCustomMetadata("t3kai-#{classname}", "#{score}", "text", "user") 
																if nuixdetectionvalues == ''
																	nuixdetectionvalues = "#{classname} - #{score}"
																else
																	if !nuixdetectionvalues.include? "#{classname} - #{score}"
																		nuixdetectionvalues = nuixdetectionvalues + "\n" + "#{classname} - #{score}"
																	end 
																end
																pollingitem.addCustomMetadata("t3kresult", "#{nuixdetectionvalues}", "text", "user")
														end
														genders = ""
														gendermap.each do |genderk,genderv|
															if genders == ""
																genders = genderv
															else
																genders = genders + "," + genderv
															end
														end
														puts "Genders : #{genders}"
														ages = ""
														agemap.each do |agesk, agesv|
															if ages == ""
																ages = agesv.to_s
															else
																ages = ages.to_s + "," + agesv.to_s
															end
														end
														puts "Ages : #{ages}"
														if genders != nil
															pollingitem.addCustomMetadata("t3kai-gender", "#{genders}", "text", "user")
														end
														if ages != nil
															pollingitem.addCustomMetadata("t3kai-ages", "#{ages}", "text", "user")
														end
														puts "Frame Number Array Count '#{framenumberarray.count}'"
														puts "Document Number Array Count '#{documentpagearray.count}'"

														if framenumberarray.count > 0
															pollingitem.addCustomMetadata("framenumbers","#{framenumberarray.to_s}", "text", "user")
														end
														documentpagevalues = ""
														if documentpagearray.count > 0
															documentpagearray.each do |pagearrayvalue|
																puts "Page Array Value '#{pagearrayvalue}'"
															
																if documentpagevalues != ""
																	if pagearrayvalue != " document_image_number"
																		documentpagevalues = documentpagevalues + "," + pagearrayvalue
																	end
																else
																	if pagearrayvalue != '"document_image_number"'
																		documentpagevalues = pagearrayvalue
																	end
																end
															end
															pollingitem.addCustomMetadata("documentpagenumbers", "#{documentpagevalues}", "text", "user")
														end

													else
														puts "No Detections"
													end
												end
											else
												puts "There is no Nuix item with MD5 : #{resultsmd5}"
											end
										else
											puts "There is no MD5 in the results"
										end
									rescue => ecp3
										failcount +=1
										success=false
										sleep 3
										puts "Failcount : #{failcount}"
										puts "Exception 3 - t3kAIanalyze: #{ecp3.backtrace}"
										puts "Exception 3 - t3kAIanalyze: #{ecp3.class.name}"
									end
								end
							else
								puts "There was an error analyzing #{pollendpoint}/#{returnid}"
							end
						elsif responsecode == '400'
							puts "T3KAI Repsonse code #{responsecode}"
						elsif responsecode == '433'
							puts "T3KAI Repsonse code #{responsecode}"
						elsif responsecode == '500'
							puts "T3KAI Repsonse code #{responsecode}"
						else
							puts "T3KAI Repsonse code #{responsecode}"
						end
					end
				rescue => ecp1
					puts "Exception1 - t3kAIanalyze: #{ecp1.message}"
					puts "Exception1 - t3kAIanalyze: #{ecp1.backtrace}"
					puts "Exception1 - t3kAIanalyze: #{ecp1.class.name}"
				ensure
#					FileUtils.rm_rf(exportdirectory)
				end
			end
		end
	rescue => ecp
		puts "General Exception - t3kAIanalyze: #{ecp.message}"
		puts "General Exception - t3kAIanalyze: #{ecp.backtrace}"
		puts "General Exception - t3kAIanalyze: #{ecp.class.name}"
	end
end
