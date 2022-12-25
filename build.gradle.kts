@Suppress("DSL_SCOPE_VIOLATION")  // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.axion.release)
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
