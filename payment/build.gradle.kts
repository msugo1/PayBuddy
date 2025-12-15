plugins {
	id("paybuddy.spring-boot-conventions")
	alias(libs.plugins.asciidoctor)
}

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
	implementation(libs.spring.boot.starter.web)
	implementation(libs.jackson.module.kotlin)

	testImplementation(libs.spring.restdocs.mockmvc)
}

tasks.test {
	outputs.dir(project.extra["snippetsDir"]!!)
}

tasks.asciidoctor {
	inputs.dir(project.extra["snippetsDir"]!!)
	dependsOn(tasks.test)
}