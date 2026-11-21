package com.aifinancial.mobile.di

import android.content.Context
import androidx.room.Room
import com.aifinancial.mobile.data.local.AppDatabase
import com.aifinancial.mobile.data.local.BudgetsDao
import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.PendingDocumentDao
import com.aifinancial.mobile.data.local.TransactionsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "aifinancial.db")
            // Stage 3 bumped the schema (version 1 -> 2) to add the read-cache
            // tables. This app hasn't shipped, so there's no real user data
            // to preserve across that bump — destructively recreating the DB
            // on a version mismatch is fine for now (it only ever drops a
            // dev install's own local queue/cache, nothing server-side).
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePendingDocumentDao(database: AppDatabase): PendingDocumentDao =
        database.pendingDocumentDao()

    @Provides
    fun provideTransactionsDao(database: AppDatabase): TransactionsDao =
        database.transactionsDao()

    @Provides
    fun provideBudgetsDao(database: AppDatabase): BudgetsDao =
        database.budgetsDao()

    @Provides
    fun provideCacheMetadataDao(database: AppDatabase): CacheMetadataDao =
        database.cacheMetadataDao()
}
