package com.roldanmatias.rickverse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CastMemberEntity::class, EpisodeEntity::class, CastEpisodeCrossRef::class],
    version = 2,
    exportSchema = true,
)
internal abstract class RickVerseDatabase : RoomDatabase() {
    abstract fun castDao(): CastDao
}
