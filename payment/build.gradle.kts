plugins {
	id("paybuddy.spring-boot-conventions")
	alias(libs.plugins.openapi.generator)
}

dependencies {
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.data.redis)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.springdoc.openapi.starter.webmvc.ui)

	// Database
	runtimeOnly(libs.postgresql)
	implementation(libs.flyway.core)
	implementation(libs.flyway.database.postgresql)

	// ULID
	implementation(libs.ulid.creator)

    testImplementation(libs.swagger.request.validator.mockmvc)
}

tasks.bootJar {
	enabled = true
}

tasks.jar {
	enabled = false
}

openApiGenerate {
	generatorName.set("spring")  // kotlin-spring → spring (Java)
    inputSpec.set("$rootDir/docs/openapi/api/payment.yaml")
	outputDir.set("${layout.buildDirectory.get()}/generated")
	apiPackage.set("com.paybuddy.payment.api")
	modelPackage.set("com.paybuddy.payment.api.model")

    // https://openapi-generator.tech/docs/generators/spring/#config-options
	configOptions.set(mapOf(
        "interfaceOnly" to "true",                // 인터페이스만 생성
        "useSpringBoot3" to "true",               // Spring Boot 3 사용
        "useTags" to "true",                      // 태그별로 API 인터페이스 생성 (PaymentsApi, ChargebacksApi 등)
        "useBeanValidation" to "true",            // @Valid, @NotNull 등 (기본값 true)
        "skipDefaultInterface" to "true",         // 기본 구현 메서드 생성 안함
        "requestMappingMode" to "none",           // @RequestMapping 생성 안함 (Controller에서 직접 관리)
        "exceptionHandler" to "false",            // 전역 예외 핸들러 생성 안함
        "useOneOfInterfaces" to "true",           // oneOf를 Java interface로 생성
        "openApiNullable" to "false"              // JsonNullable 사용 안함 (일반 nullable로)
	))
}

sourceSets {
    main {
        java.srcDir(layout.buildDirectory.dir("generated/src/main/java"))
    }
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}