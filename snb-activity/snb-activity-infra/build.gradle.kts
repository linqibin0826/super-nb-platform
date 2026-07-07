plugins {
    id("snb.hexagonal-infra")
}

dependencies {
    api(project(":snb-activity:snb-activity-app"))
    implementation(project(":snb-sub2api"))
}
