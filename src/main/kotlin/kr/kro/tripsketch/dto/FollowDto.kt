package kr.kro.tripsketch.dto

import jakarta.validation.constraints.NotBlank

data class FollowDto(
    @field:NotBlank(message = "닉네임을 입력해주세요")
    val nickname: String
)
