Pod::Spec.new do |s|
  s.name = 'CapacitorPicaNetworkLogger'
  s.version = '0.1.1'
  s.summary = 'Capacitor HTTP inspector'
  s.license = 'MIT'
  s.author = { 'Nikos Linakis' => 'nikos@linakis.net' }
  s.homepage = 'https://github.com/linakis/capacitor-pica-network-logger'
  s.source = { :path => '.' }
  s.source_files = 'ios/Plugin/**/*.{swift,h,m}'
  s.resources = ['ios/Plugin/PluginManifest.json']
  s.dependency 'Capacitor'
  s.ios.deployment_target = '14.0'

  # KMP shared framework (Compose Multiplatform inspector UI)
  s.vendored_frameworks = 'kmp/shared/build/cocoapods/framework/PicaNetworkLoggerShared.framework'
  s.libraries = 'c++'

  s.pod_target_xcconfig = {
    'KOTLIN_PROJECT_PATH' => ':shared',
  }

  s.script_phases = [
    {
      :name => 'Build PicaNetworkLoggerShared',
      :execution_position => :before_compile,
      :shell_path => '/bin/sh',
      :script => <<-SCRIPT
        if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
          echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
          exit 0
        fi
        set -ev
        REPO_ROOT="$PODS_TARGET_SRCROOT"
        "$REPO_ROOT/kmp/gradlew" -p "$REPO_ROOT/kmp" $KOTLIN_PROJECT_PATH:syncFramework \
            -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
            -Pkotlin.native.cocoapods.archs="$ARCHS" \
            -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
      SCRIPT
    }
  ]
end
