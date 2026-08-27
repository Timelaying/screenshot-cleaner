package com.example.screenshotcleaner.data.local

import androidx.room.Room
import com.example.screenshotcleaner.domain.ScreenshotDecision
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenshotDecisionDaoTest {
    private lateinit var database: ScreenshotCleanerDatabase
    private lateinit var dao: ScreenshotDecisionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ScreenshotCleanerDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.screenshotDecisionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun storesKeptAndDeletedDecisions() = runTest {
        dao.upsertDecision(decision(mediaId = 11, value = ScreenshotDecision.KEPT))
        dao.upsertDecision(decision(mediaId = 22, value = ScreenshotDecision.DELETED))

        val decidedIds = dao.mediaIdsForDecisions(
            listOf(ScreenshotDecision.KEPT, ScreenshotDecision.DELETED)
        )

        assertEquals(setOf(11L, 22L), decidedIds.toSet())
    }

    @Test
    fun upsertReplacesDecisionForSameMediaId() = runTest {
        dao.upsertDecision(decision(mediaId = 11, value = ScreenshotDecision.KEPT))
        dao.upsertDecision(decision(mediaId = 11, value = ScreenshotDecision.DELETED))

        assertTrue(
            dao.mediaIdsForDecisions(listOf(ScreenshotDecision.KEPT)).isEmpty()
        )
        assertEquals(
            listOf(11L),
            dao.mediaIdsForDecisions(listOf(ScreenshotDecision.DELETED))
        )
    }

    private fun decision(mediaId: Long, value: ScreenshotDecision): ScreenshotDecisionEntity {
        return ScreenshotDecisionEntity(
            mediaId = mediaId,
            decision = value,
            decidedAtMillis = 1234L
        )
    }
}
