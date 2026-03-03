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
            .addMigrations(MIGRATION_26_27, MIGRATION_27_28)
            .build()
    }

    @Provides
    fun provideAddonDao(db: LumeraDatabase): AddonDao {
        return db.addonDao()
    }
}
