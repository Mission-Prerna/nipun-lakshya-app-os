package com.samagra.parent.di

import com.morziz.network.config.ClientType
import com.morziz.network.network.Network
import com.samagra.commons.utils.CommonConstants
import com.samagra.parent.network.TeacherInsightsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideTeacherInsightsService(): TeacherInsightsService {
        return Network.getClient(
            ClientType.RETROFIT,
            TeacherInsightsService::class.java,
            CommonConstants.IDENTITY_APP_SERVICE
        )!!
    }
}
