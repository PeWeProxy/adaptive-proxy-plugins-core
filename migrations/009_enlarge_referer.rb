class EnlargeReferer < ActiveRecord::Migration
  def self.up
    execute "ALTER TABLE access_logs CHANGE referer referer VARCHAR(4000)"
  end

  def self.down
    change_column :access_logs, :referer, :precision => 255
  end
end
