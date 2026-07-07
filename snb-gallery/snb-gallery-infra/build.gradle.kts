plugins {
    id("snb.hexagonal-infra")
}

dependencies {
    api(project(":snb-gallery:snb-gallery-app"))
    implementation(project(":snb-sub2api"))
    implementation(libs.aws.s3)
    testImplementation("org.testcontainers:minio:1.21.4")
}
