class AddSourceToPagesTerms < ActiveRecord::Migration
  def self.up
    add_column :pages_terms, :source, :string, :limit => 20
  end

  def self.down
    remove_column :pages_terms, :source
  end
end

