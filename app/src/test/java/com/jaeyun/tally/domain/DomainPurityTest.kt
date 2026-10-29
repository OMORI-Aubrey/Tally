package com.jaeyun.tally.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** `domain`은 Android 의존성 없는 순수 Kotlin이다 (CLAUDE.md 아키텍처 규칙, §10.3) */
class DomainPurityTest {

    @Test
    fun `domain 패키지는 android를 import하지 않는다`() {
        // 단위 테스트의 작업 디렉터리는 app 모듈이다
        val domainDir = File("src/main/java/com/jaeyun/tally/domain")
        assertTrue("domain 디렉터리를 찾지 못했다: ${domainDir.absolutePath}", domainDir.isDirectory)

        val offenders = domainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readLines().any { it.trim().startsWith("import android.") || it.trim().startsWith("import androidx.") } }
            .map { it.name }
            .toList()

        assertEquals(emptyList<String>(), offenders)
    }
}
