package com.aifinancial.mobile.di

import android.content.Context
import androidx.room.Room
import com.aifinancial.mobile.data.local.AppDatabase
import com.aifinancial.mobile.data.local.PendingDocumentDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "aifinancial.db").build()

    @Provides
    fun providePendingDocumentDao(database: AppDatabase): PendingDocumentDao =
        database.pendingDocumentDao()
}
