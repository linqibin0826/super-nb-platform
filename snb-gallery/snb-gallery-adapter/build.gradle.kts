plugins {
    id("snb.hexagonal-adapter")
}

dependencies {
    api(project(":snb-gallery:snb-gallery-app"))
    implementation(project(":snb-common"))
    implementation(project(":snb-sub2api"))
}
