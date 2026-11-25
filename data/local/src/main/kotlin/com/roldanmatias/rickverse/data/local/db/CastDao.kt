package com.roldanmatias.rickverse.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
internal abstract class CastDao {
    @Query(
        """
        SELECT * FROM cast_member
        WHERE (:name IS NULL OR name LIKE '%' || :name || '%' ESCAPE '\')
          AND (:status IS NULL OR status = :status)
        ORDER BY id LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun page(name: String?, status: String?, limit: Int, offset: Int): List<CastMemberEntity>

    @Query("SELECT * FROM cast_member WHERE id = :id")
    abstract suspend fun member(id: Int): CastMemberEntity?

    @Query(
        """
        SELECT episode.* FROM episode
        JOIN cast_episode ON episode.id = cast_episode.episode_id
        WHERE cast_episode.cast_id = :castId ORDER BY episode.id
        """,
    )
    abstract suspend fun episodesOf(castId: Int): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIgnore(members: List<CastMemberEntity>)

    @Query(
        """
        UPDATE cast_member SET name = :name, status = :status, species = :species,
          image_url = :imageUrl, cached_at = :cachedAt WHERE id = :id
        """,
    )
    protected abstract suspend fun updateBasics(
        id: Int,
        name: String,
        status: String,
        species: String,
        imageUrl: String,
        cachedAt: Long,
    )

    /** Inserts new rows and refreshes existing ones WITHOUT wiping columns a list response does not carry. */
    @Transaction
    open suspend fun saveList(members: List<CastMemberEntity>) {
        insertIgnore(members)
        members.forEach { updateBasics(it.id, it.name, it.status, it.species, it.imageUrl, it.cachedAt) }
    }

    @Upsert
    protected abstract suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM cast_episode WHERE cast_id = :castId")
    protected abstract suspend fun deleteLinks(castId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLinks(links: List<CastEpisodeCrossRef>)

    @Query("UPDATE cast_member SET gender = :gender, origin_name = :origin, location_name = :location WHERE id = :id")
    protected abstract suspend fun updateDetailColumns(id: Int, gender: String?, origin: String?, location: String?)

    /** One transaction: a reader never sees a character with half of its episodes. */
    @Transaction
    open suspend fun saveDetail(member: CastMemberEntity, episodes: List<EpisodeEntity>) {
        saveList(listOf(member))
        updateDetailColumns(member.id, member.gender, member.originName, member.locationName)
        upsertEpisodes(episodes)
        deleteLinks(member.id)
        insertLinks(episodes.map { CastEpisodeCrossRef(member.id, it.id) })
    }

    @Query("DELETE FROM cast_member")
    abstract suspend fun clearMembers()
}
