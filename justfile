gradle := "./gradlew"
app := ":composeApp"

# List available recipes
default:
    @just --list

# Build Android debug APK
android-debug:
    {{gradle}} {{app}}:assembleDebug
    @echo "APK: composeApp/build/outputs/apk/debug/"

# Build and install Android debug APK on a connected device
android-install:
    {{gradle}} {{app}}:installDebug

# Build Desktop RPM package
desktop-rpm:
    {{gradle}} {{app}}:packageRpm
    @echo "RPM: composeApp/build/compose/binaries/main/rpm/"

# Build Desktop RPM package (release, ProGuard)
desktop-rpm-release:
    {{gradle}} {{app}}:packageReleaseRpm
    @echo "RPM: composeApp/build/compose/binaries/main-release/rpm/"

# Build Android debug APK + Desktop RPM
build: android-debug desktop-rpm

# Run Desktop with Compose Hot Reload (async, returns immediately)
dev-desktop:
    {{gradle}} {{app}}:hotRunJvmAsync

# Run Desktop with Compose Hot Reload (foreground)
dev-desktop-fg:
    {{gradle}} {{app}}:hotRunJvm

# Trigger hot reload for running desktop apps
reload:
    {{gradle}} {{app}}:reload

# Run Web (JS) dev server with continuous rebuild
dev-web:
    {{gradle}} {{app}}:jsBrowserDevelopmentRun --continuous

# Clean build outputs
clean:
    {{gradle}} clean
