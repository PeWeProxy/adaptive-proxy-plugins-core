class ReworkIdForPages < ActiveRecord::Migration
  def self.up
    change_column :access_logs, :page_id, :string, :limit => 40
    change_column :pages, :id, :string, :limit => 40
  end

  def self.down
    change_column :access_logs, :page_id, :integer
    change_column :pages, :id, :integer
  end
end

