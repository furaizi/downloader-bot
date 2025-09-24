package com.download.downloaderbot.infra.network

import com.download.downloaderbot.core.net.FinalUrlResolver
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component

@Component
class OkHttpFinalUrlResolver(private val client: OkHttpClient) : FinalUrlResolver {
    override suspend fun resolve(url: String): String {
        val req = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Mozilla/5.0")
            .build()
        client.newCall(req)
            .execute()
            .use { resp ->
            return resp.request.url.toString()
        }
    }
}
