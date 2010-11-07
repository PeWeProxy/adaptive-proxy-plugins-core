require 'rubygems'

begin
  require 'active_record'
rescue LoadError
  warn 'You need active_record gem'
  warn '$ sudo gem install active_record'
  exit(1)
end

require 'filter/string_additions'
require 'filter/pattern_matcher'
require 'filter/model/access_log'
require 'filter/cleandb'
require 'filter/filter_generator'
