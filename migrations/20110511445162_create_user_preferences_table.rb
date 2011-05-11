class CreateUserPreferencesTable < ActiveRecord::Migration
  create_table "user_preferences", :force => true do |t|
    t.integer  "ID",                :limit => 32
    t.string   "preference_name",   :limit => 50
    t.string   "user",              :limit => 32
    t.string   "preference_value",  :limit => 40
  end
end
