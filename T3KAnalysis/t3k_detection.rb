class T3kDetection
  protected def initialize(type)
    super()

    @type = type
    @info = nil
  end

  public

  attr_reader :type
  attr_accessor :info

  def to_s
    instance_variables.collect{|var| "#{var}=#{instance_variable_get var}"}.join", "
  end

end

class T3KPersonDetection < T3kDetection
  def initialize
    super "person"

    @age = nil
    @gender = nil
    @symbol = nil
    @data = nil
    @score = nil
    @box = nil
  end

  attr_accessor :age, :gender, :symbol, :data, :score, :box
end

class T3KObjectDetection < T3kDetection
  def initialize
    super "object"

    @classification = nil
    @data = nil
    @score = nil
    @box = nil
  end

  attr_accessor :classification, :data, :score, :box
end

class T3KMd5Detection < T3kDetection
  def initialize
    super "md5"

    @hit_type = nil
    @hit_hash = nil
    @description = nil
    @metadata = nil
    @hit_id = nil
  end

  attr_accessor :hit_type, :hit_id, :hit_hash, :description, :metadata
end

class T3KTextDetection < T3kDetection
  def initialize
    super "text"

    @string = nil
    @description = nil
    @language = nil
    @regex = false
    @fuzzy = false
    @mlr = nil
    @matches = nil
  end

  attr_accessor :string, :description, :language, :regex, :fuzzy, :mlr, :matches
end

class DetectionData
  def initialize(type)
    super()

    @type = type
  end

  attr_reader :type

  def to_s
    instance_variables.collect{|var| "#{var}=#{instance_variable_get var}"}.join", "
  end
end

class VideoDetectionData < DetectionData
  def initialize
    super "video"

    @frame = nil
  end

  attr_accessor :frame
end

class DocumentDetectionData < DetectionData
  def initialize
    super "document"

    @page_number = nil
    @image_number = nil
  end

  attr_accessor :image_number, :page_number
end