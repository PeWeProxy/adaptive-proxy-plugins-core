class CreateEmail < ActiveRecord::Migration
  def self.up
    create_table :emails do |t|
      t.string :email
    end
  end

  def self.down
    drop_table :emails
  end
end

