package app.simplifymy.browser.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "speed_dial")
data class SpeedDialTile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val position: Int,
)

@Dao
interface SpeedDialDao {
    @Query("SELECT * FROM speed_dial ORDER BY position")
    fun tiles(): Flow<List<SpeedDialTile>>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM speed_dial")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(tile: SpeedDialTile)

    @Update
    suspend fun update(vararg tiles: SpeedDialTile)

    @Delete
    suspend fun delete(tile: SpeedDialTile)

    @Transaction
    suspend fun add(title: String, url: String) = insert(SpeedDialTile(title = title, url = url, position = nextPosition()))
}
