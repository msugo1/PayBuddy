plugins {
	id("paybuddy.kotlin-conventions")
	id("org.springframework.boot")
	id("io.spring.dependency-management")
	kotlin("plugin.spring")
	kotlin("plugin.jpa")
}

dependencies {
	implementation(libs.kotlin.reflect)

	testImplementation(libs.spring.boot.starter.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.jar {
	enabled = true
}
