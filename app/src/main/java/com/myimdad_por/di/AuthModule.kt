package com.myimdad_por.di

import com.myimdad_por.data.remote.api.ApiService
import com.myimdad_por.data.remote.datasource.AuthRemoteDataSource
import com.myimdad_por.data.repository.AuthRepositoryImpl
import com.myimdad_por.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    companion object {

        @Provides
        @Singleton
        fun provideAuthRemoteDataSource(
            apiService: ApiService
        ): AuthRemoteDataSource {
            return AuthRemoteDataSource(apiService)
        }
    }
}