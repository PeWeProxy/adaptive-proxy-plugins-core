class AddIpAddress < ActiveRecord::Migration
  def self.up
    add_column :access_logs, :ip, :string, :limit => 50
  end

  def self.down
    remove_column :access_logs, :ip
  end
end
