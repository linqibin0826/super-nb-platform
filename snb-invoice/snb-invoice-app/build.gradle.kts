plugins {
    id("snb.hexagonal-app")
}

dependencies {
    api(project(":snb-invoice:snb-invoice-domain"))
}
