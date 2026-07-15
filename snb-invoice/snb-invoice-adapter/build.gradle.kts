plugins {
    id("snb.hexagonal-adapter")
}

dependencies {
    api(project(":snb-invoice:snb-invoice-app"))
    implementation(project(":snb-common"))
    implementation(project(":snb-sub2api"))
}
