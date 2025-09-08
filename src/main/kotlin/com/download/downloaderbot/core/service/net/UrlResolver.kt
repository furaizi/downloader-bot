package com.download.downloaderbot.core.service.net

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component

@Component
class UrlResolver(private val client: OkHttpClient) {
    fun finalUrl(url: String): String {
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