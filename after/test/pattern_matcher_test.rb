require File.expand_path(File.dirname(__FILE__) + "/test_helper")

class PatternMatcherTest < Test::Unit::TestCase
  def setup
    @matcher = PatternMatcher.new(['|ad', 'end|', 'sme', 'match*th*is'])
  end

  def test_start_match
    assert @matcher.matches('adserver.ads.com')
  end

  def test_end_match
    assert @matcher.matches('this must end')
  end

  def test_simple_match
    assert @matcher.matches('http://www.sme.sk')
  end

  def test_wildcard_match
    assert @matcher.matches('matcher matches all of this string')
  end

  def test_good
    assert !@matcher.matches('www.pravda.sk')
    assert !@matcher.matches('this ad is not end ad')
  end
end
