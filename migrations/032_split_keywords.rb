class SplitKeywords < ActiveRecord::Migration
  def self.up
    create_table :terms do |t|
      t.string :label
      t.string :term_type
    end

    create_table :pages_terms do |t|
	  t.references :page
	  t.references :term
	  t.float :weight
	  t.boolean :active
	  t.timestamps
    end

    add_index(:terms, [:label, :term_type], :unique => true)

  end

  def self.down
    drop_table :pages_terms
    drop_table :terms
  end
end

