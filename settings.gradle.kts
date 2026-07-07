rootProject.name = "snb-platform"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal() // linqibin-commons 产物在此(见 scripts/bootstrap-commons.sh)
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
    }
    repositoriesMode = RepositoriesMode.PREFER_PROJECT
}

fun includeAt(path: String, dir: String) {
    include(path)
    project(path).projectDir = file(dir)
}

includeAt(":snb-common", "snb-common")
includeAt(":snb-sub2api", "snb-sub2api")

includeAt(":snb-activity:snb-activity-domain", "snb-activity/snb-activity-domain")
includeAt(":snb-activity:snb-activity-app", "snb-activity/snb-activity-app")
includeAt(":snb-activity:snb-activity-infra", "snb-activity/snb-activity-infra")
includeAt(":snb-activity:snb-activity-adapter", "snb-activity/snb-activity-adapter")
includeAt(":snb-activity:snb-activity-api", "snb-activity/snb-activity-api")
project(":snb-activity").projectDir = file("snb-activity")

includeAt(":snb-gallery:snb-gallery-domain", "snb-gallery/snb-gallery-domain")
includeAt(":snb-gallery:snb-gallery-app", "snb-gallery/snb-gallery-app")
includeAt(":snb-gallery:snb-gallery-infra", "snb-gallery/snb-gallery-infra")
includeAt(":snb-gallery:snb-gallery-adapter", "snb-gallery/snb-gallery-adapter")
includeAt(":snb-gallery:snb-gallery-api", "snb-gallery/snb-gallery-api")
project(":snb-gallery").projectDir = file("snb-gallery")

includeAt(":snb-boot", "snb-boot")
