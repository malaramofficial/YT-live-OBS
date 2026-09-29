package com.example.data.model

data class PollOption(
    val id: Int,
    val text: String,
    val votes: Int = 0
)

data class LivePoll(
    val id: String = "poll_current",
    val question: String,
    val options: List<PollOption>,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val showOnOverlay: Boolean = true
) {
    val totalVotes: Int
        get() = options.sumOf { it.votes }

    fun percentageFor(optionId: Int): Int {
        val total = totalVotes
        if (total == 0) return 0
        val option = options.find { it.id == optionId } ?: return 0
        return ((option.votes.toDouble() / total) * 100).toInt()
    }
}
