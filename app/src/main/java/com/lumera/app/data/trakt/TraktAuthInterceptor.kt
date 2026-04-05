package com.lumera.app.data.trakt

import android.util.Log
import com.lumera.app.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraktAuthInterceptor @Inject constructor(
    private val traktAuthManager: TraktAuthManager
) : Interceptor {

    companion object {
        private const val TAG = "TraktAuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val token = traktAuthManager.getAccessToken()
        val response = chain.proceed(buildRequest(original, token))

        // Fix #1: If we get a 401, try refreshing the token and retry once
        if (response.code == 401 && token != null) {
            Log.d(TAG, "Got 401, attempting token refresh")
            response.close()

            val newToken = runBlocking { traktAuthManager.refreshAccessToken() }
            if (newToken != null) {
                Log.d(TAG, "Token refreshed, retrying request")
                return chain.proceed(buildRequest(original, newToken))
            } else {
                Log.w(TAG, "Token refresh failed, returning 401")
            }
        }

        return response
    }

    private fun buildRequest(original: okhttp3.Request, token: String?): okhttp3.Request {
        val builder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("trakt-api-version", "2")
            .header("trakt-api-key", BuildConfig.TRAKT_CLIENT_ID)
            .header("User-Agent", "Lumera/${BuildConfig.VERSION_NAME}")

        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        return builder.build()
    }
}
