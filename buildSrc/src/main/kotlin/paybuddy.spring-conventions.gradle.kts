plugins {
	id("paybuddy.kotlin-conventions")
	id("io.spring.dependency-management")
	kotlin("plugin.spring")
}

dependencies {
	implementation(libs.kotlin.reflect)

	testImplementation(libs.spring.boot.starter.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}
