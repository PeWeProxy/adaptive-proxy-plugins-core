require File.expand_path(File.dirname(__FILE__) + "/test_helper")

class CleanDbTest < Test::Unit::TestCase

  class FakeLog
    attr_accessor :log, :url
  end

  def setup
    fake_log_1 = FakeLog.new
    fake_log_1.url = "http://www.adserver.com/ad?id=234234"
    fake_log_1.stubs(:delete)
    fake_log_1.expects(:delete)

    fake_log_2 = FakeLog.new
    fake_log_2.url = "http://www.google.com/images/test.jpg"
    fake_log_2.stubs(:delete)
    fake_log_2.expects(:delete)

    AccessLog.stubs(:all).returns([fake_log_1, fake_log_2])
  end

  def test_clean
    assert_nothing_thrown do
      matcher = PatternMatcher.new(['ad', 'jpg'])

      cleandb = CleanDb.new
      cleandb.matcher = matcher

      cleandb.clean(:go)
    end
  end
end
