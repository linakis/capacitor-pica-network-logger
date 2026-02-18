Pod::Spec.new do |spec|
    spec.name                     = 'PicaNetworkLoggerShared'
    spec.version                  = '0.1.0'
    spec.homepage                 = 'https://github.com/linakis/capacitor-pica-network-logger'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Capacitor HTTP inspector shared UI'
    spec.vendored_frameworks      = 'build/cocoapods/framework/PicaNetworkLoggerShared.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target = '14.0'
                
                
    if !Dir.exist?('build/cocoapods/framework/PicaNetworkLoggerShared.framework') || Dir.empty?('build/cocoapods/framework/PicaNetworkLoggerShared.framework')
        raise "

        Kotlin framework 'PicaNetworkLoggerShared' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :shared:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':shared',
        'PRODUCT_MODULE_NAME' => 'PicaNetworkLoggerShared',
    }
                
    spec.script_phases = [
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
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
    spec.resources = ['build/compose/cocoapods/compose-resources']
end