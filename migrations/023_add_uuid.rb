class AddUUID < ActiveRecord::Migration
  def self.up
    change_column :access_logs, :id, :string, :limit => 40
  end

  def self.down
    change_column :access_logs, :id, :int
  end
end
