class CreateStoredApuids < ActiveRecord::Migration
  def self.up
    create_table :stored_apuids do |t|
      t.string :identifier
      t.string :uid
      t.timestamp :valid_until
    end
  end

  def self.down
    drop_table :stored_apuids
  end
end