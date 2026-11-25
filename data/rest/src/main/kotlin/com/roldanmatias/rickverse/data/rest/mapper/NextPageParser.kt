package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.data.rest.dto.PageDto
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** REST gives `info.next` as a URL (`…/character?page=2`); Domain wants the page number. `null` on the last page. */
internal fun parseNextPage(next: String?): Int? = next?.toHttpUrlOrNull()?.queryParameter("page")?.toIntOrNull()

internal fun <D, T> PageDto<D>.toPage(transform: (D) -> T): Page<T> =
    Page(items = results.map(transform), nextPage = parseNextPage(info.next))

/** `"https://…/episode/28"` -> `28`. */
internal fun String.trailingId(): Int? = substringAfterLast('/').toIntOrNull()
