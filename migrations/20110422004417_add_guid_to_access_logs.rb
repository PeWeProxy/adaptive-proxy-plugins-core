class AddGuidToAccessLogs < ActiveRecord::Migration
  add_column :access_logs, :guid, :string, :limit => 40
  add_index :access_logs, :guid, :unique => true
end
