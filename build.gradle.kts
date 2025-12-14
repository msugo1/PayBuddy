plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.asciidoctor)
}

group = "co.paybuddy"
version = "0.0.1-SNAPSHOT"
description = "Payment Platform"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
	implementation(libs.spring.boot.starter.web)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.kotlin.reflect)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.spring.restdocs.mockmvc)
	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	outputs.dir(project.extra["snippetsDir"]!!)
}

tasks.asciidoctor {
	inputs.dir(project.extra["snippetsDir"]!!)
	dependsOn(tasks.test)
}
