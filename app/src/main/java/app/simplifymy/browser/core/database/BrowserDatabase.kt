package app.simplifymy.browser.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

private val DEFAULT_TILES = listOf(
    "Wikipedia" to "https://www.wikipedia.org",
    "YouTube" to "https://m.youtube.com",
    "Reddit" to "https://www.reddit.com",
    "GitHub" to "https://github.com",
    "BBC News" to "https://www.bbc.com/news",
    "Amazon" to "https://www.amazon.com",
    "IMDb" to "https://m.imdb.com",
    "Booking" to "https://www.booking.com",
)

@Database(entities = [SpeedDialTile::class], version = 1)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun speedDial(): SpeedDialDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, BrowserDatabase::class.java, "browser.db")
            .addCallback(SeedDefaultTiles)
            .build()
    }
}

private object SeedDefaultTiles : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DEFAULT_TILES.forEachIndexed { position, (title, url) ->
            db.execSQL("INSERT INTO speed_dial (title, url, position) VALUES (?, ?, ?)", arrayOf<Any>(title, url, position))
        }
    }
}
