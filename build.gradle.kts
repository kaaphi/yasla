buildscript {
    extra.apply{
        set("compose_version", "1.3.2")
    }
}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
    id("pl.allegro.tech.build.axion-release") version "1.14.3"
}

val pl.allegro.tech.build.axion.release.domain.VersionConfig.versionCode : Int
    get() {
        val version = com.github.zafarkhaja.semver.Version.valueOf(undecoratedVersion)
        check(version.majorVersion < 210) { "Major version cannot be 210 or larger : $version!" }
        check(version.minorVersion < 1000) { "Minor version must be less than 1000: $version!" }
        check(version.patchVersion < 10000) { "Patch version must be less than 10000: $version!"}

        //format will be: MMMmmmpppp
        val versionCode = version.majorVersion * 10000000 + version.minorVersion * 10000 + version.patchVersion
        check(versionCode <= 2100000000) { "Version code from $version exceeds 2100000000: $versionCode!" }
        return versionCode
    }

allprojects {
    version = rootProject.scmVersion.version
    extensions.add("versionCode", rootProject.scmVersion.versionCode)
}
