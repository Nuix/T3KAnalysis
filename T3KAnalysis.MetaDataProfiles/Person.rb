item = $current_item
metadata = item.custom_metadata

# Find these things
obj_label = "Person"

# Display this many in the field
results_count = 2

output = ""
if metadata.include? "T3K Detections"
  count = metadata["T3K Detections|Count"]

  count = 0 if count.nil?

  found_list = []
  (1..count).each { |index|

    label = "T3K Detections|#{index}|#{obj_label}"

    if metadata.include? "#{label}|Score"

      score = metadata["T3K Detections|#{index}|Person|Score"]

      # Person objects have special data format.  Use it instead
      # of the standard page/image or frame location.
      gender = metadata["T3K Detections|#{index}|Person|Gender"]
      age = metadata["T3K Detections|#{index}|Person|Age"]
      person = "#{gender}: #{age} (#{score}%)"

      found_list.append person
    end
  }

  output = found_list[0, results_count].join ", "
  output = output + "..." if found_list.size > results_count

end

output
