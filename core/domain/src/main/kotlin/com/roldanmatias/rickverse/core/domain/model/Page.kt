package com.roldanmatias.rickverse.core.domain.model

/**
 * One page of a paginated collection.
 *
 * Pagination is page-number based in both APIs; this type hides how each backend encodes "what comes next"
 * (a URL in REST, an `Int` in GraphQL).
 *
 * @property nextPage the number of the following page, or `null` on the last page.
 */
public data class Page<out T>(
    val items: List<T>,
    val nextPage: Int?,
) {
    /** `true` when another page can be requested. */
    public val hasMore: Boolean get() = nextPage != null

    public companion object {
        /** An empty, final page. Used for "no results" (REST answers `404` for an empty search). */
        public fun <T> empty(): Page<T> = Page(items = emptyList(), nextPage = null)
    }
}
