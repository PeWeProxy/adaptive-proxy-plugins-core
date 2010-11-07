class CleanDb

  attr_accessor :matcher

  def initialize(config = 'database.yml')
    conf = YAML::load(File.open(config))
    ActiveRecord::Base.establish_connection(conf)

    @matcher = PatternMatcher.new
  end


  def clean(method = :dry_run)
    deleted = 0

    AccessLog.all.each do |log|
      if @matcher.matches(log.url) then
        puts "Deleting #{log.url}"
        log.delete unless method == :dry_run
        deleted += 1

        return if deleted  == 10
      end
    end

    puts "Deleted #{deleted} records from database"
  end

end
