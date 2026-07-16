plugins {
    id("snb.hexagonal-adapter")
}

dependencies {
    api(project(":snb-guide:snb-guide-app"))
    implementation(project(":snb-common"))
    implementation(project(":snb-sub2api"))
}
