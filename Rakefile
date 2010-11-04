require 'rubygems'
require 'fileutils'
require 'jake'
require 'active_record'
require 'active_support'
require 'whenever'
require 'yaml'
require 'rake'
require 'logger'

PROXY_DIR = 'adaptive-proxy'

# java offline tasks
# directory (project name) => main class (including package)
java_apps = {
  #'test' => 'sk.fiit.plesko.pokus.Test'
}

namespace :src do
  desc "Compile proxy plugin bundle"
  task :build => :clean do
    Javac.in('.').execute do |javac|
      javac.src = 'src/**/*.java'
      javac.cp << 'external_libs/**/*.jar'
      javac.cp << "../../#{PROXY_DIR}/bin"
      javac.output = 'bin'
    end
  end

  desc "Clean proxy plugin bundle build"
  task :clean do
    FileUtils.mkdir('bin') unless File.exists?("bin")
    FileUtils.rm_rf Dir.glob('bin/*')
  end

  desc "Create proxy plugin bundle jar file"
  task :jar do
    Jar.in('.').execute do |jar|
      jar.name = File.dirname(__FILE__).match(/[^\/]+$/)[0] + '.jar'
      jar.bin << 'bin'
    end
  end
end


namespace :offline do

  desc "Compile offline tasks"
  task :build do
		Dir.chdir("offline")
  	FileUtils.mkdir('jars') unless File.exists?("jars")
  	java_apps.each_pair do |app_name, mainclass|

  		FileUtils.rm_rf Dir.glob(app_name+'/bin/')
  		FileUtils.rm_rf Dir.glob(app_name+'/META-INF')
  		FileUtils.rm_rf Dir.glob(app_name+'/*.jar')

  		Javac.in(app_name).execute do |javac|
  			javac.src = 'src/**/*.java'
  			javac.cp << '.'
  			javac.output = 'bin'
  		end

  		Manifest.in(app_name).execute do |manifest|
  			manifest.main_class = mainclass
  			manifest.cp = '.'
  		end

  		Jar.in(app_name).execute do |jar|
  			jar.name = app_name+'.jar'
  			jar.bin << 'bin'
  			jar.with_manifest = true
  		end

  		FileUtils.mv(app_name+'/'+app_name+'.jar', 'jars/'+app_name+'.jar')

  	end
		Dir.chdir("..")
  end

  desc "Schedule tasks as defined in config/schedule.rb"
  task :schedule do
		Dir.chdir("offline")
		Whenever::CommandLine.execute({:update=>true})
		Dir.chdir("..")
  end
end

namespace :migrations do
  # we need to make migrations for database
  desc "Migrate the database through scripts in this folder. Target specific version with VERSION=x"
  task :migrate => :environment do
    ActiveRecord::Migrator.migrate('migrations', ENV["VERSION"] ? ENV["VERSION"].to_i : nil )
  end

  task :rollback => :environment do
    ActiveRecord::Migrator.rollback('migrations')
  end

  task :environment do
    ActiveRecord::Base.establish_connection(YAML::load(File.open('migrations/database.yml'))[ENV["RAILS_ENV"] ? ENV["RAILS_ENV"] : "development"])
    ActiveRecord::Base.logger = Logger.new(File.open('migrations/database.log', 'a'))
  end
end

namespace :after do
  desc "Run task after deploy"
  task :after_deploy do
    # Add code here to run after deploy
  end
end

task :default => [":src:build", ":src:jar", "migrations:migrate", "offline:build", "offline:schedule", "after:after_deploy"]
