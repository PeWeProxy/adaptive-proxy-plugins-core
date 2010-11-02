class AddPages < ActiveRecord::Migration
  def self.up
    create_table :pages do |t|
      t.string :url, :limit => 4000
      t.string :checksum, :limit => 32
      t.integer :content_length
      t.string :keywords, :limit => 4000
    end

    remove_column :access_logs, :url
    remove_column :access_logs, :keywords
    remove_column :access_logs, :checksum

    add_column :access_logs, :page_id, :integer
  end

  def self.down
    drop_table :pages
    add_column :access_logs, :url, :string, :limit => 4000
    add_column :access_logs, :keywords, :string, :limit => 1000
    add_column :access_logs, :checksum, :string, :limit => 32
    remove_column :access_logs, :page_id
  end
end
