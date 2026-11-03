package com.ibramusic.mobile

import com.ibramusic.mobile.core.network.NewPipeDownloader
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class YouTubeResolverTest {

    @Test
    fun testStudioAudioVideoIds() {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        NewPipe.init(NewPipeDownloader(client))

        val queries = listOf("Trueno FEEL ME??", "Nemzzz 4AM")
        val pattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")

        for (q in queries) {
            val cleanTitle = q.replace("??", "").trim()
            val jsonBody = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20261020.01.00",
                            "hl": "en",
                            "gl": "US"
                        }
                    },
                    "query": "$cleanTitle",
                    "params": "EgWKAQIIAQ%3D%3D"
                }
            """.trimIndent()

            val req = okhttp3.Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://music.youtube.com/")
                .build()

            val resp = client.newCall(req).execute()
            println("HTTP Status for '$q': ${resp.code}")
            val body = resp.body?.string() ?: ""
            println("Body length: ${body.length}")
            val flexiblePattern = Pattern.compile("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"")
            val matcher = flexiblePattern.matcher(body)
            val found = matcher.find()
            if (!found) {
                println("No videoId found in body: ${body.take(300)}")
            }
            assert(found)
            val videoId = matcher.group(1)
            println("Query '$q' -> Resolved Studio VideoId: $videoId")

            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
            val audioStreams = streamInfo.audioStreams
            assert(audioStreams.isNotEmpty())
            val bestAudio = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
            println("  Stream Title: '${streamInfo.name}', Uploader: '${streamInfo.uploaderName}', Mime: ${bestAudio.format?.mimeType}, Bitrate: ${bestAudio.averageBitrate}")
        }
    }
}
