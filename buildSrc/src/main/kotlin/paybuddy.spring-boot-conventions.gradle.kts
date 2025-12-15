plugins {
	id("paybuddy.kotlin-conventions")
	id("org.springframework.boot")
	id("io.spring.dependency-management")
	kotlin("plugin.spring")
}

dependencies {
	implementation(libs.kotlin.reflect)

	testImplementation(libs.spring.boot.starter.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.bootJar {
	enabled = true
}

tasks.jar {
	enabled = false
}
