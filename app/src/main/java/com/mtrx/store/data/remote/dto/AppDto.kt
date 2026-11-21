package com.mtrx.store.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AppDto(
    @SerializedName("packageName")
    val packageName: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("shortDescription")
    val shortDescription: String,

    @SerializedName("iconUrl")
    val iconUrl: String,

    @SerializedName("screenshots")
    val screenshots: List<String>?,

    @SerializedName("category")
    val category: String,

    @SerializedName("versionName")
    val versionName: String,

    @SerializedName("versionCode")
    val versionCode: Long,

    @SerializedName("apkUrl")
    val apkUrl: String,

    @SerializedName("sha256")
    val sha256: String,

    @SerializedName("sizeBytes")
    val sizeBytes: Long,

    @SerializedName("changelog")
    val changelog: String?,

    @SerializedName("minSdk")
    val minSdk: Int,

    @SerializedName("updatedAt")
    val updatedAt: String?
)
