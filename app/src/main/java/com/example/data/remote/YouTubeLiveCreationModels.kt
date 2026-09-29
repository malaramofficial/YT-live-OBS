package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateLiveBroadcastRequest(
    @Json(name = "snippet") val snippet: CreateBroadcastSnippet,
    @Json(name = "status") val status: CreateBroadcastStatus,
    @Json(name = "contentDetails") val contentDetails: CreateBroadcastContentDetails
)

@JsonClass(generateAdapter = true)
data class CreateBroadcastSnippet(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "scheduledStartTime") val scheduledStartTime: String
)

@JsonClass(generateAdapter = true)
data class CreateBroadcastStatus(
    @Json(name = "privacyStatus") val privacyStatus: String
)

@JsonClass(generateAdapter = true)
data class CreateBroadcastContentDetails(
    @Json(name = "enableAutoStart") val enableAutoStart: Boolean = true,
    @Json(name = "enableAutoStop") val enableAutoStop: Boolean = true,
    @Json(name = "recordFromStart") val recordFromStart: Boolean = true,
    @Json(name = "enableDvr") val enableDvr: Boolean = true,
    @Json(name = "enableEmbed") val enableEmbed: Boolean = true,
    @Json(name = "monitorStream") val monitorStream: MonitorStreamSettings = MonitorStreamSettings()
)

@JsonClass(generateAdapter = true)
data class MonitorStreamSettings(
    @Json(name = "enableMonitorStream") val enableMonitorStream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateLiveStreamRequest(
    @Json(name = "snippet") val snippet: CreateStreamSnippet,
    @Json(name = "cdn") val cdn: CreateStreamCdn,
    @Json(name = "contentDetails") val contentDetails: CreateStreamContentDetails = CreateStreamContentDetails()
)

@JsonClass(generateAdapter = true)
data class CreateStreamSnippet(
    @Json(name = "title") val title: String
)

@JsonClass(generateAdapter = true)
data class CreateStreamCdn(
    @Json(name = "frameRate") val frameRate: String,
    @Json(name = "ingestionType") val ingestionType: String,
    @Json(name = "resolution") val resolution: String
)

@JsonClass(generateAdapter = true)
data class CreateStreamContentDetails(
    @Json(name = "isReusable") val isReusable: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateLiveBroadcastResponse(
    @Json(name = "id") val id: String?
)

@JsonClass(generateAdapter = true)
data class CreateLiveStreamResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "cdn") val cdn: LiveStreamCdn?
)

@JsonClass(generateAdapter = true)
data class LiveStreamCdn(
    @Json(name = "ingestionInfo") val ingestionInfo: LiveIngestionInfo?,
    @Json(name = "rtmpsIngestionAddress") val rtmpsIngestionAddress: String?
)

@JsonClass(generateAdapter = true)
data class LiveIngestionInfo(
    @Json(name = "streamName") val streamName: String?,
    @Json(name = "ingestionAddress") val ingestionAddress: String?,
    @Json(name = "backupIngestionAddress") val backupIngestionAddress: String?,
    @Json(name = "rtmpsIngestionAddress") val rtmpsIngestionAddress: String?,
    @Json(name = "rtmpsBackupIngestionAddress") val rtmpsBackupIngestionAddress: String?
)
