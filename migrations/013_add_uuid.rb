class AddUUID < ActiveRecord::Migration
  def self.up
    add_column :access_logs, :uuid, :string, :limit => 40
  end

  def self.down
    remove_column :access_logs, :uuid
  end
end
