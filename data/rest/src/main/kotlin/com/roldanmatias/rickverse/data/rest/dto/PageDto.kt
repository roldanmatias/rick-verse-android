package com.roldanmatias.rickverse.data.rest.dto

import kotlinx.serialization.Serializable

/** Pagination envelope of every list endpoint. `next` is a full URL (or null on the last page). */
@Serializable
internal data class InfoDto(val count: Int, val pages: Int, val next: String? = null, val prev: String? = null)

@Serializable
internal data class PageDto<T>(val info: InfoDto, val results: List<T>)
