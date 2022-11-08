Analyze
Step 1. Files included
1.	anaylze.rb– to be copied to C:\Users\username\AppData\Roaming\Nuix\Scripts folder
2.	T3KAI.json – to be copied to C:\ProgramData\Nuix\ProcessingFiles\T3KAI folder
3.	T3KAIWss.rb  – to be copied to C:\ProgramData\Nuix\ProcessingFiles\T3KAI folder

Step 2. Modify T3KAI.json to meet your specifications – i.e.
{
  "t3kairestserver": "http://172.23.19.54",
  "t3kairestport": "5000",
  "t3kaiuploadendpoint": "upload",
  "t3kaipollendpoint": "poll",
  "t3kairesultendpoint": "result",
  "nuixServerType": "desktop",
  "nsmAddress": "",
  "registryServer": "",
  "nmsUid": "",
  "nmsPwd": "",
  "licenceType": "enterprise-workstation",
  "ocrOutput": "C:/nuix/ocr-out",
  "ocrQuery": "(kind:image or kind:document) and has-text:0",
  "sendMail": false,
  "generateReport": true,
  "additionalAnalysisItems": "(kind:image or kind:multimedia)",
  "reportType": "",
  "processingProfile": "C:/ProgramData/Nuix/Processing Profiles/T3KAIProfile",
  "caseBaseDirectory": "C:/Nuix_WORKING/T3KAI",
  "analyzekinds": "image,multimedia,document",
  "processingFilesDirectory": "C:/Nuix_WORKING/T3KAI",
  "reportsDirectory": "C:/Nuix_WORKING/T3KAI/reports",
  "caseBaseName": "T3KAI",
  "investigator": "investigator",
  "consoleLocation": "C:/Program Files/Nuix/Nuix 9.4/nuix_console.exe",
  "appMemory": "Xmx4g",
  "nuixLogLocation": "C:/Nuix_WORKING/Logs/T3KAI",
  "windowsexportlocation": "C:\\T3KAI\\images",
  "linuxprocessingdir": "/host/mnt/c/T3KAI/images",
  "t3kid": 679,
  "workerCount": 4,
  "workerMemory": 4096
}
Step 3. Run script to create new case
1.	Open Nuix and do no open a case
2.	Click on the Scripts Menu
3.	Choose analzye
5.	Click on the Settings tab
6.	Adjust the settings as necessary
7.	Click the “Apply Settings:” button
8.	Click the Test connection to T3KAI button
a.	If there is an error testing the connection a dialog will appear stating “Failed to open TCP connection to…”
b.	Fix any connection errors by modifying the IP address as necessary and ensuring port configuration is correct and that T3Kai is running
c.	If no errors occur connecting to T3Kai a status message will state “Successfully tested connection to…”
9.	Click on the “Create and Analyze” tab
10.	Click the “Browse” button to select the directory that the source items are stored in
11.	Click “Open”
12.	Click the “Process to Nuix” button
13.	This will start the processing items into Nuix while analyzing the items in T3KAI simultaneously
14.	The status bar will continue to update and when the process is complete the percent complete will state 100%
15.	You can click on the Processing Stats Tab and see the stats…
Step 4. Run script to analyze items already in a Nuix case
1.	Open Nuix and open a case
2.	Click on the Scripts Menu
3.	Choose analzye
5.	Click on the Settings tab
6.	Adjust the settings as necessary
7.	Click the “Apply Settings:” button
8.	Click the Test connection to T3KAI button
a.	If there is an error testing the connection a dialog will appear stating “Failed to open TCP connection to…”
b.	Fix any connection errors by modifying the IP address as necessary and ensuring port configuration is correct and that T3Kai is running
c.	If no errors occur connecting to T3Kai a status message will state “Successfully tested connection to…”
9.	Click on the “Create and Analyze” tab
10.	The T3KAI Analyze Type: options are
a.	"selected" – will analyze any items that are currently selected in the open case
b.	"kind:image" – will analyze all items in the open case
c.	"kind:multimedia" – will analyze all multimedia items in the open case
d.	"kind:document" – will analyze all documents in the open case
e.	"kind:( image OR multimedia OR document )" – will analyze all item, multimedia and document items in the open case
f.	"kind:( image OR multimedia )" – will analyze all image and multimedia items in the open case
g.	"kind:( image OR document )" – will analyze all image and document items in the open case
h.	"kind:( multimedia OR document )" – will analyze all multimedia and document items in the open case
i.	"NQL" – will allow the user to enter any valid Nuix Query Language query to sent to T3KAI to analyze
11.	Click the “Process to Nuix” button
12.	This will start the analyzing of the above items in T3KAI (NOTE: based on how many items are responsive to the selection this step could take a while because the process will export the items from Nuix to the “windowsexportlocation” specified in the T3KAI.json file 
13.	The status bar will continue to update and when the process is complete the percent complete will state 100%
14.	You can click on the Processing Stats Tab and see the stats…
