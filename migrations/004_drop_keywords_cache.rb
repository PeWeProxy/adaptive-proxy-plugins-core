class DropKeywordsCache < ActiveRecord::Migration
  def self.up
    drop_table :keyword_cache
  end

  def self.down
  end
end
