package network.bisq.mobile.domain.core.pagination

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaginationParamsTest {
    @Test
    fun `constructor accepts valid range`() {
        val params = PaginationParams(page = 1, pageSize = 20)
        assertEquals(1, params.page)
        assertEquals(20, params.pageSize)
    }

    @Test
    fun `constructor rejects page below 1`() {
        assertFailsWith<IllegalArgumentException> { PaginationParams(page = 0, pageSize = 20) }
        assertFailsWith<IllegalArgumentException> { PaginationParams(page = -1, pageSize = 20) }
    }

    @Test
    fun `constructor rejects pageSize outside 1 to MAX_PAGE_SIZE`() {
        assertFailsWith<IllegalArgumentException> { PaginationParams(page = 1, pageSize = 0) }
        assertFailsWith<IllegalArgumentException> { PaginationParams(page = 1, pageSize = -1) }
        assertFailsWith<IllegalArgumentException> {
            PaginationParams(page = 1, pageSize = PaginationParams.MAX_PAGE_SIZE + 1)
        }
    }

    @Test
    fun `of returns defaults for null inputs`() {
        val params = PaginationParams.of(page = null, pageSize = null)
        assertEquals(PaginationParams.DEFAULT_PAGE, params.page)
        assertEquals(PaginationParams.DEFAULT_PAGE_SIZE, params.pageSize)
    }

    @Test
    fun `of clamps invalid page values to default`() {
        assertEquals(PaginationParams.DEFAULT_PAGE, PaginationParams.of(page = 0, pageSize = 20).page)
        assertEquals(PaginationParams.DEFAULT_PAGE, PaginationParams.of(page = -5, pageSize = 20).page)
    }

    @Test
    fun `of clamps invalid pageSize to default`() {
        assertEquals(PaginationParams.DEFAULT_PAGE_SIZE, PaginationParams.of(page = 1, pageSize = 0).pageSize)
        assertEquals(PaginationParams.DEFAULT_PAGE_SIZE, PaginationParams.of(page = 1, pageSize = -1).pageSize)
    }

    @Test
    fun `of caps pageSize at MAX_PAGE_SIZE`() {
        val params = PaginationParams.of(page = 1, pageSize = PaginationParams.MAX_PAGE_SIZE + 50)
        assertEquals(PaginationParams.MAX_PAGE_SIZE, params.pageSize)
    }

    @Test
    fun `of preserves valid values`() {
        val params = PaginationParams.of(page = 5, pageSize = 50)
        assertEquals(5, params.page)
        assertEquals(50, params.pageSize)
    }

    @Test
    fun `of accepts boundary values`() {
        val minBoundary = PaginationParams.of(page = 1, pageSize = 1)
        assertEquals(1, minBoundary.page)
        assertEquals(1, minBoundary.pageSize)

        val maxBoundary = PaginationParams.of(page = 1, pageSize = PaginationParams.MAX_PAGE_SIZE)
        assertEquals(PaginationParams.MAX_PAGE_SIZE, maxBoundary.pageSize)
    }
}
