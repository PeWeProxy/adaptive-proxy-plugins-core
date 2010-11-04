require File.expand_path(File.dirname(__FILE__) + "/test_helper")

class StringAdditionsTest < Test::Unit::TestCase
  def test_starts_with
    assert "unittest".starts_with? "unit"
    assert !("unittest".starts_with? "test")
    assert "unittest".starts_with? ["test", "unit"]
    assert !("unittest".starts_with? ["test", "best"])
  end

  def test_ends_with
    assert "unittest".ends_with? "test"
    assert !("unittest".ends_with? "unit")
    assert "unittest".ends_with? ["test", "unit"]
    assert !("unittest".ends_with? ["unit", "best"])
  end

  def test_contains
    assert "unittest".contains? "itte"
    assert !("unittest".contains? "best")
    assert "unittest".contains? ["nit", "tes"]
    assert !("unittest".contains? ["best", "git"])
  end
end
