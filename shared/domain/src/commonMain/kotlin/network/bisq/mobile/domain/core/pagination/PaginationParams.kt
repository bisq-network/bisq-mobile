package network.bisq.mobile.domain.core.pagination

import kotlinx.serialization.Serializable

@Serializable
data class PaginationParams(
    val page: Int,
    val pageSize: Int,
) {
    init {
        require(page >= 1) { "page must be >= 1, was $page" }
        require(pageSize in 1..MAX_PAGE_SIZE) { "pageSize must be in 1..$MAX_PAGE_SIZE, was $pageSize" }
    }

    companion object {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100

        fun of(
            page: Int?,
            pageSize: Int?,
        ): PaginationParams {
            val resolvedPage = if (page == null || page < 1) DEFAULT_PAGE else page
            val resolvedSize =
                if (pageSize == null || pageSize < 1) {
                    DEFAULT_PAGE_SIZE
                } else {
                    minOf(pageSize, MAX_PAGE_SIZE)
                }
            return PaginationParams(resolvedPage, resolvedSize)
        }
    }
}
