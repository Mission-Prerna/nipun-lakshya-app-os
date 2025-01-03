package com.data.di

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.room.Room
import com.data.DataConstants
import com.data.db.Convertors
import com.data.db.NLDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Singleton
    @Provides
    fun provideNLDatabase(
        @ApplicationContext context: Context
    ): NLDatabase {
        val typeConverter = Convertors()
        val nlDatabase = Room.databaseBuilder(
            context,
            NLDatabase::class.java,
            DataConstants.DB_NAME
        ).addTypeConverter(typeConverter).build()
        return nlDatabase
    }

    @Singleton
    @Provides
    fun providesTeacherPerformanceInsightsDao(database: NLDatabase) = database.getTeacherPerformanceInsightsDao()

    @Singleton
    @Provides
    fun provideStudentsDao(database: NLDatabase) = database.getStudentsDao()

    @Singleton
    @Provides
    fun provideMetadataDao(database: NLDatabase) = database.getMetadataDao()

    @Singleton
    @Provides
    fun provideAssessmentHistoryDao(database: NLDatabase) = database.getAssessmentHistoryDao()

    @Singleton
    @Provides
    fun provideAssessmentStateDao(database: NLDatabase) = database.getAssessmentStateDao()

    @Singleton
    @Provides
    fun provideAssessmentSubmissionsDao(database: NLDatabase) = database.getAssessmentSubmissionDao()

    @Singleton
    @Provides
    fun provideAssessmentSchoolHistoryDao(database: NLDatabase) = database.getAssessmentSchoolHistoryDao()

    @Singleton
    @Provides
    fun provideCommonPrefs(@ApplicationContext context: Context) : SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
