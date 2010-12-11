every 1.minute do
  command "ruby #{options[:deploy_path]}/scripts/myjob.rb"
end
