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

includeAt(":snb-content:snb-content-domain", "snb-content/snb-content-domain")
includeAt(":snb-content:snb-content-app", "snb-content/snb-content-app")
includeAt(":snb-content:snb-content-infra", "snb-content/snb-content-infra")
includeAt(":snb-content:snb-content-adapter", "snb-content/snb-content-adapter")
includeAt(":snb-content:snb-content-api", "snb-content/snb-content-api")
project(":snb-content").projectDir = file("snb-content")

includeAt(":snb-invoice:snb-invoice-domain", "snb-invoice/snb-invoice-domain")
includeAt(":snb-invoice:snb-invoice-app", "snb-invoice/snb-invoice-app")
includeAt(":snb-invoice:snb-invoice-infra", "snb-invoice/snb-invoice-infra")
includeAt(":snb-invoice:snb-invoice-adapter", "snb-invoice/snb-invoice-adapter")
includeAt(":snb-invoice:snb-invoice-api", "snb-invoice/snb-invoice-api")
project(":snb-invoice").projectDir = file("snb-invoice")

includeAt(":snb-guide:snb-guide-domain", "snb-guide/snb-guide-domain")
includeAt(":snb-guide:snb-guide-app", "snb-guide/snb-guide-app")
includeAt(":snb-guide:snb-guide-infra", "snb-guide/snb-guide-infra")
includeAt(":snb-guide:snb-guide-adapter", "snb-guide/snb-guide-adapter")
includeAt(":snb-guide:snb-guide-api", "snb-guide/snb-guide-api")
project(":snb-guide").projectDir = file("snb-guide")

includeAt(":snb-boot", "snb-boot")
