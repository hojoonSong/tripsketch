package kr.kro.tripsketch.controllers

import kr.kro.tripsketch.dto.AdditionalUserInfo
import kr.kro.tripsketch.dto.UserLoginDto
import kr.kro.tripsketch.dto.UserRegistrationDto
import kr.kro.tripsketch.services.KakaoOAuthService
import kr.kro.tripsketch.services.UserService
import kr.kro.tripsketch.services.JwtService
import kr.kro.tripsketch.services.NickNameService

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/oauth")
class UserController(
    private val kakaoOAuthService: KakaoOAuthService,
    private val userService: UserService,
    private val jwtService: JwtService,
    private val nicknameService: NickNameService,
) {

    @GetMapping("/kakao/callback")
    fun kakaoCallback(@RequestParam code: String): ResponseEntity<Any> {
        val accessToken = kakaoOAuthService.getKakaoAccessToken(code)
        val userInfo = kakaoOAuthService.getUserInfo(accessToken)

        if (userInfo == null) {
            return ResponseEntity.status(400).body("유효하지 않은 요청입니다.")
        }

        val kakaoAccountInfo = userInfo["kakao_account"] as? Map<*, *>
        val email = kakaoAccountInfo?.get("email")?.toString()
        if (email == null) {
            return ResponseEntity.status(400).body("이메일 정보가 없습니다.")
        }

        var user = userService.findUserByEmail(email)
        if (user == null) {
            // 회원 가입을 위한 추가 정보가 없는 경우, 임의의 값을 설정합니다.
            val additionalUserInfo = AdditionalUserInfo(
                nickname = nicknameService.generateRandomNickname(), // 임의의 닉네임
                profileImageUrl = "https://example.com/default-profile-image.png", // 기본 프로필 이미지 URL
                introduction = "Nice to meet you!" // 기본 소개 문구
            )
            val userRegistrationDto = UserRegistrationDto(
                email,
                additionalUserInfo.nickname,
                additionalUserInfo.profileImageUrl,
                additionalUserInfo.introduction
            )
            user = userService.registerUser(userRegistrationDto)
        }

        val jwt = jwtService.createToken(user)
        val response = mapOf("token" to jwt)
        return ResponseEntity.ok(response)
    }
}