package com.mtrx.store.di

import android.content.Context
import androidx.room.Room
import com.mtrx.store.core.Constants
import com.mtrx.store.data.local.MtrxDatabase
import com.mtrx.store.data.local.dao.AppDao
import com.mtrx.store.data.local.preferences.SettingsDataStore
import com.mtrx.store.data.remote.CatalogApiService
import com.mtrx.store.data.repository.StoreRepository
import com.mtrx.store.data.repository.StoreRepositoryImpl
import com.mtrx.store.domain.downloader.ApkDownloader
import com.mtrx.store.domain.installer.PackageInstallerHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MTRXStore/1.0 (Android)")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideCatalogApiService(okHttpClient: OkHttpClient): CatalogApiService {
        return Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CatalogApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MtrxDatabase {
        return Room.databaseBuilder(
            context,
            MtrxDatabase::class.java,
            "mtrx_store.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideAppDao(database: MtrxDatabase): AppDao {
        return database.appDao
    }

    @Provides
    @Singleton
    fun provideStoreRepository(
        @ApplicationContext context: Context,
        apiService: CatalogApiService,
        appDao: AppDao,
        settingsDataStore: SettingsDataStore,
        apkDownloader: ApkDownloader,
        packageInstallerHelper: PackageInstallerHelper
    ): StoreRepository {
        return StoreRepositoryImpl(
            context,
            apiService,
            appDao,
            settingsDataStore,
            apkDownloader,
            packageInstallerHelper
        )
    }
}
