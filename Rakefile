require 'rubygems'
require 'fileutils'
require 'jake'

PROXY_DIR = 'adaptive-proxy'


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
  FileUtils.rm_rf Dir.glob('bin/*')
end

desc "Create proxy plugin bundle jar file"
task :jar do
  Jar.in('.').execute do |jar|
    jar.name = File.dirname(__FILE__).match(/[^\/]+$/)[0] + '.jar'
    jar.bin << 'bin'
  end

#    FileUtils.cp("#{PLUGINS_DIR}/proxy-plugins.jar", "#{PROXY_DIR}/libs")
#    FileUtils.cp_r(Dir.glob("#{PLUGINS_DIR}/external_libs/*"), "#{PROXY_DIR}/libs")
end

desc "Move (and overwrite) plugin configuration to PROXY_DIR"
task :configuration do
  FileUtils.cp_r(Dir.glob("plugins/*.xml"), "../../#{PROXY_DIR}/plugins")
end



namespace :offline do
  task :build do
    # build the offline jobs and move them to offline/build folder
  end
end
