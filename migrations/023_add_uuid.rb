class AddUuid < ActiveRecord::Migration
  def self.up
    change_column :access_logs, :id, :string, :limit => 40
  end

  def self.down
    change_column :access_logs, :id, :integer
  end
end
