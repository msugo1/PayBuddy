plugins {
    id("paybuddy.spring-boot-conventions")
	id("org.springframework.boot")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
}
