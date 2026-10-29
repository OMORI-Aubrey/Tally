package com.jaeyun.tally.ui.screens.appclass

import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRowsTest {

    private fun app(pkg: String, label: String, category: Category = Category.DISTRACT) =
        AppClassification(pkg, label, category, ClassificationSource.AUTO_CATEGORY)

    @Test
    fun `한글 이름은 가나다 순서로 둔다`() {
        val rows = appRows(listOf(app("c", "카카오톡"), app("a", "가계부"), app("b", "네이버 사전")))

        assertEquals(listOf("가계부", "네이버 사전", "카카오톡"), rows.map { it.appLabel })
    }

    @Test
    fun `영문 이름은 대소문자와 관계없이 섞는다`() {
        val rows = appRows(listOf(app("y", "youtube"), app("c", "Chrome"), app("a", "anki")))

        assertEquals(listOf("anki", "Chrome", "youtube"), rows.map { it.appLabel })
    }

    @Test
    fun `이름이 같으면 패키지 이름으로 순서를 고정한다`() {
        val rows = appRows(listOf(app("com.b", "메모"), app("com.a", "메모")))

        assertEquals(listOf("com.a", "com.b"), rows.map { it.packageName })
    }

    @Test
    fun `분류를 그대로 옮긴다`() {
        val row = appRows(listOf(app("d", "사전", Category.ALLOWED))).single()

        assertEquals(AppRow("d", "사전", Category.ALLOWED), row)
    }
}
