package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExecutionLog
import com.example.data.model.TermuxScript
import kotlinx.coroutines.flow.Flow

@Dao
interface TermuxDao {
    @Query("SELECT * FROM termux_scripts ORDER BY isFavorite DESC, name ASC")
    fun getAllScripts(): Flow<List<TermuxScript>>

    @Query("SELECT * FROM termux_scripts WHERE id = :id")
    suspend fun getScriptById(id: Long): TermuxScript?

    @Query("SELECT * FROM termux_scripts WHERE showInWidget = 1 ORDER BY isFavorite DESC, id ASC LIMIT 5")
    fun getWidgetScripts(): Flow<List<TermuxScript>>

    @Query("SELECT * FROM termux_scripts WHERE showInWidget = 1 ORDER BY isFavorite DESC, id ASC LIMIT 5")
    suspend fun getWidgetScriptsSync(): List<TermuxScript>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: TermuxScript): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllScripts(scripts: List<TermuxScript>)

    @Update
    suspend fun updateScript(script: TermuxScript)

    @Delete
    suspend fun deleteScript(script: TermuxScript)

    @Query("DELETE FROM termux_scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Long)

    @Query("DELETE FROM termux_scripts")
    suspend fun clearAllScripts()

    @Query("SELECT COUNT(*) FROM termux_scripts")
    suspend fun getScriptCount(): Int

    // Execution Logs
    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE scriptId = :scriptId ORDER BY timestamp DESC LIMIT 50")
    fun getLogsByScript(scriptId: Long): Flow<List<ExecutionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLog): Long

    @Query("DELETE FROM execution_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM execution_logs WHERE scriptId = :scriptId")
    suspend fun deleteLogsByScript(scriptId: Long)
}
