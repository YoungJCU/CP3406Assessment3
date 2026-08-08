package com.youngjcu.pclab.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.youngjcu.pclab.data.local.AppDatabase
import com.youngjcu.pclab.data.remote.HardwareApi
import com.youngjcu.pclab.data.repository.DataStoreSettingsRepository
import com.youngjcu.pclab.data.repository.GitHubHardwareRepository
import com.youngjcu.pclab.data.repository.HardwareRepository
import com.youngjcu.pclab.data.repository.LearningRepository
import com.youngjcu.pclab.data.repository.RoomLearningRepository
import com.youngjcu.pclab.data.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Provides
    @Singleton
    fun provideApi(moshi: Moshi): HardwareApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HardwareApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pclab.db").build()

    @Provides
    fun provideLearningDao(database: AppDatabase) = database.learningDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context) = context.dataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindHardwareRepository(repository: GitHubHardwareRepository): HardwareRepository
    @Binds abstract fun bindLearningRepository(repository: RoomLearningRepository): LearningRepository
    @Binds abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
}
