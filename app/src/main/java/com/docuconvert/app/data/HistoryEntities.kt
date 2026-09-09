package com.docuconvert.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Minimal metadata for one conversion (§16). NEVER stores file bytes —
 * only file names, formats, sizes and timestamps.
 */
@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceName: String,
    val sourceFormat: String,
    val targetFormat: String,
    val resultName: String?,
    val sizeBytes: Long,
    val timestampMs: Long,
    val success: Boolean
)

@Dao
interface ConversionHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ConversionHistoryEntity): Long

    @Query("SELECT * FROM conversion_history ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<ConversionHistoryEntity>>

    @Query("SELECT * FROM conversion_history ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ConversionHistoryEntity>

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entry: ConversionHistoryEntity)
}

@Database(entities = [ConversionHistoryEntity::class], version = 1, exportSchema = true)
abstract class DocuConvertDatabase : RoomDatabase() {
    abstract fun conversionHistoryDao(): ConversionHistoryDao
}
