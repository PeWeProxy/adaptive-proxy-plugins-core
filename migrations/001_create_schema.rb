class CreateSchema < ActiveRecord::Migration
  def self.up
    create_table :access_logs do |t|
      t.string :userid, :limit => 32
      t.timestamp :timestamp
      t.string :url, :limit => 4000
      t.string :keywords, :limit => 1000
    end

    add_index :access_logs, [:userid, :timestamp]

    create_table :keyword_cache do |t|
      t.string :url, :limit => 1000
      t.string :checksum, :limit => 32
      t.string :keywords, :limit => 1000
    end

    add_index :keyword_cache, :checksum
  end

  def self.down
    drop_table :access_logs
    drop_table :keyword_cache
  end

end
