class T3kResult
  protected def initialize(type)
    super()

    @id = nil
    @path = nil
    @detections = []
    @type = type
  end

  public

  attr_reader :type
  attr_accessor :id, :path

  def detection(index)
    @detections[index]
  end

  def add_detection(detection)
    @detections << detection
  end

  def each_detection
    @detections.each { | detection | yield detection }
  end

  def to_s
    instance_variables.collect{|var| "#{var}=#{instance_variable_get var}"}.join", "
  end
end

class T3KImageResult < T3kResult

  def initialize
    super("image")

    @md5 = nil
    @sha1 = nil
    @mode = nil
    @width = nil
    @height = nil
    @size = nil
    @bytes = nil
    @photo_dna = nil
    @nalvis = nil
  end

  attr_accessor :md5, :sha1, :mode, :width, :height, :size, :bytes, :photo_dna, :nalvis
end

class T3KVideoResult < T3kResult
  def initialize
    super("video")

    @frame_count = nil
    @width = nil
    @height = nil
    @fps = nil
    @keyframes = nil
    @keyframe_search = nil
  end

  attr_accessor :frame_count, :width, :height, :fps, :keyframes, :keyframe_search
end

class T3KDocumentResult < T3kResult
  def initialize
    super("document")

    @page_count = nil
    @image_count = nil
    @text = false
    @images = nil
    @md5 = nil
    @sha1 = nil
    @doc_type = nil
  end

  attr_accessor :page_count, :image_count, :text, :images, :md5, :sha1, :doc_type
end