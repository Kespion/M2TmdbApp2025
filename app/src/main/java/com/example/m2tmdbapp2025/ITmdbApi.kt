package com.example.m2tmdbapp2025

import com.example.m2tmdbapp2025.model.PersonDetail
import com.example.m2tmdbapp2025.model.PersonPopularResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// TMDB API call example:
// https://api.themoviedb.org/3/configuration?api_key=f8c59b73c44d9240c1ded0a07da0d5f5
// https://api.themoviedb.org/3/person/popular?api_key=f8c59b73c44d9240c1ded0a07da0d5f5

const val TMDB_API_KEY = "f8c59b73c44d9240c1ded0a07da0d5f5"

interface ITmdbApi {
    @GET("person/popular")
    fun getPopularPerson(
        @Query("api_key") apiKey: String,
        @Query("page") pageNb: Int
    ) : Call<PersonPopularResponse>

    @GET("person/{id}")
    suspend fun getPersonDetail(
        @Path("id") personId: Int,
        @Query("api_key") apiKey: String = TMDB_API_KEY,
        @Query("language") lang: String = "en-US"
    ): PersonDetail
}