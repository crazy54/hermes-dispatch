package com.nousresearch.hermes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ─── Entities ────────────────────────────────────────────────────────────────

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val toolName: String? = null,
    val toolStatus: String? = null,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessages(sessionId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE sessionId = :id LIMIT 1")
    suspend fun findById(id: String): SessionEntity?

    @Query("DELETE FROM sessions WHERE sessionId = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE sessions SET title = :title, updatedAt = :updatedAt WHERE sessionId = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [MessageEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HermesDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun sessionDao(): SessionDao
}
