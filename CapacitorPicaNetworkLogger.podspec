Pod::Spec.new do |s|
  s.name = 'CapacitorPicaNetworkLogger'
  s.version = '0.2.2'
  s.summary = 'Capacitor HTTP inspector'
  s.license = 'MIT'
  s.author = { 'Nikos Linakis' => 'nikos@linakis.net' }
  s.homepage = 'https://github.com/linakis/capacitor-pica-network-logger'
  s.source = { :path => '.' }
  s.source_files = 'ios/Plugin/**/*.{swift,h,m}'
  s.resources = ['ios/Plugin/PluginManifest.json']
  s.dependency 'Capacitor'
  s.ios.deployment_target = '14.0'

end
