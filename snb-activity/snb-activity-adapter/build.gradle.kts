plugins {
    id("snb.hexagonal-adapter")
}

dependencies {
    api(project(":snb-activity:snb-activity-app"))
    implementation(project(":snb-common"))
    implementation(project(":snb-sub2api"))
}
