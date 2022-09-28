item = $current_item
metadata = item.custom_metadata

# Find these things
obj_label = "gun"

# Display this many in the field
results_count = 2

output = ""
if metadata.include? "T3K Detections"

  count = metadata["T3K Detections|Count"]

  count = 0 if count.nil?

  found_list = []
  for index in 1..count do

    label = "T3K Detections|#{index}|#{obj_label}"

    # The label itself doesn't contain data, but all items will have a score...
    # so determine if this detection is the type of interest by looking for its score
    if metadata.include? "#{label}|Score"

      score = metadata["#{label}|Score"]

      # Add location information.  If it is a document, use the page and label
      if metadata.include? "#{label}|Page"
        page = metadata.get("#{label}|Page")
        image = metadata.get("#{label}|Image")
        found_list.append "#{score}% pg. #{page}, img. #{image}"

        # If it is a movie, add the frame
      elsif metadata.include? "#{label}|Frame"
        frame = metadata.get("#{label}|Frame")
        found_list.append "#{score}% frame #{frame}"

        # Otherwise just provide the score
      else
        found_list.append "#{score}%"
      end
    end
  end

  output = found_list.slice(0, results_count).join ", "

  output = output + "..." if found_list.size > results_count
end

output