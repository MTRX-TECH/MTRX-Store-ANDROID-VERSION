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
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.InetAddress
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
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("User-Agent", "MTRXStore/1.0 (Android)")
                if (original.header("Accept") == null) {
                    builder.header("Accept", "application/json, text/plain, */*")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(120L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideCatalogApiService(okHttpClient: OkHttpClient): CatalogApiService {
        val catalogClient = okHttpClient.newBuilder()
            .connectTimeout(3L, TimeUnit.SECONDS)
            .readTimeout(4L, TimeUnit.SECONDS)
            .callTimeout(5L, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(catalogClient)
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
