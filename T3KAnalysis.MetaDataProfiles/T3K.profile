<?xml version="1.0" encoding="UTF-8"?>
<metadata-profile xmlns="http://nuix.com/fbi/metadata-profile">
  <metadata-list>
    <metadata type="SPECIAL" name="Name" />
    <metadata type="CUSTOM" name="T3K Detections" />
    <metadata type="CUSTOM" name="T3K Detections|Count" />
    <metadata type="SPECIAL" name="Person">
      <scripted-expression>
        <type>ruby</type>
        <script><![CDATA[item = $current_item
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
]]></script>
      </scripted-expression>
    </metadata>
    <metadata type="SPECIAL" name="Gun">
      <scripted-expression>
        <type>ruby</type>
        <script><![CDATA[item = $current_item
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

output]]></script>
      </scripted-expression>
    </metadata>
    <metadata type="SPECIAL" name="Military Uniform">
      <scripted-expression>
        <type>ruby</type>
        <script><![CDATA[item = $current_item
metadata = item.custom_metadata

# Find these things
obj_label = "military_uniform"

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
]]></script>
      </scripted-expression>
    </metadata>
    <metadata type="SPECIAL" name="Army Tank">
      <scripted-expression>
        <type>ruby</type>
        <script><![CDATA[item = $current_item
metadata = item.custom_metadata

# Find these things
obj_label = "army_tank"

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
]]></script>
      </scripted-expression>
    </metadata>
    <metadata type="SPECIAL" name="License Plate">
      <scripted-expression>
        <type>ruby</type>
        <script><![CDATA[item = $current_item
metadata = item.custom_metadata

# Find these things
obj_label = "licenseplate"

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
]]></script>
      </scripted-expression>
    </metadata>
  </metadata-list>
</metadata-profile>
