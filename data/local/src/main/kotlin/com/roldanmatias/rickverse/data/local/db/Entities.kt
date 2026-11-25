package com.roldanmatias.rickverse.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cast_member")
internal data class CastMemberEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val status: String,
    val species: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
    // Added in schema 2: only a detail response carries these. `null` means "detail never cached".
    val gender: String? = null,
    @ColumnInfo(name = "origin_name") val originName: String? = null,
    @ColumnInfo(name = "location_name") val locationName: String? = null,
)

@Entity(tableName = "episode")
internal data class EpisodeEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val code: String,
    val season: Int,
    val number: Int,
    @ColumnInfo(name = "air_date") val airDate: String?,
)

/** Join table: which characters appear in which episodes. */
@Entity(
    tableName = "cast_episode",
    primaryKeys = ["cast_id", "episode_id"],
    indices = [Index("episode_id")],
)
internal data class CastEpisodeCrossRef(
    @ColumnInfo(name = "cast_id") val castId: Int,
    @ColumnInfo(name = "episode_id") val episodeId: Int,
)
