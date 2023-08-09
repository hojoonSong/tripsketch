package kr.kro.tripsketch.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "users")
data class User(
    @Id val id: String? = null,

    @Indexed(unique = true)
    val email: String,

    @Indexed(unique = true)
    var nickname: String,

    var introduction: String?,
    var profileImageUrl: String?,

    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    var refreshToken: String? = null
)