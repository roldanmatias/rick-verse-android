package com.roldanmatias.rickverse.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTest {
    @Test
    fun `hasMore is true only when a next page exists`() {
        assertTrue(Page(listOf(1), nextPage = 2).hasMore)
        assertFalse(Page(listOf(1), nextPage = null).hasMore)
    }

    @Test
    fun `empty page is final`() {
        val page = Page.empty<Int>()

        assertTrue(page.items.isEmpty())
        assertFalse(page.hasMore)
    }
}
