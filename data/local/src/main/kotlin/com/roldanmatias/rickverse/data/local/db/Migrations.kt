package com.roldanmatias.rickverse.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 1 -> 2: the detail screen is cached too, so `cast_member` gains gender / origin / location.
 * Additive and nullable: existing rows stay valid (they are simply "detail not cached yet") and nothing is
 * dropped, so users keep their cache across the update. `MigrationTest` runs this against the exported schemas.
 */
internal val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE cast_member ADD COLUMN gender TEXT")
            connection.execSQL("ALTER TABLE cast_member ADD COLUMN origin_name TEXT")
            connection.execSQL("ALTER TABLE cast_member ADD COLUMN location_name TEXT")
        }
    }
