This is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM), and Web (Wasm).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/webApp](./webApp) is the Compose Multiplatform Wasm/JS browser entry (Kotlin/Wasm).

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.
- Web (local): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- Web (static bundle): `./gradlew :webApp:wasmJsBrowserDistribution`
  - Output: `webApp/build/dist/wasmJs/productionExecutable/`

### Web deployment notes

- Serve the distribution directory as static files. Set MIME `application/wasm` for `.wasm`.
- Required response headers (Compose Wasm / SharedArrayBuffer):
  - `Cross-Origin-Opener-Policy: same-origin`
  - `Cross-Origin-Embedder-Policy: require-corp`
- Current Web MVP keeps data in memory (refresh clears profiles/images). OPFS persistence is not enabled yet.
- For mainland China reachability, prefer Tencent COS / Alibaba OSS + domestic CDN (or similar). GitHub Pages is often slow or blocked. Custom domains in mainland China require ICP filing.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- Desktop tests: `./gradlew :shared:jvmTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
