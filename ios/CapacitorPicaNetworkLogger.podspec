Pod::Spec.new do |s|
  s.name = 'CapacitorPicaNetworkLogger'
  s.version = '0.1.0'
  s.summary = 'Capacitor HTTP inspector'
  s.license = 'MIT'
  s.author = { 'Nikos Linakis' => 'nikos@linakis.net' }
  s.homepage = 'https://github.com/linakis/capacitor-pica-network-logger'
  s.source = { :path => '.' }
  s.source_files = 'Plugin/**/*.{swift,h,m}'
  s.dependency 'Capacitor'
end
