package com.example.screenshotcleaner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.screenshotcleaner.domain.ScreenshotDecision

@Database(
    entities = [ScreenshotDecisionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ScreenshotDecisionConverters::class)
abstract class ScreenshotCleanerDatabase : RoomDatabase() {
    abstract fun screenshotDecisionDao(): ScreenshotDecisionDao
}

class ScreenshotDecisionConverters {
    @TypeConverter
    fun fromDecision(decision: ScreenshotDecision): String = decision.name

    @TypeConverter
    fun toDecision(value: String): ScreenshotDecision = ScreenshotDecision.valueOf(value)
}
