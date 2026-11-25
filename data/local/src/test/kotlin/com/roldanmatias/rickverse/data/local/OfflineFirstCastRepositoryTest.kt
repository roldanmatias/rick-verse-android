package com.roldanmatias.rickverse.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.testing.FakeCastRepository
import com.roldanmatias.rickverse.core.testing.TestData
import com.roldanmatias.rickverse.data.local.db.RickVerseDatabase
import com.roldanmatias.rickverse.data.local.repository.OfflineFirstCastRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Real Room (in memory, via Robolectric) behind the decorator, with a scripted fake as the "network". */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OfflineFirstCastRepositoryTest {
    private lateinit var database: RickVerseDatabase
    private val remote = FakeCastRepository()
    private lateinit var repository: OfflineFirstCastRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RickVerseDatabase::class.java).allowMainThreadQueries().build()
        repository = OfflineFirstCastRepository(remote, database.castDao())
    }

    @After
    fun tearDown() = database.close()

    private fun online(vararg members: CastMember, next: Int? = null) {
        remote.castHandler = { _, _ -> DomainResult.Success(Page(members.toList(), next)) }
    }

    private fun offline(error: DomainError = DomainError.Offline) {
        remote.castHandler = { _, _ -> DomainResult.Failure(error) }
        remote.detailHandler = { DomainResult.Failure(error) }
    }

    private fun <T> DomainResult<T>.value() = (this as DomainResult.Success).value

    @Test
    fun `online answers pass through untouched and are not flagged as cached`() = runBlocking {
        online(TestData.castMember(1), next = 2)

        val page = repository.cast(1, CastFilter.None).value()

        assertFalse(page.fromCache)
        assertEquals(2, page.nextPage)
    }

    @Test
    fun `offline serves the cached page with a flag`() = runBlocking {
        online(TestData.castMember(1, "Rick"), TestData.castMember(2, "Morty"))
        repository.cast(1, CastFilter.None)

        offline()
        val page = repository.cast(1, CastFilter.None).value()

        assertTrue(page.fromCache)
        assertEquals(listOf("Rick", "Morty"), page.items.map { it.name })
    }

    @Test
    fun `offline with an empty cache keeps the original error`() = runBlocking {
        offline(DomainError.Timeout)

        assertEquals(DomainResult.Failure(DomainError.Timeout), repository.cast(1, CastFilter.None))
    }

    @Test
    fun `offline search and status filter run against the cache`() = runBlocking {
        online(
            TestData.castMember(1, "Rick Sanchez"),
            TestData.castMember(2, "Morty Smith", CastMember.Status.Dead),
            TestData.castMember(3, "100% Rick_"),
        )
        repository.cast(1, CastFilter.None)
        offline()

        assertEquals(
            listOf("Rick Sanchez"),
            repository.cast(1, CastFilter(name = "sanchez")).value().items.map {
                it.name
            },
        )
        assertEquals(
            listOf("Morty Smith"),
            repository.cast(1, CastFilter(status = CastMember.Status.Dead)).value().items.map {
                it.name
            },
        )
        // LIKE wildcards in user input are escaped, not interpreted.
        assertEquals(listOf("100% Rick_"), repository.cast(1, CastFilter(name = "0% R")).value().items.map { it.name })
    }

    @Test
    fun `offline pagination exposes a next page only when the cache has more`() = runBlocking {
        online(*TestData.castMembers(25).toTypedArray(), next = 2)
        repository.cast(1, CastFilter.None)
        offline()

        val first = repository.cast(1, CastFilter.None).value()
        val second = repository.cast(2, CastFilter.None).value()

        assertEquals(20, first.items.size)
        assertEquals(2, first.nextPage)
        assertEquals(5, second.items.size)
        assertEquals(null, second.nextPage)
    }

    @Test
    fun `not found and invalid data are answers and not outages so there is no cache fallback`() = runBlocking {
        online(TestData.castMember(1))
        repository.cast(1, CastFilter.None)

        offline(DomainError.NotFound)

        assertEquals(DomainResult.Failure(DomainError.NotFound), repository.cast(1, CastFilter.None))
    }

    @Test
    fun `a list refresh does not wipe a cached detail`() = runBlocking {
        val detail = TestData.castMemberDetail(episodes = listOf(TestData.episode(1), TestData.episode(2)))
        remote.detailHandler = { DomainResult.Success(detail) }
        repository.detail(1)
        online(TestData.castMember(1, "Rick Renamed"))
        repository.cast(1, CastFilter.None) // list upsert on the same id

        offline()
        val cached = repository.detail(1).value()

        assertTrue(cached.fromCache)
        assertEquals("Rick Renamed", cached.member.name)
        assertEquals("Earth (C-137)", cached.originName)
        assertEquals(listOf(1, 2), cached.episodes.map { it.id })
    }

    @Test
    fun `detail offline without a cached detail fails (a list row alone is not enough)`() = runBlocking {
        online(TestData.castMember(1))
        repository.cast(1, CastFilter.None)

        offline()

        assertEquals(DomainResult.Failure(DomainError.Offline), repository.detail(1))
    }

    @Test
    fun `detailUpdates emits the cached copy first, then the fresh one`() = runBlocking {
        val first = TestData.castMemberDetail(episodes = listOf(TestData.episode(1)))
        remote.detailHandler = { DomainResult.Success(first) }
        repository.detail(1)
        val fresh = TestData.castMemberDetail(episodes = listOf(TestData.episode(1), TestData.episode(2)))
        remote.detailHandler = { DomainResult.Success(fresh) }

        val emissions = repository.detailUpdates(1).toList().map { it.value() }

        assertEquals(listOf(true, false), emissions.map { it.fromCache })
        assertEquals(listOf(1, 2), emissions.last().episodes.map { it.id })
    }

    @Test
    fun `detailUpdates offline keeps the cached copy and emits no error`() = runBlocking {
        remote.detailHandler = { DomainResult.Success(TestData.castMemberDetail()) }
        repository.detail(1)
        offline()

        val emissions = repository.detailUpdates(1).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions.single().value().fromCache)
    }
}
