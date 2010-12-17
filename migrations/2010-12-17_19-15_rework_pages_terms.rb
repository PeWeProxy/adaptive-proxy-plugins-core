class ReworkPagesTerms < ActiveRecord::Migration
  def self.up
    change_column :pages_terms, :page_id, :string, :limit => 40
  end

  def self.down
    change_column :pages_terms, :page_id, :integer
  end
end

