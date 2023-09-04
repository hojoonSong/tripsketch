package kr.kro.tripsketch.services

import kr.kro.tripsketch.domain.User
import kr.kro.tripsketch.dto.ProfileDto
import kr.kro.tripsketch.dto.UserDto
import kr.kro.tripsketch.dto.UserUpdateDto
import kr.kro.tripsketch.exceptions.BadRequestException
import kr.kro.tripsketch.repositories.FollowRepository
import kr.kro.tripsketch.repositories.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val nicknameService: NickNameService,
    private val imageService: ImageService,
) {

    fun registerOrUpdateUser(email: String): User {
        var user = userRepository.findByEmail(email)
        if (user == null) {
            var nickname: String
            do {
                nickname = nicknameService.generateRandomNickname()
            } while (isNicknameExist(nickname))

            user = User(
                email = email,
                nickname = nickname,
                profileImageUrl = "https://objectstorage.ap-osaka-1.oraclecloud.com/p/_EncCFAsYOUIwlJqRN7blRAETL9_l-fpCH-D07N4qig261ob7VHU8VIgtZaP-Thz/n/ax6izwmsuv9c/b/image-tripsketch/o/default-02.png",
                introduction = "안녕하세요! 만나서 반갑습니다!"
            )
            user = userRepository.save(user)
        }
        return user
    }


    fun findUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun findUserByNickname(nickname: String): User? {
        return userRepository.findByNickname(nickname)
    }

    fun getAllUsers(pageable: Pageable): Page<User> {
        return userRepository.findAll(pageable)
    }


    fun updateUser(email: String, userUpdateDto: UserUpdateDto): User {
        val user = userRepository.findByEmail(email) ?: throw BadRequestException("해당 이메일을 가진 사용자가 존재하지 않습니다.")

        userUpdateDto.nickname?.let {
            if (it != user.nickname && isNicknameExist(it)) {
                throw BadRequestException("이미 사용중인 닉네임입니다.")
            }
            user.nickname = it
        }

        userUpdateDto.profileImageUrl?.let { newImageFile ->
            val defaultImageUrl = "https://objectstorage.ap-osaka-1.oraclecloud.com/p/_EncCFAsYOUIwlJqRN7blRAETL9_l-fpCH-D07N4qig261ob7VHU8VIgtZaP-Thz/n/ax6izwmsuv9c/b/image-tripsketch/o/default-02.png"

            if (user.profileImageUrl != defaultImageUrl) {
                user.profileImageUrl?.let { oldImageUrl ->
                    try {
                        imageService.deleteImage(oldImageUrl)  // `ImageService`의 `deleteImage` 함수를 사용하여 URL을 삭제합니다.
                    } catch (e: Exception) {
                        // 오류 로깅
                        println("이미지 삭제에 실패했습니다. URL: $oldImageUrl, 오류: ${e.message}")
                    }
                }
            }

            val newImageUrl = imageService.uploadImage("tripsketch/trip-user", newImageFile)
            user.profileImageUrl = newImageUrl
        }

        userUpdateDto.introduction?.let {
            user.introduction = it
        }

        return userRepository.save(user)
    }



    fun isNicknameExist(nickname: String): Boolean {
        return userRepository.existsByNickname(nickname)
    }

    fun updateUserRefreshToken(email: String, ourRefreshToken: String): User {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("해당 이메일을 가진 사용자가 존재하지 않습니다.")
        user.ourRefreshToken = ourRefreshToken
        return userRepository.save(user)
    }

    fun findByOurRefreshToken(ourRefreshToken: String): User? {
        return userRepository.findByOurRefreshToken(ourRefreshToken)
    }

    fun updateKakaoRefreshToken(email: String, kakaoRefreshToken: String): User {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("해당 이메일을 가진 사용자가 존재하지 않습니다.")
        user.kakaoRefreshToken = kakaoRefreshToken
        return userRepository.save(user)
    }

    fun storeUserPushToken(email: String, pushToken: String) {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found with email $email")
        user.expoPushToken = pushToken
        userRepository.save(user)
    }

    fun getUserFollowInfo(email: String): Pair<Long, Long> {
        val followingCount = followRepository.countByFollower(email)
        val followersCount = followRepository.countByFollowing(email)

        return Pair(followersCount, followingCount)
    }

    fun toDto(user: User, includeEmail: Boolean = true): UserDto {
        val (followersCount, followingCount) = getUserFollowInfo(user.email)

        return if (includeEmail) {
            UserDto(
                email = user.email,
                nickname = user.nickname,
                introduction = user.introduction,
                profileImageUrl = user.profileImageUrl,
                followersCount = followersCount,
                followingCount = followingCount
            )
        } else {
            UserDto(
                email = null,
                nickname = user.nickname,
                introduction = user.introduction,
                profileImageUrl = user.profileImageUrl,
                followersCount = followersCount,
                followingCount = followingCount
            )
        }
    }
}
