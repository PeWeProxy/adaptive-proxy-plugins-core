class AddIndexes < ActiveRecord::Migration
  def self.up
    add_index :access_logs, :page_id
    add_index :pages, [:url, :checksum]
  end

  def self.down
    remove_index :access_logs, :page_id
    remove_index :pages, [:url, :checksum]
  end
end
