require File.expand_path(File.dirname(__FILE__) + "/test_helper")

class FilterGeneratorTest < Test::Unit::TestCase

  TEST_FILTERS = 'test_filters.txt'

  def setup
    File.delete(TEST_FILTERS) if File.exist? TEST_FILTERS
  end

  def test_generate
    fg = FilterGenerator.new('patterns', TEST_FILTERS)

    assert_nothing_raised do
      fg.generate
    end
    assert File.exist? TEST_FILTERS
  end
end
