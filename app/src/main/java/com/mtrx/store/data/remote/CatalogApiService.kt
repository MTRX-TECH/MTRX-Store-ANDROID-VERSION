package com.mtrx.store.data.remote

import com.mtrx.store.data.remote.dto.AppDto
import retrofit2.http.GET
import retrofit2.http.Url

interface CatalogApiService {
    @GET
    suspend fun getCatalog(@Url url: String): List<AppDto>
}
