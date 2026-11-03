package com.ibramusic.mobile.core.network

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val reqBuilder = okhttp3.Request.Builder()
            .url(url)

        headers.forEach { (key, values) ->
            values.forEach { value ->
                reqBuilder.addHeader(key, value)
            }
        }

        // Set realistic browser User-Agent if none specified to prevent 403 Forbidden on Android
        if (headers.none { it.key.equals("User-Agent", ignoreCase = true) }) {
            reqBuilder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )
        }

        if (headers.none { it.key.equals("Accept-Language", ignoreCase = true) }) {
            reqBuilder.header("Accept-Language", "en-US,en;q=0.9,es;q=0.8")
        }

        // YouTube cookie to bypass consent redirect banners
        if (headers.none { it.key.equals("Cookie", ignoreCase = true) }) {
            reqBuilder.header("Cookie", "SOCS=CAESEwgDEgk2MTQ1NzU1MDQaAmVuIAEaBgiA_LyaBg; CONSENT=PENDING+999; PREF=f4=4000000")
        }

        val requestBody = if (dataToSend != null) {
            dataToSend.toRequestBody(null)
        } else if (httpMethod.equals("POST", ignoreCase = true)) {
            ByteArray(0).toRequestBody(null)
        } else {
            null
        }

        reqBuilder.method(httpMethod, requestBody)

        val okCall = client.newCall(reqBuilder.build())
        val okResponse = okCall.execute()

        val bodyString = okResponse.body?.string() ?: ""
        val responseHeaders = mutableMapOf<String, List<String>>()
        okResponse.headers.toMultimap().forEach { (k, v) ->
            responseHeaders[k] = v
        }

        return Response(
            okResponse.code,
            okResponse.message,
            responseHeaders,
            bodyString,
            okResponse.request.url.toString()
        )
    }
}

