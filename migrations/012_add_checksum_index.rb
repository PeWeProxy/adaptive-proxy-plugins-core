class AddChecksumIndex < ActiveRecord::Migration
  def self.up
    add_index :pages, :checksum
  end

  def self.down
    remove_index :pages, :checksum
  end
end
