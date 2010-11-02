require 'rubygems'
require 'fileutils'
require 'jake'
require 'whenever'
require 'active_record'
require 'active_support'
require 'yaml'
require 'rake'
require 'logger'

PROXY_DIR = 'adaptive-proxy'

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
# we need to know what directories contain java application and what are the main classes
java_apps = {
	'test' => 'sk.fiit.plesko.test.MainClass'
}

desc "Compile offline tasks"
  task :build do
  
  	FileUtils.mkdir('offline/build') unless File.exists?("offline/build")
  	java_apps.each_pair do |app_name, mainclass|
  
  		FileUtils.rm_rf Dir.glob('offline/'+app_name+'/bin/')
  		FileUtils.rm_rf Dir.glob('offline/'+app_name+'/META-INF')
  		FileUtils.rm_rf Dir.glob('offline/'+app_name+'/*.jar')
  		
  		Javac.in('offline/'+app_name).execute do |javac|
  			javac.src = 'src/**/*.java'
  			javac.cp << '.'
  			javac.output = 'bin'
  		end
  
  		Manifest.in('offline/'+app_name).execute do |manifest|
  			manifest.main_class = mainclass
  			manifest.cp = ''
  		end
  
  		Jar.in('offline/'+app_name).execute do |jar|
  			jar.name = 'offline/'+app_name+'.jar'
  			jar.bin << 'bin'
  			jar.with_manifest = true
  		end
  
  		FileUtils.mv('offline/'+app_name+'/'+app_name+'.jar', 'offline/build/'+app_name+'.jar')
  		
  	end
  end
  
  desc "Schedule tasks as defined in config/schedule.rb"
  task :schedule do
  	Whenever::CommandLine.execute({:update=>true})
  end
end

namespace :migrations
# we need to make migrations for database
desc "Migrate the database through scripts in this folder. Target specific version with VERSION=x"
   task :migrate => :environment do
     ActiveRecord::Migrator.migrate('migrations', ENV["VERSION"] ? ENV["VERSION"].to_i : nil )
   end
   
   task :rollback => :environment do
     ActiveRecord::Migrator.rollback('migrations')
   end
   
   task :environment do
     ActiveRecord::Base.establish_connection(YAML::load(File.open('database.yml'))[ENV["RAILS_ENV"] ? ENV["RAILS_ENV"] : "development"])
     ActiveRecord::Base.logger = Logger.new(File.open('database.log', 'a'))
   end

namespace :after do
  desc "Run task after deploy"
  task :after_deploy do
    # Add code here to run after deploy
  end
end

task :default => [":src:build", "migrations:migrate", "offline:build", "after:after_deploy"]