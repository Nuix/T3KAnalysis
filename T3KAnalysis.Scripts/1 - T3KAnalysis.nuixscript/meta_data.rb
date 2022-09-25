class MetaData
  def self.t3k
    "T3K Detections"
  end

  def self.detection_count
    "#{MetaData.t3k}|Count"
  end

  def self.detection_index(index)
    "#{MetaData.t3k}|#{index}"
  end

  def self.person(index)
    base = "#{MetaData.detection_index index}|Person"
    %W(#{base}|Age #{base}|Gender #{base}|Score)
  end

  def self.object(index, classification)
    base = "#{MetaData.detection_index index}|#{classification}"
    "#{base}|Score"
  end

  def self.md5(index)
    base = "#{MetaData.detection_index index}|MD5 Hit"
    %W(#{base}|Type #{base}|Hash #{base}|Description #{base}|ID)
  end

  def self.text(index)
    base = "#{MetaData.detection_index index}|Text Hit"
    %W(#{base}|String #{base}|Description #{base}|Language #{base}|Regex #{base}|Fuzzy #{base}|MLR #{base}|Matches)
  end

  def self.unknown(index)
    "#{MetaData.detection_index index}|Unknown"
  end

  def self.video_data(index, detection)
    case detection
    when "person"
      base = MetaData.person index
    when "md5"
      base = MetaData.md5 index
    when "text"
      base = MetaData.text index
    else
      base = MetaData.object index, detection
    end

    "#{base}|Frame"
  end

  def self.document_data(index, detection)
    case detection
    when "person"
      base = "#{MetaData.detection_index index}|Person"
    when "md5"
      base = "#{MetaData.detection_index index}|MD5 Hit"
    when "text"
      base = "#{MetaData.detection_index index}|Text Hit"
    else
      base = "#{MetaData.detection_index index}|#{detection}"
    end

    %W(#{base}|Page #{base}|Image)
  end

  def self.nalvis
    "#{MetaData.t3k}|nalvis"
  end

end
