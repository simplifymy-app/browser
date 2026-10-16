APP_ID   := app.simplifymy.browser
SDK      ?= $(HOME)/Library/Android/sdk
ADB      := $(SDK)/platform-tools/adb
EMULATOR := $(SDK)/emulator/emulator
VERSION  := $(shell sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
DIST     := dist
ABI      ?= arm64-v8a
APK      := $(DIST)/simplify-browser-$(VERSION)-$(ABI).apk
AAB      := $(DIST)/simplify-browser-$(VERSION).aab
DEBUG_APK = app/build/outputs/apk/debug/app-$$($(ADB) shell getprop ro.product.cpu.abi | tr -d '\r')-debug.apk

.PHONY: help
help: #shows what make commands are available
	@grep '^\S\S*: .*#' $(MAKEFILE_LIST) | sed 's/:.*#/: #/' | column -s '#' -t | sort

.PHONY: run
run: install #installs the debug build on a device or emulator and launches it
	@$(ADB) shell monkey -p $(APP_ID).debug -c android.intent.category.LAUNCHER 1 >/dev/null
	@echo "launched $(APP_ID).debug"

.PHONY: install
install: #streams the debug apk for the device abi onto the attached device
	./gradlew :app:assembleDebug
	$(ADB) install -r $(DEBUG_APK)

.PHONY: debug
debug: #builds the debug apk
	./gradlew :app:assembleDebug

.PHONY: release
release: #builds the minified release apk
	./gradlew :app:assembleRelease

.PHONY: apk
apk: #builds the release apk for one abi into dist, make apk ABI=x86_64
	./gradlew :app:assembleRelease
	@mkdir -p $(DIST)
	@cp app/build/outputs/apk/release/app-$(ABI)-release.apk $(APK)
	@$$(ls $(SDK)/build-tools/*/apksigner | sort -V | tail -1) verify $(APK) && echo "signature ok"
	@ls -lh $(APK)

.PHONY: apk-debug
apk-debug: #builds the debug apk for one abi into dist, make apk-debug ABI=x86_64
	./gradlew :app:assembleDebug
	@mkdir -p $(DIST)
	@cp app/build/outputs/apk/debug/app-$(ABI)-debug.apk $(DIST)/simplify-browser-$(VERSION)-$(ABI)-debug.apk
	@ls -lh $(DIST)/simplify-browser-$(VERSION)-$(ABI)-debug.apk

.PHONY: bundle
bundle: #builds the signed app bundle for google play into dist
	./gradlew :app:bundleRelease
	@mkdir -p $(DIST)
	@cp app/build/outputs/bundle/release/app-release.aab $(AAB)
	@ls -lh $(AAB)

.PHONY: emulator
emulator: #boots the first available avd and waits for it
	@$(EMULATOR) -avd $$($(EMULATOR) -list-avds | head -1) >/dev/null 2>&1 &
	@$(ADB) wait-for-device
	@until [ "$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
	@echo "emulator ready"

.PHONY: logcat
logcat: #follows the app log output
	@$(ADB) logcat --pid=$$($(ADB) shell pidof $(APP_ID).debug)

.PHONY: screenshot
screenshot: #saves a screenshot of the device to screenshot.png
	@$(ADB) exec-out screencap -p > screenshot.png && echo "wrote screenshot.png"

.PHONY: lint
lint: #runs android lint over the debug variant
	./gradlew :app:lintDebug

.PHONY: version
version: #prints the current version name
	@echo $(VERSION)

.PHONY: bump-patch
bump-patch: #bumps the patch version and the version code, 1.0.0 to 1.0.1
	@sh scripts/bump.sh patch

.PHONY: bump-minor
bump-minor: #bumps the minor version and the version code, 1.0.3 to 1.1.0
	@sh scripts/bump.sh minor

.PHONY: bump-major
bump-major: #bumps the major version and the version code, 1.2.3 to 2.0.0
	@sh scripts/bump.sh major

.PHONY: set-version
set-version: #sets an exact version and bumps the code, make set-version VERSION=1.4.0
	@sh scripts/bump.sh $(VERSION)

.PHONY: clean
clean: #removes build output and the dist directory
	./gradlew clean
	@rm -rf $(DIST)
