# see http://adblockplus.org/en/filters for the adblock filter rules description

require 'open-uri'
require 'set'

class FilterGenerator

  FILTER_PREFIX = ['!', '[', '@', '#']
  FILTER_BODY = ['$', '#']

  def initialize(input = 'patterns', output = 'filter.txt')
    @output = output

    @sources = Dir.glob("#{input}/*.txt").reject{|f| f =~ /.*\<online\.txt/}
    File.open(File.join(input, 'online.txt'), 'r') do |f|
      f.readlines.each {|l| @sources << l}
    end
  end

  def generate
    uniq = Set.new

    @sources.each do |source|
      open(source) do |f|
        f.each do |line|
          convert_line(line).each { |l| uniq << l }
        end
      end
    end

    write uniq
  end

  private

  def convert_line(line)
    line.chomp!
    line = line[1, line.length] if line.starts_with? '*'
    line = line[0, line.length - 1] if line.ends_with? '*'

    if line.starts_with? '||' then
      line = line[2, line.length]
      lines = ['http://' + line, 'https://' + line, 
            'http://www.' + line, 'https://www.' + line]
    else
      lines = [line]
    end

    lines.collect do |line|
      line unless line.nil? or line.starts_with? FILTER_PREFIX or line.contains? FILTER_BODY
    end
  end

  def write(list)
    File.open(@output, 'w') do |f|
      list.each do |line|
        f.puts line
      end
    end
  end
end
