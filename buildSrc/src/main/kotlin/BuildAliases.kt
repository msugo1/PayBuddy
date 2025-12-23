import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

// buildSrc 내에서 compile 된 version catalog 접근하기 위한 선언
// 'LibrariesForLibs' 접근을 위해서는
// buildSrc/build.gradle.kts 안의 files(libs.javaClass.superclass.protectionDomain.codeSource.location) 선언이 필요하다.
// https://github.com/gradle/gradle/issues/15383
val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()