package com.lumera.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.local.LumeraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN preferredAudioLanguage TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE profiles ADD COLUMN preferredAudioLanguageSecondary TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE profiles ADD COLUMN preferredSubtitleLanguage TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE profiles ADD COLUMN preferredSubtitleLanguageSecondary TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Version 28: no-op migration (schema unchanged, version was bumped without migration)
    }
}

private val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE addons ADD COLUMN supportsMeta INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE addons ADD COLUMN typesJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE addons ADD COLUMN idPrefixesJson TEXT NOT NULL DEFAULT '[]'")
    }
}

private val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE addons ADD COLUMN supportsStream INTEGER NOT NULL DEFAULT 1")
    }
}

private val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN splashEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

private val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN subtitleSize INTEGER NOT NULL DEFAULT 100")
        db.execSQL("ALTER TABLE profiles ADD COLUMN subtitleOffset INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE profiles ADD COLUMN subtitleTextColor INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE profiles ADD COLUMN subtitleBackgroundColor INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN sourceSortingEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE profiles ADD COLUMN sourceEnabledQualities TEXT NOT NULL DEFAULT '4k,1080p,720p,unknown'")
        db.execSQL("ALTER TABLE profiles ADD COLUMN sourceExcludePhrases TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN sourceSortPrimary TEXT NOT NULL DEFAULT 'quality'")
        db.execSQL("ALTER TABLE profiles ADD COLUMN sourceSortSecondary TEXT NOT NULL DEFAULT 'size'")
    }
}

private val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN sourceMaxSizeGb INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE profiles ADD COLUMN sourceExcludedFormats TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN tmdbEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE profiles ADD COLUMN tmdbLanguage TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN assRendererEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS watchlist (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "type TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "poster TEXT, " +
                "addedAt INTEGER NOT NULL)"
        )
    }
}

private val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN watchedThreshold INTEGER NOT NULL DEFAULT 85")
        db.execSQL("ALTER TABLE watch_history ADD COLUMN watched INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE watch_history ADD COLUMN background TEXT")
        db.execSQL("ALTER TABLE watch_history ADD COLUMN logo TEXT")
    }
}

private val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE watch_history ADD COLUMN scrobbled INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS series_next_up (" +
                "seriesId TEXT NOT NULL PRIMARY KEY, " +
                "title TEXT NOT NULL, " +
                "poster TEXT, " +
                "nextSeason INTEGER NOT NULL, " +
                "nextEpisode INTEGER NOT NULL, " +
                "nextEpisodeTitle TEXT, " +
                "nextReleased TEXT, " +
                "isComplete INTEGER NOT NULL DEFAULT 0, " +
                "updatedAt INTEGER NOT NULL)"
        )
    }
}

private val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE series_next_up ADD COLUMN isNewEpisode INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Backfill profileId on existing watchlist/watch_history rows to the first existing
        // profile (SQLite auto-generated IDs start at 1). If there are no profiles, rows
        // orphan to profileId = 0 and will remain invisible until a profile claims them.
        val defaultProfileId: Int = db.query("SELECT MIN(id) FROM profiles").use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else 0
        }

        // Recreate `watchlist` with composite primary key (id, profileId).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS watchlist_new (" +
                "id TEXT NOT NULL, " +
                "profileId INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "poster TEXT, " +
                "addedAt INTEGER NOT NULL, " +
                "PRIMARY KEY(id, profileId))"
        )
        db.execSQL(
            "INSERT INTO watchlist_new (id, profileId, type, title, poster, addedAt) " +
                "SELECT id, $defaultProfileId, type, title, poster, addedAt FROM watchlist"
        )
        db.execSQL("DROP TABLE watchlist")
        db.execSQL("ALTER TABLE watchlist_new RENAME TO watchlist")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_watchlist_profileId ON watchlist(profileId)")

        // Recreate `watch_history` with composite primary key (id, profileId).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS watch_history_new (" +
                "id TEXT NOT NULL, " +
                "profileId INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "poster TEXT, " +
                "background TEXT, " +
                "logo TEXT, " +
                "position INTEGER NOT NULL, " +
                "duration INTEGER NOT NULL, " +
                "lastWatched INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "watched INTEGER NOT NULL DEFAULT 0, " +
                "scrobbled INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(id, profileId))"
        )
        db.execSQL(
            "INSERT INTO watch_history_new (id, profileId, title, poster, background, logo, position, duration, lastWatched, type, watched, scrobbled) " +
                "SELECT id, $defaultProfileId, title, poster, background, logo, position, duration, lastWatched, type, watched, scrobbled FROM watch_history"
        )
        db.execSQL("DROP TABLE watch_history")
        db.execSQL("ALTER TABLE watch_history_new RENAME TO watch_history")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_history_profileId ON watch_history(profileId)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LumeraDatabase {
        return Room.databaseBuilder(
            context,
            LumeraDatabase::class.java,
            "lumera_db"
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA synchronous = 2")
                }
            })
            .addMigrations(MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41, MIGRATION_41_42, MIGRATION_42_43, MIGRATION_43_44)
            .build()
    }

    @Provides
    fun provideAddonDao(db: LumeraDatabase): AddonDao {
        return db.addonDao()
    }
}
