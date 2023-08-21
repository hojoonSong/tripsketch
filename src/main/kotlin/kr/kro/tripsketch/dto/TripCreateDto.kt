package kr.kro.tripsketch.dto

import java.time.LocalDateTime

data class TripCreateDto(
    var id: String? = null,
//    var userId: String,
    var userEmail: String,
    var nickname: String?,
//    var scheduleId: String,
    var title: String,
    var content: String,
//    var likes: Int? = 0,
//    var views: Int? = 0,
    var location: String? = null,
    var startedAt: LocalDateTime = LocalDateTime.now(),
    var endAt: LocalDateTime = LocalDateTime.now(),
    var hashtag: String? = null,
//    var hidden: Boolean = false,
//    val createdAt: LocalDateTime = LocalDateTime.now(),
//    var updatedAt: LocalDateTime? = null,
//    var deletedAt: LocalDateTime? = null,
//    var likeFlag: Int = 0,
//    var tripViews: Set<String> = setOf()
)
