class DefaultValueOfFeedbackIndicators < ActiveRecord::Migration
  def self.up
    change_column :access_logs, :time_on_page, :integer, :default => 0
    change_column :access_logs, :scroll_count, :integer, :default => 0
    change_column :access_logs, :copy_count, :integer, :default => 0
  end

  def self.down
    change_column :access_logs, :time_on_page, :integer, :default => nil
    change_column :access_logs, :scroll_count, :integer, :default => nil
    change_column :access_logs, :copy_count, :integer, :default => nil
  end
end
