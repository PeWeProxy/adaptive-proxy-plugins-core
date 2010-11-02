class AddReferer < ActiveRecord::Migration
  def self.up
    add_column :access_logs, :referer, :string
  end

  def self.down
    remove_column :access_logs, :referer
  end
end
