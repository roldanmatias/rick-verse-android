package com.roldanmatias.rickverse.core.domain.model

/**
 * A character of the show, as the rest of the app sees it.
 *
 * Named `CastMember` (not `Character`) to avoid clashing with `java.lang.Character`.
 * Deliberately free of any UI-framework annotation; see ADR 0001 and `config/compose/stability.conf`.
 */
public data class CastMember(
    val id: Int,
    val name: String,
    val status: Status,
    val species: String,
    val imageUrl: String,
) {
    /** Life status. Anything the backend sends that we do not recognise becomes [Unknown]. */
    public enum class Status { Alive, Dead, Unknown }
}
