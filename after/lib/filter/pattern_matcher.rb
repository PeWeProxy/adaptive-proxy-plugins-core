class PatternMatcher
  def initialize(patterns = nil)
    @start_patterns = []
    @end_patterns = []
    @simple_filters = []
    @match_filters = []

    patterns = load_patterns_from_file if patterns.nil?

    patterns.each do |pattern|
      pattern.chomp!
      if pattern.starts_with? '|' then
        @start_patterns << pattern[1, pattern.length]
      elsif pattern.ends_with? '|'
        @end_patterns << pattern[0, pattern.length - 1]
      elsif pattern.contains? '*' then
        @match_filters << pattern
      else
        @simple_filters << pattern
      end
    end

  end

  def matches(s)
    return true if @simple_filters.detect { |p| s.contains? p }
    return true if @start_patterns.detect { |p| s.starts_with? p }
    return true if @end_patterns.detect { |p| s.ends_with? p }
    return true if @match_filters.detect { |p| matches_wildcard(s, p) }
    false
  end

  private

  def matches_wildcard(string, pattern)
    pattern.split(/\*/).each do |card|
      idx = string.index(card)
      return false if idx.nil?
      string = string[idx, string.length]
    end

    true
  end

  def load_patterns_from_file
    patterns = []
    File.open('filter.txt', 'r') do |f|
      f.readlines.each do |pattern|
        patterns << pattern
      end
    end

    patterns
  end

end
