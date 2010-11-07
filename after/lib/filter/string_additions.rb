class String
  def starts_with?(prefixes)
    with_array prefixes do |prefix|
      prefix = prefix.to_s
      return true if self[0, prefix.length] == prefix
    end
    false
  end

  def ends_with?(suffixes)
    with_array suffixes do |suffix|
      suffix = suffix.to_s
      return true if self[-suffix.length, suffix.length] == suffix
    end
    false
  end

  def contains?(characters)
    with_array characters do |character|
      character = character.to_s
      return true unless self.index(character).nil?
    end
    false
  end

  private

  def with_array(array, &block)
    array = Array.new(1, array) unless array.kind_of? Array

    array.each do |elem|
      yield elem
    end

    false
  end
end
