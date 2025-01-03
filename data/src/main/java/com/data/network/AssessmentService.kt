package com.data.network

import com.data.models.history.GradeSummary
import com.morziz.network.annotation.Retry
import com.morziz.network.annotation.RetryPolicyType
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AssessmentService {
    @GET("school/{udise}/students/result/summary")
    @Retry(retryPolicy = RetryPolicyType.exponential, retryCount = 2)
    fun getAssessmentHistories(
        @Path("udise") udise: String,
        @Query("grade") grades : String,
        @Header("Accept-Language") lang: String,
        @Header("authorization") token: String
    ): Call<MutableList<GradeSummary>>
}