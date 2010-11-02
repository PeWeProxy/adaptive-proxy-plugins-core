class AddScrollCountAndCopyCount < ActiveRecord::Migration
  def self.up
    add_column :access_logs, :scroll_count, :integer
    add_column :access_logs, :copy_count, :integer
  end

  def self.down
    remove_column :scroll_count
    remove_column :copy_count
  end
end