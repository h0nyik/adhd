import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // JVM: backend a testy. Android aplikace knihovnu spotřebuje jako JVM artefakt,
    // androidTarget() přibude spolu s modulem apps/android.
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    // iOS a macOS: sestaví se jen na macOS (Xcode), na Linuxu se přeskočí.
    // ./gradlew :shared:core:assembleUlovkoCoreXCFramework → shared/core/build/XCFrameworks/
    val xcframework = XCFramework("UlovkoCore")
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach {
        it.binaries.framework {
            baseName = "UlovkoCore"
            xcframework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.serialization.json)
            api(libs.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
