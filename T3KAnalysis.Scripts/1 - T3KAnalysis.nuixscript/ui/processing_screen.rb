script_directory = File.dirname $0
require_relative File.join script_directory, '..', 'libs.nuixscript', 'NxBootstrap'

def show_processing_dialog
  report_data = ReportDataModel.new

  section1 = {"Analyzed" => 0, "Errors" => 0, "Detected" => 0, "Not Matched" =>0}
  report_data.add_section "Item Counts", section1

  ProgressDialog.forBlock do | pd |
    pd.set_title "Nuix T3K Analysis"
    pd.set_sub_status nil
    pd.set_sub_progress_visible false
    pd.set_abort_button_visible true
    pd.add_report report_data

    yield pd, report_data
  end
end
