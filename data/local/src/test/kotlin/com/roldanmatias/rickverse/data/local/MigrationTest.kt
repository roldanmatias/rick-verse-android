package com.roldanmatias.rickverse.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roldanmatias.rickverse.data.local.db.MIGRATION_1_2
import com.roldanmatias.rickverse.data.local.db.RickVerseDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Runs the real 1 -> 2 migration against the committed schema JSON files (`data/local/schemas`). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), RickVerseDatabase::class.java)

    @Test
    fun `migration 1 to 2 keeps existing rows and adds nullable detail columns`() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                "INSERT INTO cast_member (id, name, status, species, image_url, cached_at) " +
                    "VALUES (1,'Rick','Alive','Human','u',5)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB, 2, true, MIGRATION_1_2)

        migrated.query("SELECT name, gender, origin_name, location_name FROM cast_member WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Rick", cursor.getString(0))
            assertEquals(true, cursor.isNull(1))
            assertEquals(true, cursor.isNull(2))
            assertEquals(true, cursor.isNull(3))
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
