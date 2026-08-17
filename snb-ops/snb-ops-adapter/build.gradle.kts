plugins {
    id("snb.hexagonal-adapter")
}

dependencies {
    api(project(":snb-ops:snb-ops-app"))
    implementation(project(":snb-common"))
    implementation(project(":snb-sub2api"))
}
