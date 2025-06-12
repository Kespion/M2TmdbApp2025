package com.example.m2tmdbapp2025.model

import com.google.gson.annotations.SerializedName

data class PersonDetail(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("birthday") val birthDate: String?,
    @SerializedName("place_of_birth") val birthPlace: String?,
    val biography: String?
)