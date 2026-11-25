package com.roldanmatias.rickverse.data.rest.mapper

/** A response decoded fine but its content is unusable (for example a malformed episode code). */
internal class MappingException(message: String) : RuntimeException(message)
