plugins {
    id("paybuddy.spring-boot-conventions")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
}

tasks.bootJar {
    enabled = true
}

tasks.jar {
    enabled = false
}