package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    @Json(name = "items") val items: List<YouTubeSearchResultItem>?
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResultItem(
    @Json(name = "id") val id: YouTubeResourceId?,
    @Json(name = "snippet") val snippet: YouTubeSnippet?
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    @Json(name = "videoId") val videoId: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    @Json(name = "title") val title: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "channelTitle") val channelTitle: String?,
    @Json(name = "channelId") val channelId: String?,
    @Json(name = "publishedAt") val publishedAt: String?,
    @Json(name = "thumbnails") val thumbnails: YouTubeThumbnails?
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    @Json(name = "medium") val medium: YouTubeThumbnailItem?,
    @Json(name = "high") val high: YouTubeThumbnailItem?
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnailItem(
    @Json(name = "url") val url: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveBroadcastListResponse(
    @Json(name = "items") val items: List<YouTubeLiveBroadcastItem>?
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveBroadcastItem(
    @Json(name = "id") val id: String?,
    @Json(name = "snippet") val snippet: YouTubeSnippet?,
    @Json(name = "contentDetails") val contentDetails: YouTubeLiveBroadcastContentDetails?,
    @Json(name = "status") val status: YouTubeLiveBroadcastStatus?
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveBroadcastContentDetails(
    @Json(name = "boundStreamId") val boundStreamId: String?,
    @Json(name = "activeLiveChatId") val activeLiveChatId: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveBroadcastStatus(
    @Json(name = "lifeCycleStatus") val lifeCycleStatus: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoListResponse(
    @Json(name = "items") val items: List<YouTubeVideoItem>?
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
    @Json(name = "id") val id: String?,
    @Json(name = "snippet") val snippet: YouTubeSnippet?,
    @Json(name = "liveStreamingDetails") val liveStreamingDetails: YouTubeLiveStreamingDetails?,
    @Json(name = "statistics") val statistics: YouTubeVideoStatistics?
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveStreamingDetails(
    @Json(name = "actualStartTime") val actualStartTime: String?,
    @Json(name = "concurrentViewers") val concurrentViewers: String?,
    @Json(name = "activeLiveChatId") val activeLiveChatId: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoStatistics(
    @Json(name = "viewCount") val viewCount: String?,
    @Json(name = "likeCount") val likeCount: String?,
    @Json(name = "commentCount") val commentCount: String?
)

@JsonClass(generateAdapter = true)
data class YouTubePollDetails(
    @Json(name = "metadata") val metadata: YouTubePollMetadata?
)

@JsonClass(generateAdapter = true)
data class YouTubePollMetadata(
    @Json(name = "questionText") val questionText: String?,
    @Json(name = "options") val options: List<YouTubePollOption>?,
    @Json(name = "status") val status: String?
)

@JsonClass(generateAdapter = true)
data class YouTubePollOption(
    @Json(name = "optionText") val optionText: String?,
    @Json(name = "tally") val tally: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveChatMessagesResponse(
    @Json(name = "items") val items: List<YouTubeLiveChatMessageItem>?,
    @Json(name = "nextPageToken") val nextPageToken: String?,
    @Json(name = "pollingIntervalMillis") val pollingIntervalMillis: Long?,
    @Json(name = "activePollItem") val activePollItem: YouTubeLiveChatMessageItem?
)

@JsonClass(generateAdapter = true)
data class YouTubeLiveChatMessageItem(
    @Json(name = "id") val id: String?,
    @Json(name = "snippet") val snippet: LiveChatMessageSnippet?,
    @Json(name = "authorDetails") val authorDetails: LiveChatAuthorDetails?
)

@JsonClass(generateAdapter = true)
data class LiveChatMessageSnippet(
    @Json(name = "type") val type: String?,
    @Json(name = "publishedAt") val publishedAt: String?,
    @Json(name = "displayMessage") val displayMessage: String?,
    @Json(name = "textMessageDetails") val textMessageDetails: TextMessageDetails?,
    @Json(name = "superChatDetails") val superChatDetails: SuperChatDetails?,
    @Json(name = "pollDetails") val pollDetails: YouTubePollDetails?
)

@JsonClass(generateAdapter = true)
data class TextMessageDetails(
    @Json(name = "messageText") val messageText: String?
)

@JsonClass(generateAdapter = true)
data class SuperChatDetails(
    @Json(name = "amountDisplayString") val amountDisplayString: String?,
    @Json(name = "userComment") val userComment: String?
)

@JsonClass(generateAdapter = true)
data class LiveChatAuthorDetails(
    @Json(name = "channelId") val channelId: String?,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "profileImageUrl") val profileImageUrl: String?,
    @Json(name = "isChatOwner") val isChatOwner: Boolean?,
    @Json(name = "isChatSponsor") val isChatSponsor: Boolean?,
    @Json(name = "isChatModerator") val isChatModerator: Boolean?,
    @Json(name = "isVerified") val isVerified: Boolean?
)

@JsonClass(generateAdapter = true)
data class CreateLivePollRequest(
    @Json(name = "snippet") val snippet: CreateLivePollSnippet
)

@JsonClass(generateAdapter = true)
data class CreateLivePollSnippet(
    @Json(name = "liveChatId") val liveChatId: String,
    @Json(name = "type") val type: String = "pollEvent",
    @Json(name = "pollDetails") val pollDetails: PollDetails
)

@JsonClass(generateAdapter = true)
data class PollDetails(
    @Json(name = "metadata") val metadata: PollMetadata
)

@JsonClass(generateAdapter = true)
data class PollMetadata(
    @Json(name = "questionText") val questionText: String,
    @Json(name = "options") val options: List<PollOptionRequest>
)

@JsonClass(generateAdapter = true)
data class PollOptionRequest(
    @Json(name = "optionText") val optionText: String
)

@JsonClass(generateAdapter = true)
data class SendChatMessageRequest(
    @Json(name = "snippet") val snippet: SendChatMessageSnippet
)

@JsonClass(generateAdapter = true)
data class SendChatMessageSnippet(
    @Json(name = "liveChatId") val liveChatId: String,
    @Json(name = "type") val type: String = "textMessageEvent",
    @Json(name = "textMessageDetails") val textMessageDetails: TextMessageDetails
)
