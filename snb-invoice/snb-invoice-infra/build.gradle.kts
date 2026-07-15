plugins {
    id("snb.hexagonal-infra")
}

dependencies {
    api(project(":snb-invoice:snb-invoice-app"))
    implementation(project(":snb-sub2api"))
}
