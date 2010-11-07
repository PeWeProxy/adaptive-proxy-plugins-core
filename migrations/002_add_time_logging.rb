class AddTimeLogging < ActiveRecord::Migration
  def self.up
    add_column :access_logs, :checksum, :string, :limit => 32
    add_column :access_logs, :time_on_page, :integer
  end

  def self.down
    remove_column :access_logs, :time_on_page
    remove_column :access_logs, :checksum
  end
end
