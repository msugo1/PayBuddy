plugins {
	kotlin("jvm")
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		freeCompilerArgs.add("-Xjsr305=strict")
	}
}

dependencies {
	testImplementation(libs.kotlin.test.junit5)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

repositories {
	mavenCentral()
}
